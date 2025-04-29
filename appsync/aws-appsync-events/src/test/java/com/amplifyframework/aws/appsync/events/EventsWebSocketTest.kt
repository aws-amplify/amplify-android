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

import com.amplifyframework.aws.appsync.core.AppSyncRequest
import io.kotest.matchers.shouldBe
import org.junit.Test
import okhttp3.Request

class EventsWebSocketTest {
    private lateinit var eventsEndpoints: EventsEndpoints
    private lateinit var request: Request

    @Test
    fun `test ConnectAppSyncRequest values`() {
        // Setup test data
        eventsEndpoints = EventsEndpoints(
            "https://11111111111111111111111111.appsync-api.us-east-1.amazonaws.com/event"
        )
        request = Request.Builder()
            .url(eventsEndpoints.websocketRealtimeEndpoint)
            .header("key", "value")
            .build()

        val connectRequest = ConnectAppSyncRequest(eventsEndpoints, request)

        connectRequest.method shouldBe  AppSyncRequest.HttpMethod.POST
        connectRequest.body shouldBe "{}"
        connectRequest.headers shouldBe mapOf("key" to "value")
        connectRequest.url shouldBe eventsEndpoints.websocketBaseEndpoint.toString()
    }

}
