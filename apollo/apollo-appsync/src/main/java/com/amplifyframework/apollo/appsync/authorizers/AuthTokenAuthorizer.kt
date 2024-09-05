/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync.authorizers

import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import com.amplifyframework.apollo.appsync.util.HeaderKeys.Http
import com.amplifyframework.apollo.appsync.util.HeaderKeys.WebSocket
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.HttpRequest
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * [AppSyncAuthorizer] implementation for using auth tokens for authorization with AppSync. You can use this class to
 * authorize requests with Cognito User Pools, OIDC, or Lambda-based custom authorization.
 * @param fetchLatestAuthToken Delegate to return a valid auth token. This delegate will be invoked on every request, so
 * it should implement a reasonable caching mechanism if necessary.
 */
class AuthTokenAuthorizer(private val fetchLatestAuthToken: suspend () -> String) : AppSyncAuthorizer {

    constructor(authTokenProvider: AuthTokenProvider) : this({
        suspendCoroutine { continuation ->
            authTokenProvider.fetchLatestAuthToken({
                continuation.resume(it)
            }, { continuation.resumeWithException(it) })
        }
    })

    // See TokenRequestDecorator
    override suspend fun getHttpAuthorizationHeaders(request: HttpRequest) =
        mapOf(Http.AUTHORIZATION to fetchLatestAuthToken())

    // See SubscriptionAuthorizer.forCognitoUserPools
    override suspend fun getWebsocketConnectionHeaders(endpoint: AppSyncEndpoint) =
        mapOf(WebSocket.AUTHORIZATION to fetchLatestAuthToken())

    // See SubscriptionAuthorizer.forCognitoUserPools
    override suspend fun getWebSocketSubscriptionPayload(endpoint: AppSyncEndpoint, request: ApolloRequest<*>) =
        mapOf(WebSocket.AUTHORIZATION to fetchLatestAuthToken())

    /**
     * Provide an Auth Token using a callback-based API. This is primarily intended for Java consumption.
     */
    fun interface AuthTokenProvider {
        fun fetchLatestAuthToken(onSuccess: Consumer<String>, onError: Consumer<Throwable>)
    }
}
