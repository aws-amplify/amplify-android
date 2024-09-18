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
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Test

/**
 * Unit tests for the [WebSocketConnectionInterceptor] class
 */
class WebSocketConnectionInterceptorTest {
    private val url = "https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql"

    @Test
    fun `adds expected headers`() {
        val endpoint = AppSyncEndpoint(url)
        val authorizer = mockk<AppSyncAuthorizer> {
            coEvery { getWebsocketConnectionHeaders(endpoint) } returns mapOf("test" to "value")
        }
        val builder = mockk<Request.Builder>(relaxed = true)
        val chain = mockk<Interceptor.Chain> {
            coEvery { request().newBuilder() } returns builder
            coEvery { proceed(any()) } returns mockk()
        }

        val interceptor = WebSocketConnectionInterceptor(endpoint, authorizer)
        interceptor.intercept(chain)

        verify {
            builder.header("test", "value")
            builder.header("host", "example1234567890123456789.appsync-api.us-east-1.amazonaws.com")
        }
    }
}
