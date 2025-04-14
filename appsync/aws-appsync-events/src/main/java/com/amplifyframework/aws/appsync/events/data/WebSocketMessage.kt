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

package com.amplifyframework.aws.appsync.events.data

import com.amplifyframework.aws.appsync.events.DisconnectReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal sealed class WebSocketMessage {
    internal sealed class Send : WebSocketMessage() {
        abstract val type: String

        @Serializable
        internal class ConnectionInit : Send() {
            override val type = "connection_init"
        }

        @Serializable
        sealed class Subscription : Send() {
            abstract val id: String

            @Serializable
            internal data class Subscribe(
                override val id: String,
                val channel: String,
                val authorization: Map<String, String>
            ) : Subscription() {
                override val type = "subscribe"
            }

            @Serializable
            internal data class Unsubscribe(override val id: String) : Subscription() {
                override val type = "unsubscribe"
            }

            @Serializable
            internal data class Publish(
                val id: String,
                val channel: String,
                val events: List<Boolean>
            ) : Send() {
                override val type = "publish"
            }
        }
    }

    @Serializable @SerialName("received")
    internal sealed class Received : WebSocketMessage() {

        @Serializable @SerialName("connection_ack")
        internal data class ConnectionAck(val connectionTimeoutMs: Long) : Received()

        @Serializable @SerialName("ka")
        internal data object KeepAlive : Received()

        @Serializable @SerialName("connection_error")
        internal data class ConnectionError(val errors: List<WebSocketError>) : Received()

        @Serializable @SerialName("connection_closed")
        internal data object ConnectionClosed : Received()

        @Serializable
        internal sealed class Subscription : Received() {
            abstract val id: String

            @Serializable @SerialName("data")
            internal data class Data(override val id: String, val event: JsonElement) : Subscription()

            @Serializable @SerialName("subscribe_success")
            internal data class SubscribeSuccess(override val id: String) : Subscription()

            @Serializable @SerialName("unsubscribe_success")
            internal data class UnsubscribeSuccess(override val id: String) : Subscription()

            @Serializable @SerialName("subscribe_error")
            internal data class SubscribeError(
                override val id: String,
                val errors: List<WebSocketError>
            ) : Subscription()

            @Serializable @SerialName("unsubscribe_error")
            internal data class UnsubscribeError(
                override val id: String,
                val errors: List<WebSocketError>
            ) : Subscription()
        }

        @Serializable @SerialName("error")
        data class Error(val errors: List<WebSocketError>)
    }

    internal data class Closed(val reason: DisconnectReason) : WebSocketMessage()
}

@Serializable
data class WebSocketError(val errorType: String, val message: String? = null) {

    // fallback message is only used if WebSocketError didn't provide a message
    fun toEventsException(fallbackMessage: String? = null): EventsException {
        return when (errorType) {
            "UnauthorizedException" -> UnauthorizedException(message ?: fallbackMessage)
            else -> EventsException(message = "$errorType: $message")
        }
    }
}
