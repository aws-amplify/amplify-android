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

package com.amplifyframework.apollo.appsync.util

import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Intercepts the WebSocket connection request to append the authorization headers
 */
internal class WebSocketConnectionInterceptor(
    private val endpoint: AppSyncEndpoint,
    private val authorizer: AppSyncAuthorizer
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // runBlocking is okay because we are on an IO thread when the interceptor is called
        val headers = runBlocking { authorizer.getWebsocketConnectionHeaders(endpoint) }
        val builder = chain.request().newBuilder()
        headers.forEach { header -> builder.header(header.key, header.value) }
        builder.header("host", endpoint.serverUrl.host)
        return chain.proceed(builder.build())
    }
}
