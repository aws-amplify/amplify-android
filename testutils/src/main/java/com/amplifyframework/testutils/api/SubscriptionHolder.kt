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

package com.amplifyframework.testutils.api

import com.amplifyframework.api.graphql.GraphQLBehavior
import com.amplifyframework.api.graphql.GraphQLOperation
import com.amplifyframework.api.graphql.GraphQLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

fun <T> GraphQLBehavior.subscribe(request: GraphQLRequest<T>): SubscriptionHolder<T> {
    val subscriptionEstablished = CompletableDeferred<Unit>()
    val subscriptionFailed = CompletableDeferred<Exception>()
    val subscriptionComplete = CompletableDeferred<Unit>()
    val dataChannel = Channel<T>(capacity = 10)
    val operation = this.subscribe(
        request,
        {
            subscriptionEstablished.complete(Unit)
        },
        {
            if (it.hasData()) {
                dataChannel.trySend(it.data)
            }
        },
        {
            subscriptionEstablished.completeExceptionally(it)
            subscriptionFailed.complete(it)
        },
        {
            subscriptionComplete.complete(Unit)
        }
    )
    return SubscriptionHolder<T>(
        operation = operation,
        subscriptionEstablished = subscriptionEstablished,
        subscriptionFailed = subscriptionFailed,
        subscriptionComplete = subscriptionComplete,
        dataChannel = dataChannel
    )
}

class SubscriptionHolder<T>(
    private val operation: GraphQLOperation<T>?,
    val subscriptionEstablished: Deferred<Unit>,
    val subscriptionFailed: Deferred<Exception>,
    val subscriptionComplete: Deferred<Unit>,
    val dataChannel: ReceiveChannel<T>
) : AutoCloseable {
    fun cancel() {
        dataChannel.cancel()
        operation?.cancel()
    }

    override fun close() = cancel()
}
