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
package com.amazonaws.sdk.appsync.events

import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Test

class EventsTest {

    private val testEndpoint = "https://abcabcabcabcabcabcabcabcab.appsync-api.us-east-1.amazonaws.com/event"
    private val mockConnectAuthorizer = mockk<AppSyncAuthorizer>()
    private val mockSubscribeAuthorizer = mockk<AppSyncAuthorizer>()
    private val mockPublishAuthorizer = mockk<AppSyncAuthorizer>()
    private val events = Events(testEndpoint)

    @Test
    fun `test Events constructor sets endpoint correctly`() {
        testEndpoint shouldBe events.endpoint
    }

    @Test
    fun `test createRestClient returns valid client`() {
        val restClient = events.createRestClient(
            publishAuthorizer = mockPublishAuthorizer
        )

        restClient.publishAuthorizer shouldBe mockPublishAuthorizer
        restClient.options.loggerProvider shouldBe null
        restClient.options.okHttpConfigurationProvider shouldBe null
    }

    @Test
    fun `test createRestClient with custom options returns valid client`() {
        val customOptions = mockk<Events.Options.Rest>(relaxed = true)

        val restClient = events.createRestClient(
            publishAuthorizer = mockPublishAuthorizer,
            options = customOptions
        )

        restClient.publishAuthorizer shouldBe mockPublishAuthorizer
        restClient.options shouldBe customOptions
    }

    @Test
    fun `test createWebSocketClient returns valid client`() {
        val webSocketClient = events.createWebSocketClient(
            connectAuthorizer = mockConnectAuthorizer,
            subscribeAuthorizer = mockSubscribeAuthorizer,
            publishAuthorizer = mockPublishAuthorizer
        )

        webSocketClient.publishAuthorizer shouldBe mockPublishAuthorizer
        webSocketClient.connectAuthorizer shouldBe mockConnectAuthorizer
        webSocketClient.subscribeAuthorizer shouldBe mockSubscribeAuthorizer
        webSocketClient.options.loggerProvider shouldBe null
        webSocketClient.options.okHttpConfigurationProvider shouldBe null
    }

    @Test
    fun `test createWebSocketClient with custom options returns valid client`() {
        val customOptions = mockk<Events.Options.WebSocket>(relaxed = true)

        val webSocketClient = events.createWebSocketClient(
            connectAuthorizer = mockConnectAuthorizer,
            subscribeAuthorizer = mockSubscribeAuthorizer,
            publishAuthorizer = mockPublishAuthorizer,
            options = customOptions
        )

        webSocketClient.publishAuthorizer shouldBe mockPublishAuthorizer
        webSocketClient.connectAuthorizer shouldBe mockConnectAuthorizer
        webSocketClient.subscribeAuthorizer shouldBe mockSubscribeAuthorizer
        webSocketClient.options shouldBe customOptions
    }
}
