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

package com.amplifyframework.apollo.appsync

import com.amplifyframework.apollo.appsync.util.PackageInfo
import com.amplifyframework.apollo.appsync.util.UserAgentHeader
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.network.http.HttpInterceptorChain
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppSyncInterceptorTest {
    @Test
    fun `appends headers returned by authorizer`() = runTest {
        val authorizer = mockk<AppSyncAuthorizer> {
            coEvery { getHttpAuthorizationHeaders(any()) } returns mapOf("test" to "result", "name" to "value")
        }
        val chain = mockk<HttpInterceptorChain> {
            coEvery { proceed(any()) } returns mockk()
        }
        val request = HttpRequest.Builder(HttpMethod.Post, "url").build()

        val interceptor = AppSyncInterceptor(authorizer)
        interceptor.intercept(request, chain)

        coVerify {
            chain.proceed(
                withArg { updatedRequest ->
                    updatedRequest.headers shouldContainAll listOf(
                        HttpHeader("test", "result"),
                        HttpHeader("name", "value")
                    )
                }
            )
        }
    }

    @Test
    fun `appends user agent header`() = runTest {
        val authorizer = mockk<AppSyncAuthorizer> { coEvery { getHttpAuthorizationHeaders(any()) } returns emptyMap() }
        val chain = mockk<HttpInterceptorChain> { coEvery { proceed(any()) } returns mockk() }
        val request = HttpRequest.Builder(HttpMethod.Post, "url").build()
        val expectedHeader = "UA/2.0 lib/aws-appsync-apollo-extensions-android#${PackageInfo.version}"

        val interceptor = AppSyncInterceptor(authorizer)
        interceptor.intercept(request, chain)

        coVerify {
            chain.proceed(
                withArg { updatedRequest ->
                    updatedRequest.headers shouldContain HttpHeader(UserAgentHeader.NAME, expectedHeader)
                }
            )
        }
    }
}
