/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync.authorizers

import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import com.amplifyframework.apollo.appsync.util.HeaderKeys.Http
import com.amplifyframework.apollo.appsync.util.HeaderKeys.WebSocket
import com.amplifyframework.apollo.appsync.util.Iso8601Timestamp
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.HttpRequest
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * [AppSyncAuthorizer] implementation that authorizes requests via API Key.
 * @param fetchApiKey Delegate that provides the API Key to use. This provider will be invoked on every request, so
 * it should implement a reasonable caching mechanism if necessary.
 */
class ApiKeyAuthorizer(private val fetchApiKey: suspend () -> String) : AppSyncAuthorizer {
    /**
     * Provide a static API Key
     * @param apiKey The API Key
     */
    constructor(apiKey: String) : this({ apiKey })

    constructor(apiKeyProvider: ApiKeyProvider) : this({
        suspendCoroutine { continuation ->
            apiKeyProvider.fetchApiKey({ continuation.resume(it) }, { continuation.resumeWithException(it) })
        }
    })

    // See ApiKeyRequestDecorator
    override suspend fun getHttpAuthorizationHeaders(request: HttpRequest) = mapOf(Http.API_KEY to fetchApiKey())

    // See SubscriptionAuthorizer.forApiKey
    override suspend fun getWebsocketConnectionHeaders(endpoint: AppSyncEndpoint) = mapOf(
        WebSocket.AMAZON_DATE to Iso8601Timestamp.now(),
        WebSocket.API_KEY to fetchApiKey()
    )

    // See SubscriptionAuthorizer.forApiKey
    override suspend fun getWebSocketSubscriptionPayload(endpoint: AppSyncEndpoint, request: ApolloRequest<*>) = mapOf(
        WebSocket.AMAZON_DATE to Iso8601Timestamp.now(),
        WebSocket.API_KEY to fetchApiKey()
    )

    /**
     * Provide an API Key using a callback-based API. This is primarily intended for Java consumption.
     */
    fun interface ApiKeyProvider {
        fun fetchApiKey(onSuccess: Consumer<String>, onError: Consumer<Throwable>)
    }
}
