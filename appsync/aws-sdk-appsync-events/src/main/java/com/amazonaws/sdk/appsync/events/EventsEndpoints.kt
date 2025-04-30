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
package com.amazonaws.sdk.appsync.events

import com.amazonaws.sdk.appsync.events.data.EventsException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Class representing the Events API endpoint. There are multiple URLs associated with each AppSync endpoint: the
 * appsync server URL, for sending HTTP requests, and the realtime URL, for establishing websocket connections. This
 * class derives the realtime URL from the server URL.
 */
internal class EventsEndpoints(private val endpoint: String) {

    companion object {
        private val standardEndpointRegex =
            "^https://\\w{26}\\.appsync-api\\.\\w{2}(?:-\\w{2,})+-\\d\\.amazonaws.com(?:\\.cn)?/event$".toRegex()
    }

    /**
     * The URL to use for HTTP requests.
     */
    val restEndpoint = try {
        endpoint.toHttpUrl()
    } catch (e: Exception) {
        throw EventsException("Invalid endpoint provided", e)
    }

    val host = restEndpoint.host

    /**
     * The URL to use for IAM Signing of WebSocket requests.
     * While it may be confusing to return https instead of wss, Okhttp expects this and converts.
     */
    val websocketBaseEndpoint: HttpUrl by lazy {
        restEndpoint.newBuilder().apply {
            if (standardEndpointRegex.matches(endpoint)) {
                host(restEndpoint.host.replace("appsync-api", "appsync-realtime-api"))
            }
        }.build()
    }

    /**
     * The URL to use for WebSocket requests.
     * While it may be confusing to return https instead of wss, Okhttp expects this and converts.
     */
    val websocketRealtimeEndpoint: HttpUrl by lazy {
        websocketBaseEndpoint.newBuilder().apply {
            addPathSegment("realtime")
        }.build()
    }

    init {
        if (!this.restEndpoint.isHttps) {
            throw EventsException("AppSync URL must start with https")
        }
    }
}
