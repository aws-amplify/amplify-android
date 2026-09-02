/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Messages of AppSync's `graphql-ws` subscription protocol.
 *
 * A private reimplementation of the plugin's `SubscriptionMessageType` plus its ad-hoc `JSONObject`
 * construction, modelled as a sealed hierarchy so the receive side is exhaustive and an unrecognised
 * message cannot be silently mistaken for a known one.
 *
 * Only the wire shapes the client actually uses are modelled. `connection_terminate` is absent
 * because the client closes the socket rather than negotiating termination.
 */
internal sealed interface AppSyncWebSocketMessage {

    /** Messages the client sends. */
    sealed interface Outbound : AppSyncWebSocketMessage {
        fun toJson(): String
    }

    /** Opens the connection. Sent immediately on socket open. */
    data object ConnectionInit : Outbound {
        override fun toJson() = """{"type":"$TYPE_CONNECTION_INIT"}"""
    }

    /**
     * Registers one subscription.
     *
     * @param id Correlates every later message for this subscription.
     * @param query The serialized GraphQL request, as `GraphQLRequest.content` produces it.
     * @param authorizationHeaders The same headers an equivalent HTTP request would carry. AppSync
     *   authorizes each subscription separately from the connection, so these are per-message.
     */
    data class Start(
        val id: String,
        val query: String,
        val authorizationHeaders: Map<String, String>
    ) : Outbound {
        override fun toJson(): String {
            val authorization = JsonObject().apply {
                authorizationHeaders.forEach { (name, value) -> addProperty(name, value) }
            }
            val extensions = JsonObject().apply { add("authorization", authorization) }
            val payload = JsonObject().apply {
                addProperty("data", query)
                add("extensions", extensions)
            }
            return JsonObject().apply {
                addProperty("id", id)
                addProperty("type", TYPE_START)
                add("payload", payload)
            }.toString()
        }
    }

    /** Unregisters one subscription. Sent on flow cancellation. */
    data class Stop(val id: String) : Outbound {
        // Built rather than interpolated, so an id containing a quote cannot produce invalid JSON.
        override fun toJson() = JsonObject().apply {
            addProperty("id", id)
            addProperty("type", TYPE_STOP)
        }.toString()
    }

    /** Messages the service sends. */
    sealed interface Inbound : AppSyncWebSocketMessage

    /**
     * The connection is established.
     *
     * @param connectionTimeoutMs How long the service will hold the connection without traffic. The
     *   client resets its keep-alive watchdog to this.
     */
    data class ConnectionAck(val connectionTimeoutMs: Long) : Inbound

    /** The connection was rejected. Terminal for every subscription on it. */
    data class ConnectionError(val errors: List<WireError>) : Inbound

    /** Keep-alive. Carries no data; its only purpose is resetting the watchdog. */
    data object KeepAlive : Inbound

    /** One subscription was registered successfully. */
    data class StartAck(val id: String) : Inbound

    /** A data message for one subscription. [payload] is the raw GraphQL response JSON. */
    data class Data(val id: String, val payload: String) : Inbound

    /** One subscription failed. Terminal for that subscription only. */
    data class Error(val id: String?, val errors: List<WireError>) : Inbound

    /**
     * One error from the service.
     *
     * @param message The human-readable reason.
     * @param errorType The service's error classification, from the error's `extensions.errorType`.
     *   Null when the service did not supply one. This is what distinguishes an authorization failure
     *   — which another auth mode might satisfy — from a failure that would recur regardless.
     */
    data class WireError(val message: String, val errorType: String?)

    /** One subscription ended normally, at the service's initiative. */
    data class Complete(val id: String) : Inbound

    /**
     * A message this client does not model. Modelled explicitly rather than discarded during parsing so
     * that an unexpected wire change is a value a caller can match on.
     *
     * TODO: log these — nothing in this module logs today, so an unknown frame currently passes
     *  unnoticed.
     */
    data class Unknown(val type: String?, val raw: String) : Inbound

    /**
     * The socket closed. **Synthetic** — never parsed from a frame; [AppSyncWebSocket] emits it so
     * that closure arrives on the same flow as everything else. Without it, a caller awaiting a
     * specific reply would hang when the socket dies instead of failing.
     *
     * @param cause Why it closed, or null for a clean client-initiated close.
     */
    data class Closed(val cause: AppSyncException?) : Inbound

    companion object {
        const val TYPE_CONNECTION_INIT = "connection_init"
        const val TYPE_START = "start"
        const val TYPE_STOP = "stop"

        private const val TYPE_CONNECTION_ACK = "connection_ack"
        private const val TYPE_CONNECTION_ERROR = "connection_error"
        private const val TYPE_KEEP_ALIVE = "ka"
        private const val TYPE_START_ACK = "start_ack"
        private const val TYPE_DATA = "data"
        private const val TYPE_ERROR = "error"
        private const val TYPE_COMPLETE = "complete"

        private const val DEFAULT_CONNECTION_TIMEOUT_MS = 300_000L

        /**
         * Parses an inbound frame. Never throws: a frame that cannot be understood becomes [Unknown],
         * because one malformed message must not take down a connection carrying other healthy
         * subscriptions.
         */
        fun parse(text: String): Inbound = try {
            val root = JsonParser.parseString(text).asJsonObject
            val type = root.get("type")?.takeIf { !it.isJsonNull }?.asString
            val id = root.get("id")?.takeIf { !it.isJsonNull }?.asString

            when (type) {
                TYPE_CONNECTION_ACK -> ConnectionAck(
                    connectionTimeoutMs = root.getAsJsonObject("payload")
                        ?.get("connectionTimeoutMs")?.takeIf { it.isJsonPrimitive }?.asLong
                        ?: DEFAULT_CONNECTION_TIMEOUT_MS
                )
                TYPE_CONNECTION_ERROR -> ConnectionError(root.errorMessages())
                TYPE_KEEP_ALIVE -> KeepAlive
                TYPE_START_ACK -> id?.let { StartAck(it) } ?: Unknown(type, text)
                // The payload is passed through as raw JSON rather than deserialized here: only the
                // subscriber knows the request's response type.
                TYPE_DATA -> id?.let { Data(it, root.get("payload")?.toString() ?: "") }
                    ?: Unknown(type, text)
                TYPE_ERROR -> Error(id, root.errorMessages())
                TYPE_COMPLETE -> id?.let { Complete(it) } ?: Unknown(type, text)
                else -> Unknown(type, text)
            }
        } catch (error: Exception) {
            Unknown(type = null, raw = text)
        }

        /**
         * AppSync puts errors in `payload.errors` on a connection error and in `payload.errors` or a
         * top-level `errors` on a subscription error, so both shapes are read.
         */
        private fun JsonObject.errorMessages(): List<WireError> {
            val errors = getAsJsonObject("payload")?.getAsJsonArray("errors")
                ?: getAsJsonArray("errors")
                ?: return emptyList()

            return errors.mapNotNull { element ->
                when {
                    element.isJsonPrimitive -> WireError(element.asString, errorType = null)
                    element.isJsonObject -> {
                        val obj = element.asJsonObject
                        WireError(
                            message = obj.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                                ?: obj.toString(),
                            // Read both locations. A subscription error frame carries a GraphQL response
                            // shape, which nests the classification under extensions; connection-level
                            // frames put it directly on the error object.
                            errorType = obj.get("errorType")?.takeIf { it.isJsonPrimitive }?.asString
                                ?: obj.getAsJsonObject("extensions")
                                    ?.get("errorType")?.takeIf { it.isJsonPrimitive }?.asString
                        )
                    }
                    else -> null
                }
            }
        }
    }
}
