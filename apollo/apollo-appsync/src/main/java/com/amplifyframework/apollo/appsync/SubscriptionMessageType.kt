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

/**
 * An enumeration of the values that are possible in the "type" field
 * of a subscription message.
 * @see [GraphQL Over WebSocket Message Types](http://bit.ly/gql-ws-message-types)
 * @see [GraphQL Over WebSocket Protocol](http://bit.ly/gql-ws-protocol)
 */
internal enum class SubscriptionMessageType(val value: String) {
    /**
     * Client requests initialization of a connection, to the server.
     */
    ConnectionInit("connection_init"),

    /**
     * The server acknowledges a client's request to init connection.
     */
    ConnectionAck("connection_ack"),

    /**
     * The server informs the client that there was an error
     * forming a connection.
     */
    ConnectionError("connection_error"),

    /**
     * The server pokes the client to tell it to keep the connection alive.
     */
    ConnectionKeepAlive("ka"),

    /**
     * The client announces its desire to terminate a connection with the server.
     */
    ConnectionTerminate("connection_terminate"),

    /**
     * Client requests a new subscription with the server.
     */
    SubscriptionStart("start"),

    /**
     * Server acknowledges client's request to start a subscription.
     */
    SubscriptionAck("start_ack"),

    /**
     * Server sends subscription data to the client.
     */
    SubscriptionData("data"),

    /**
     * Server tells client that there was an error with a subscription.
     */
    SubscriptionError("error"),

    /**
     * Server tells client that a subscription has ended.
     */
    SubscriptionComplete("complete"),

    /**
     * Client requests that a subscription be stopped.
     */
    SubscriptionStop("stop");

    override fun toString() = value
}

internal val String.messageType: SubscriptionMessageType?
    get() = SubscriptionMessageType.entries.find { it.value == this }
