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
import com.amazonaws.appsync.ConnectionState
import com.amazonaws.appsync.SubscriptionEvent
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.GsonGraphQLResponseFactory
import com.amplifyframework.api.aws.LazyQueryExecutor
import com.amplifyframework.api.aws.MultiAuthSubscriptionOperation
import com.amplifyframework.api.aws.SubscriptionEndpoint
import com.amplifyframework.api.aws.auth.AuthRuleRequestDecorator
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Internal WebSocket client for GraphQL subscriptions. Delegates to the battle-tested
 * [MultiAuthSubscriptionOperation] from `:aws-api` which handles the full multi-auth
 * retry loop for subscription establishment, and [SubscriptionEndpoint] for the actual
 * WebSocket lifecycle, protocol handling, keep-alive, and message dispatching.
 */
internal class GraphQLWebSocketClient(
    private val configuration: AmplifyAppSyncClient.Configuration,
    private val lazyQueryExecutor: LazyQueryExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closed = AtomicBoolean(false)
    private val executorService = Executors.newCachedThreadPool()

    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    private val authProviders by lazy {
        AuthProviderBridge.buildApiAuthProviders(configuration.authorization)
    }

    private val subscriptionEndpoint: SubscriptionEndpoint by lazy {
        val authMode = configuration.authorization.resolveDefaultAuthMode()
        SubscriptionEndpoint(
            configuration.endpoint,
            configuration.region,
            AuthModeBridge.mapAuthMode(authMode),
            null,
            configuration.webSocketClientConfigurator?.let { configurator ->
                com.amplifyframework.api.aws.OkHttpConfigurator { builder ->
                    configurator(builder)
                }
            },
            GsonGraphQLResponseFactory(),
            authProviders,
            lazyQueryExecutor
        )
    }

    private val requestDecorator: AuthRuleRequestDecorator by lazy {
        AuthRuleRequestDecorator(authProviders)
    }

    init {
        _connectionState.tryEmit(ConnectionState.Disconnected())
    }

    /**
     * Subscribe to a GraphQL subscription with multi-auth retry.
     * For [AppSyncGraphQLRequest] (model-based), uses [MultiAuthSubscriptionOperation]
     * which tries each auth mode from `@auth` rules in priority order.
     * For raw requests, uses [SubscriptionEndpoint] directly with the default auth mode.
     */
    fun <T> subscribe(request: GraphQLRequest<T>): Flow<SubscriptionEvent<T>> = callbackFlow {
        if (closed.get()) {
            throw ApiException(
                "Client has been closed.",
                "Create a new AmplifyAppSyncClient instance."
            )
        }

        trySend(SubscriptionEvent.Connection.Connecting)
        scope.launch { _connectionState.emit(ConnectionState.Connecting) }

        val appSyncRequest = request as? AppSyncGraphQLRequest<T>
        val useMultiAuth = appSyncRequest != null &&
            appSyncRequest.authorizationType == null &&
            appSyncRequest.authModeStrategyType != null &&
            appSyncRequest.modelSchema.hasModelLevelRules()

        if (useMultiAuth) {
            val operation = MultiAuthSubscriptionOperation.builder<T>()
                .subscriptionEndpoint(subscriptionEndpoint)
                .graphQlRequest(appSyncRequest!!)
                .responseFactory(GsonGraphQLResponseFactory())
                .executorService(executorService)
                .requestDecorator(requestDecorator)
                .onSubscriptionStart { _ ->
                    scope.launch { _connectionState.emit(ConnectionState.Connected) }
                    trySend(SubscriptionEvent.Connection.Connected)
                }
                .onNextItem { response: GraphQLResponse<T> ->
                    trySend(SubscriptionEvent.Data(response))
                }
                .onSubscriptionError { error: ApiException ->
                    close(error)
                }
                .onSubscriptionComplete {
                    channel.close()
                }
                .build()

            operation.start()
            awaitClose { operation.cancel() }
        } else {
            val authMode = configuration.authorization.resolveDefaultAuthMode()
            val authType = AuthModeBridge.mapAuthMode(authMode)
            var subscriptionId: String? = null

            subscriptionEndpoint.requestSubscription(
                request,
                authType,
                { id ->
                    subscriptionId = id
                    scope.launch { _connectionState.emit(ConnectionState.Connected) }
                    trySend(SubscriptionEvent.Connection.Connected)
                },
                { response: GraphQLResponse<T> ->
                    trySend(SubscriptionEvent.Data(response))
                },
                { error: ApiException ->
                    close(error)
                },
                { channel.close() }
            )

            awaitClose {
                subscriptionId?.let { id ->
                    try { subscriptionEndpoint.releaseSubscription(id) } catch (_: Exception) {}
                }
            }
        }
    }

    fun close() {
        if (closed.getAndSet(true)) return
        executorService.shutdown()
        scope.cancel()
        _connectionState.tryEmit(ConnectionState.Disconnected())
    }
}
