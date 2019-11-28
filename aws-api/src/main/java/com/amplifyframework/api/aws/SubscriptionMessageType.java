/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;

/**
 * An enumeration of the values that are possible in the "type" field
 * of a subscription message.
 * @see <a href="http://bit.ly/gql-ws-message-types">GraphQL Over WebSocket Message Types</a>
 * @see <a href="http://bit.ly/gql-ws-protocol">GraphQL Over WebSocket Protocol</a>
 */
enum SubscriptionMessageType {

    /**
     * Client requests initialization of a connection, to the server.
     */
    CONNECTION_INIT("connection_init"),

    /**
     * The server acknowledges a client's request to init connection.
     */
    CONNECTION_ACK("connection_ack"),

    /**
     * The server informs the client that there was an error
     * forming a connection.
     */
    CONNECTION_ERROR("connection_error"),

    /**
     * The server pokes the client to tell it to keep the connection alive.
     */
    CONNECTION_KEEP_ALIVE("ka"),

    /**
     * The client announces its desire to terminate a connection with the server.
     */
    CONNECTION_TERMINATE("connection_terminate"),

    /**
     * Client requests a new subscription with the server.
     */
    SUBSCRIPTION_START("start"),

    /**
     * Server acknowledges client's request to start a subscription.
     */
    SUBSCRIPTION_ACK("start_ack"),

    /**
     * Server sends subscription data to the client.
     */
    SUBSCRIPTION_DATA("data"),

    /**
     * Server tells client that there was an error with a subscription.
     */
    SUBSCRIPTION_ERROR("error"),

    /**
     * Server tells client that a subscription has ended.
     */
    SUBSCRIPTION_COMPLETE("complete"),

    /**
     * Client requests that a subscription be stopped.
     */
    SUBSCRIPTION_STOP("stop");

    private final String value;

    SubscriptionMessageType(String value) {
        this.value = value;
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }

    /**
     * Gets an enumerated SubscriptionMessageType from a type value.
     * @param value A type value, as found in JSON
     * @return A qualified subscription message type
     */
    public static SubscriptionMessageType from(String value) {
        for (SubscriptionMessageType possibleMatch : SubscriptionMessageType.values()) {
            if (possibleMatch.value.equals(value)) {
                return possibleMatch;
            }
        }

        throw new ApiException("No such subscription message type: " + value);
    }
}
