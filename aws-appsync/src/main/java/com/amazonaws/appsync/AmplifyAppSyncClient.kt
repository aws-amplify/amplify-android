/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.appsync

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Standalone, instantiable AppSync GraphQL client. Supports queries, mutations, and
 * subscriptions with typed auth and per-client connection state.
 *
 * Not a singleton — create multiple instances for multi-tenant / multi-API scenarios.
 *
 * ```kotlin
 * val client = AmplifyAppSyncClient(
 *     AmplifyAppSyncClient.Configuration {
 *         endpoint = "https://xxx.appsync-api.us-east-1.amazonaws.com/graphql"
 *         authorization = AppSyncAuthorization.Single(
 *             AppSyncClientAuthorizer.ApiKey("da2-xxx")
 *         )
 *     }
 * )
 *
 * when (val result = client.query(ModelQuery.get(Todo::class.java, "id-123"))) {
 *     is Result.Success -> use(result.data)
 *     is Result.Failure -> handleError(result.error)
 * }
 * ```
 */
@ExperimentalAmplifyApi
class AmplifyAppSyncClient(val configuration: Configuration) {

    private val closed = AtomicBoolean(false)

    // close() is not suspend but the WebSocket teardown is, so it needs somewhere to run. Deliberately
    // not cancelled: cancelling it would abort the very teardown it was created to perform.
    private val teardownScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val lazyHttpClient = lazy {
        OkHttpClient.Builder()
            .apply { configuration.httpClientConfigurator?.invoke(this) }
            .build()
    }

    private val httpClient: OkHttpClient by lazyHttpClient

    private val transport: AppSyncHttpTransport by lazy {
        AppSyncHttpTransport(
            endpoint = configuration.endpoint,
            client = httpClient,
            authorization = configuration.authorization,
            decorator = AppSyncRequestDecorator(configuration.region)
        )
    }

