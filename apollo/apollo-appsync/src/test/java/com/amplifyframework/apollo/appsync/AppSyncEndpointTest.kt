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

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.ByteString.Companion.decodeBase64
import org.junit.Test

class AppSyncEndpointTest {
    private val standardAppSyncUrl = "https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql"
    private val customAppSyncUrl = "https://api.example.com/graphql"

    @Test
    fun `only https urls are accepted`() {
        shouldThrowWithMessage<IllegalArgumentException>("AppSync URL must be using TLS") {
            AppSyncEndpoint(standardAppSyncUrl.replace("https", "http"))
        }
    }

    @Test
    fun `returns expected serverUrl`() {
        val endpoint = AppSyncEndpoint(standardAppSyncUrl)
        endpoint.serverUrl.toString() shouldBe standardAppSyncUrl
    }

    @Test
    fun `uses expected realtime URL for standard endpoint`() {
        val endpoint = AppSyncEndpoint(standardAppSyncUrl)
        endpoint.websocketConnection.toString() shouldBe
            "https://example1234567890123456789.appsync-realtime-api.us-east-1.amazonaws.com/graphql/connect"
    }

    @Test
    fun `uses expected realtime URL for custom endpoint`() {
        val endpoint = AppSyncEndpoint(customAppSyncUrl)
        endpoint.websocketConnection.toString() shouldBe "https://api.example.com/graphql/realtime/connect"
    }

    @Test
    fun `appends authorizer headers to realtime connection`() = runTest {
        val endpoint = AppSyncEndpoint(standardAppSyncUrl)
        val authorizer = mockk<AppSyncAuthorizer> {
            coEvery { getWebsocketConnectionHeaders(endpoint) } returns mapOf("Authorization" to "test")
        }

        val url = endpoint.createWebsocketServerUrl(authorizer)
        val parsed = url.toHttpUrl()

        val header = parsed.queryParameter("header")?.decodeBase64()?.utf8()
        val payload = parsed.queryParameter("payload")?.decodeBase64()?.utf8()

        header shouldBe
            """{"host":"example1234567890123456789.appsync-api.us-east-1.amazonaws.com","Authorization":"test"}"""
        payload shouldBe "{}"
    }
}
