/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.events.data.EventsException
import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class EventsEndpointsTest {

    @Test
    fun `test standard endpoint`() {
        val endpoints = EventsEndpoints(
            "https://abcabcabcabcabcabcabcabcab.appsync-api.us-east-1.amazonaws.com/event"
        )
        endpoints.restEndpoint shouldBe
            "https://abcabcabcabcabcabcabcabcab.appsync-api.us-east-1.amazonaws.com/event".toHttpUrl()
        endpoints.websocketBaseEndpoint shouldBe
            "https://abcabcabcabcabcabcabcabcab.appsync-realtime-api.us-east-1.amazonaws.com/event".toHttpUrl()
        endpoints.websocketRealtimeEndpoint shouldBe
            "https://abcabcabcabcabcabcabcabcab.appsync-realtime-api.us-east-1.amazonaws.com/event/realtime".toHttpUrl()
        endpoints.host shouldBe "abcabcabcabcabcabcabcabcab.appsync-api.us-east-1.amazonaws.com"
    }

    @Test
    fun `test custom endpoint`() {
        val endpoints = EventsEndpoints(
            "https://amazon.com/event"
        )
        endpoints.restEndpoint shouldBe
            "https://amazon.com/event".toHttpUrl()
        endpoints.websocketBaseEndpoint shouldBe
            "https://amazon.com/event".toHttpUrl()
        endpoints.websocketRealtimeEndpoint shouldBe
            "https://amazon.com/event/realtime".toHttpUrl()
        endpoints.host shouldBe "amazon.com"
    }

    @Test(expected = EventsException::class)
    fun `test invalid endpoint`() {
        EventsEndpoints("bad endpoint")
    }

    @Test(expected = EventsException::class)
    fun `test http only endpoint`() {
        EventsEndpoints("http://amazon.com")
    }
}