    private val webSocketClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { configuration.webSocketClientConfigurator?.invoke(this) }
            .build()
    }

    private val subscriber: AppSyncSubscriber by lazy {
        val decorator = AppSyncRequestDecorator(configuration.region)
        val realtimeUrl = AppSyncEndpointParser.realtimeUrl(configuration.endpoint).getOrThrow()

        AppSyncSubscriber(
            provider = AppSyncWebSocketProvider {
                AppSyncWebSocket(
                    realtimeUrl = realtimeUrl,
                    httpEndpoint = configuration.endpoint,
                    authorizer = configuration.authorization.defaultAuthorizer,
                    decorator = decorator,
                    client = webSocketClient
                )
            },
            authorization = configuration.authorization,
            decorator = decorator,
            httpEndpoint = configuration.endpoint
        )
    }

    /**
     * Per-client connection state flow.
     * Emits [ConnectionState] changes for the shared WebSocket connection.
     */
    val events: SharedFlow<ConnectionState>
        get() = subscriber.events

    /**
     * Execute a GraphQL query.
     *
     * @param request The GraphQL request. Use model helpers or construct manually.
     * @return [Result.Success] with the typed GraphQL response, or [Result.Failure] with an [AppSyncException].
     */
    suspend fun <T> query(request: GraphQLRequest<T>): Result<GraphQLResponse<T>, AppSyncException> = send(request)

    /**
     * Execute a GraphQL mutation.
     *
     * @param request The GraphQL request. Use model helpers or construct manually.
     * @return [Result.Success] with the typed GraphQL response, or [Result.Failure] with an [AppSyncException].
     */
    suspend fun <T> mutate(request: GraphQLRequest<T>): Result<GraphQLResponse<T>, AppSyncException> = send(request)

    /**
     * Queries and mutations are the same HTTP exchange — AppSync distinguishes them by the operation
     * in the document, not by transport. They are separate public functions for call-site clarity and
     * for parity with Swift and Flutter.
     *
     * Failures arrive as [Result.Failure] rather than being thrown, so a caller never needs a
     * try/catch. Coroutine cancellation is deliberately not caught: it must propagate for structured
     * concurrency to work.
     */
    private suspend fun <T> send(request: GraphQLRequest<T>): Result<GraphQLResponse<T>, AppSyncException> {
        if (closed.get()) {
            // A lifecycle misuse, not a bad request: AppSyncRequestException would tell the caller to
            // correct a request that was fine.
            return Result.Failure(
                AppSyncInvalidConfigException(
                    message = "This client has been closed and cannot be reused.",
                    recoverySuggestion = "Create a new AmplifyAppSyncClient."
                )
            )
        }

        return try {
            Result.Success(withContext(Dispatchers.IO) { transport.execute(request) })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.Failure(AppSyncException.from(error))
        }
    }

    /**
     * Subscribe to a GraphQL subscription. Returns a [Flow] of [SubscriptionEvent].
     *
     * The WebSocket connection is lazy (established on first subscribe) and shared across
     * all subscriptions on this client. Cancelling the collecting coroutine sends an
     * unsubscribe message and releases the subscription.
     *
     * @param request The GraphQL subscription request. Use model helpers or construct manually.
     * @return A cold [Flow] of [SubscriptionEvent].
     */
    fun <T> subscribe(request: GraphQLRequest<T>): Flow<SubscriptionEvent<T>> = flow {
        // Checked at collection rather than call time, so a closed client fails the collector rather
        // than throwing from a function that only builds a cold flow.
        if (closed.get()) {
            throw AppSyncValidationException(
                message = "This client has been closed and cannot be reused.",
                recoverySuggestion = "Create a new AmplifyAppSyncClient."
            )
        }
        emitAll(subscriber.subscribe(request))
    }

    /**
     * Close the client, cancelling enqueued requests, terminating active subscriptions and releasing
     * pooled connections. The client cannot be reused after closing.
     *
     * Subscription teardown is asynchronous: this returns before the WebSocket has finished closing.
     *
     * A request that has already passed its own closed check can still be dispatched after this
     * returns, because closing does not lock the send path. Such a request completes normally rather
     * than being cancelled.
     */
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        // Nothing to release if no request was ever issued, and touching the client here would build
        // one — running the caller's configurator — purely to cancel an empty dispatcher.
        if (lazyHttpClient.isInitialized()) {
            httpClient.dispatcher.cancelAll()
            httpClient.connectionPool.evictAll()
        }
        // The WebSocket teardown is suspending — it waits for closure to be observed so subscription
        // flows complete normally rather than being cut off. close() is not suspend, so it is launched
        // on a scope that deliberately outlives this call.
        teardownScope.launch { subscriber.close() }
    }

    // ── Configuration ───────────────────────────────────────────────────

    /**
     * Configuration for [AmplifyAppSyncClient].
     *
     * Use the builder DSL:
     * ```kotlin
     * AmplifyAppSyncClient.Configuration {
     *     endpoint = "https://xxx.appsync-api.us-east-1.amazonaws.com/graphql"
     *     authorization = AppSyncAuthorization.Single(
     *         AppSyncClientAuthorizer.ApiKey("da2-xxx")
     *     )
     * }
     * ```
     */
    data class Configuration internal constructor(
        /** The AppSync GraphQL endpoint URL. */
        val endpoint: String,
        /** Auth configuration for the client. */
        val authorization: AppSyncAuthorization,
        /** AWS region. Inferred from the endpoint URL or set explicitly. */
        val region: String,
        /** Optional configurator for the OkHttp client used for HTTP requests. */
        val httpClientConfigurator: ((OkHttpClient.Builder) -> Unit)? = null,
        /** Optional configurator for the OkHttp client used for WebSocket connections. */
        val webSocketClientConfigurator: ((OkHttpClient.Builder) -> Unit)? = null
    ) {
        /**
         * Builder for [Configuration]. Required fields: [endpoint] and [authorization].
         */
        class Builder internal constructor() {
            /** The AppSync GraphQL endpoint URL. Required. */
            lateinit var endpoint: String

            /** Auth configuration. Required. */
            lateinit var authorization: AppSyncAuthorization

            /** AWS region. Defaults to inferred from the endpoint URL. */
            var region: String? = null

            /** Optional configurator for the HTTP OkHttp client. */
            var httpClientConfigurator: ((OkHttpClient.Builder) -> Unit)? = null

            /** Optional configurator for the WebSocket OkHttp client. */
            var webSocketClientConfigurator: ((OkHttpClient.Builder) -> Unit)? = null

            internal fun build(): Configuration {
                require(::endpoint.isInitialized) { "endpoint is required" }
                require(::authorization.isInitialized) { "authorization is required" }
                val resolvedRegion = region ?: inferRegion(endpoint)
                return Configuration(
                    endpoint = endpoint,
                    authorization = authorization,
                    region = resolvedRegion,
                    httpClientConfigurator = httpClientConfigurator,
                    webSocketClientConfigurator = webSocketClientConfigurator
                )
            }
        }

        companion object {
            /**
             * Create a [Configuration] using the builder DSL.
             */
            operator fun invoke(block: Builder.() -> Unit): Configuration = Builder().apply(block).build()

            /**
             * Infer the AWS region from an AppSync endpoint URL.
             * Expected format: `https://{id}.appsync-api.{region}.{dnsSuffix}/graphql`, across the
             * commercial, China and GovCloud partitions.
             */
            internal fun inferRegion(endpoint: String): String =
                AppSyncEndpointParser.parse(endpoint).getOrThrow().region
        }
    }
}
