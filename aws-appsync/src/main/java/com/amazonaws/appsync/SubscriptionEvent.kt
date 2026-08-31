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
 * Lifecycle: the flow emits [Connecting] → [Connected] → [Data]* and then either completes normally
 * (consumer cancellation, server complete, client close) or throws an [AppSyncException] (network,
 * auth, timeout). All errors are terminal — the stream ends and the consumer must re-subscribe.
 *
 * There is deliberately no `Disconnected` event: a stream ends by completing or throwing, so a
 * disconnect event would be redundant with termination.
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

    /** The subscription is being established (WebSocket connecting + registration in progress). */
    data object Connecting : SubscriptionEvent<Nothing>()

    /** The subscription is established and receiving data. */
    data object Connected : SubscriptionEvent<Nothing>()
}
