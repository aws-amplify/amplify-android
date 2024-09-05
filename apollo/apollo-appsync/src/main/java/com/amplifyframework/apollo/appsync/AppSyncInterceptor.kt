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

package com.amplifyframework.apollo.appsync

import com.amplifyframework.apollo.appsync.util.UserAgentHeader
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain

/**
 * Implementation of Apollo's [HttpInterceptor] interface that uses an [AppSyncAuthorizer] to append authorization
 * headers to the outgoing request.
 *
 * @param authorizer The [AppSyncAuthorizer] that provides the authorization headers
 */
class AppSyncInterceptor(private val authorizer: AppSyncAuthorizer) : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        val headers = authorizer.getHttpAuthorizationHeaders(request).map { (key, value) -> HttpHeader(key, value) }
        return chain.proceed(
            request
                .newBuilder()
                .addHeaders(headers)
                .addHeader(UserAgentHeader.NAME, UserAgentHeader.value)
                .build()
        )
    }
}
