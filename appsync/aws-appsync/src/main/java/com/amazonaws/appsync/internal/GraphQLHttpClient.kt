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
package com.amazonaws.appsync.internal

import com.amazonaws.appsync.AmplifyAppSyncClient
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.aws.ApiAuthProviders
import com.amplifyframework.api.aws.AppSyncGraphQLOperation
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.EndpointType
import com.amplifyframework.api.aws.GsonGraphQLResponseFactory
import com.amplifyframework.api.aws.MultiAuthAppSyncGraphQLOperation
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Internal HTTP transport for GraphQL queries and mutations. Uses the battle-tested
 * [MultiAuthAppSyncGraphQLOperation] from `:aws-api` which handles the full multi-auth
 * retry loop — trying each auth mode from the model's `@auth` rules in priority order
 * until one succeeds or all fail.
 */
internal class GraphQLHttpClient(
    private val configuration: AmplifyAppSyncClient.Configuration
) {
    private val closed = AtomicBoolean(false)
    private val executorService = Executors.newCachedThreadPool()
    private val responseFactory = GsonGraphQLResponseFactory()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        configuration.httpClientConfigurator?.invoke(this)
    }.build()

    private val authProviders: ApiAuthProviders by lazy {
        AuthProviderBridge.buildApiAuthProviders(configuration.authorization)
    }

    private val apiRequestDecoratorFactory: ApiRequestDecoratorFactory by lazy {
        val authMode = configuration.authorization.resolveDefaultAuthMode()
        val authType = AuthModeBridge.mapAuthMode(authMode)
        ApiRequestDecoratorFactory(
            authProviders,
            authType,
            configuration.region,
            EndpointType.GRAPHQL,
            null // apiKey resolved by authProviders
        )
    }

    /**
     * Execute a GraphQL request (query or mutation).
     *
     * Mirrors the V2 plugin's auth strategy selection: uses [MultiAuthAppSyncGraphQLOperation]
     * only when the model has `@auth` rules; otherwise falls back to [AppSyncGraphQLOperation]
     * which uses the client's default auth mode.
     */
    suspend fun <T> execute(request: GraphQLRequest<T>): GraphQLResponse<T> =
        suspendCancellableCoroutine { continuation ->
            val useMultiAuth = (request as? AppSyncGraphQLRequest<*>)?.let {
                it.authorizationType == null &&
                    it.authModeStrategyType != null &&
                    it.modelSchema.hasModelLevelRules()
            } ?: false

            val operation = if (useMultiAuth) {
                MultiAuthAppSyncGraphQLOperation.builder<T>()
                    .endpoint(configuration.endpoint)
                    .client(okHttpClient)
                    .request(request)
                    .responseFactory(responseFactory)
                    .apiRequestDecoratorFactory(apiRequestDecoratorFactory)
                    .executorService(executorService)
                    .onResponse { response -> continuation.resume(response) }
                    .onFailure { error -> continuation.resumeWithException(error) }
                    .build()
            } else {
                AppSyncGraphQLOperation.builder<T>()
                    .endpoint(configuration.endpoint)
                    .client(okHttpClient)
                    .request(request)
                    .responseFactory(responseFactory)
                    .apiRequestDecoratorFactory(apiRequestDecoratorFactory)
                    .executorService(executorService)
                    .onResponse { response -> continuation.resume(response) }
                    .onFailure { error -> continuation.resumeWithException(error) }
                    .build()
            }

            continuation.invokeOnCancellation { operation.cancel() }
            operation.start()
        }

    fun close() {
        if (closed.getAndSet(true)) return
        executorService.shutdown()
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}
