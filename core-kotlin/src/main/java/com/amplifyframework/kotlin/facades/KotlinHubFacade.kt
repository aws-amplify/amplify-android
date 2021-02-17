/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.facades

import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubCategoryBehavior as Delegate
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.HubEventFilter
import com.amplifyframework.kotlin.Hub
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class KotlinHubFacade(private val delegate: Delegate = Amplify.Hub) : Hub {
    override fun publish(channel: HubChannel, event: HubEvent<*>) {
        delegate.publish(channel, event)
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun subscribe(channel: HubChannel, filter: HubEventFilter): Flow<HubEvent<*>> =
        callbackFlow {
            val token = delegate.subscribe(channel, filter) { sendBlocking(it) }
            awaitClose { delegate.unsubscribe(token) }
        }
}
