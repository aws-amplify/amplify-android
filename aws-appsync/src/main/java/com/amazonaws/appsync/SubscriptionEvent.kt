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
package com.amazonaws.appsync

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.api.graphql.GraphQLResponse

/**
 * Events emitted by a GraphQL subscription flow.
 *
 * Lifecycle: the flow emits [Connection.Connecting] → [Connection.Connected] → [Data]*
 * and then either completes normally (user cancel, server complete, client close) or
 * throws an [ApiException] (network, auth, timeout, etc.).
 *
 * For client-wide WebSocket connection state, observe [AmplifyAppSyncClient.events].
 */
@ExperimentalAmplifyApi
sealed class SubscriptionEvent<out T> {

    /**
     * A data message received from the subscription.
     * @param response The GraphQL response, which may contain data, errors, or both (partial success).
     */
    data class Data<T>(val response: GraphQLResponse<T>) : SubscriptionEvent<T>()

    /**
     * Subscription establishment lifecycle events.
     */
    sealed class Connection : SubscriptionEvent<Nothing>() {
        /** The subscription is being established (WebSocket connecting + registration in progress). */
        data object Connecting : Connection()

        /** The subscription is established and receiving data. */
        data object Connected : Connection()
    }
}
