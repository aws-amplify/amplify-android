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

import com.amplifyframework.aws.appsync.events.WebSocketDisconnectReason
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
internal sealed class WebSocketMessage {
    internal sealed class Send : WebSocketMessage() {
        abstract val id: String
        abstract val type: String

        /**
         * id is not necessary for ConnectionInit. Marked transient so that it isn't included in serialized json
         */
        @Serializable
        internal class ConnectionInit : Send() {
            override val type = "connection_init"
            @Transient override val id = UUID.randomUUID().toString()
        }

        @Serializable
        sealed class Subscription : Send() {

            /**
             * We mark id and type as transient because they can't be included when provided for sigv4 signature
             * We will later add id and type in addition to authorization values needed to send over the websocket
             */
            @Serializable
            internal data class Subscribe(
                @Transient override val id: String = UUID.randomUUID().toString(),
                val channel: String,
            ) : Subscription() {
                @Transient override val type = "subscribe"
            }

            @Serializable
            internal data class Unsubscribe(override val id: String) : Subscription() {
                override val type = "unsubscribe"
            }
        }

        /**
         * We mark id and type as transient because they can't be included when provided for sigv4 signature
         * We will later add id and type in addition to authorization values needed to send over the websocket
         */
        @Serializable
        internal data class Publish(
            @Transient override val id: String = UUID.randomUUID().toString(),
            val channel: String,
            val events: JsonArray,
        ) : Send() {
            @Transient override val type = "publish"
        }
    }

    @Serializable @SerialName("received")
    internal sealed class Received : WebSocketMessage() {

        @Serializable @SerialName("connection_ack")
        internal data class ConnectionAck(val connectionTimeoutMs: Long) : Received()

        @Serializable @SerialName("ka")
        internal data object KeepAlive : Received()

        @Serializable @SerialName("connection_error")
        internal data class ConnectionError(val errors: List<EventsError>) : Received()

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
                override val errors: List<EventsError>
            ) : Subscription(), ErrorContainer

            @Serializable @SerialName("unsubscribe_error")
            internal data class UnsubscribeError(
                override val id: String,
                override val errors: List<EventsError>
            ) : Subscription(), ErrorContainer
        }

        @Serializable @SerialName("publish_success")
        internal data class PublishSuccess(
            override val id: String,
            @SerialName("successful") val successfulEvents: List<SuccessfulEvent>,
            @SerialName("failed") val failedEvents: List<FailedEvent>
        ) : Subscription()

        @Serializable @SerialName("publish_error")
        data class PublishError(
            override val id: String? = null,
            override val errors: List<EventsError>
        ) : Received(), ErrorContainer

        @Serializable @SerialName("error")
        data class Error(
            override val id: String? = null,
            override val errors: List<EventsError>
        ) : Received(), ErrorContainer
    }

    internal data class Closed(val reason: WebSocketDisconnectReason) : WebSocketMessage()

    // All errors contain an id and errors list
    internal interface ErrorContainer {
        val id: String?
        val errors: List<EventsError>
    }
}
