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
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.foundation.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
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

    /**
     * Per-client connection state flow.
     * Emits [ConnectionState] changes for the shared WebSocket connection.
     */
    val events: SharedFlow<ConnectionState>
        get() = TODO("Connection state will be implemented with subscriptions")

    /**
     * Execute a GraphQL query.
     *
     * @param request The GraphQL request. Use model helpers or construct manually.
     * @return [Result.Success] with the typed GraphQL response, or [Result.Failure] with an [ApiException].
     */
    suspend fun <T> query(request: GraphQLRequest<T>): Result<GraphQLResponse<T>, ApiException> =
        TODO("Query implementation will be added in a follow-up PR")

    /**
     * Execute a GraphQL mutation.
     *
     * @param request The GraphQL request. Use model helpers or construct manually.
     * @return [Result.Success] with the typed GraphQL response, or [Result.Failure] with an [ApiException].
     */
    suspend fun <T> mutate(request: GraphQLRequest<T>): Result<GraphQLResponse<T>, ApiException> =
        TODO("Mutation implementation will be added in a follow-up PR")

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
    fun <T> subscribe(request: GraphQLRequest<T>): Flow<SubscriptionEvent<T>> =
        TODO("Subscription implementation will be added in a follow-up PR")

    /**
     * Close the client. Terminates all active subscriptions and releases resources.
     * The client cannot be reused after closing.
     */
    fun close() {
        TODO("Close implementation will be added in a follow-up PR")
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
                requireNotNull(resolvedRegion) {
                    "region is required. Either set it explicitly or use a standard AppSync endpoint URL."
                }
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
            operator fun invoke(block: Builder.() -> Unit): Configuration =
                Builder().apply(block).build()

            /**
             * Infer the AWS region from an AppSync endpoint URL.
             * Expected format: `https://{id}.appsync-api.{region}.amazonaws.com/graphql`
             */
            internal fun inferRegion(endpoint: String): String? {
                val regex = Regex("""\.appsync-api\.([a-z0-9-]+)\.amazonaws\.com""")
                return regex.find(endpoint)?.groupValues?.get(1)
            }
        }
    }
}
