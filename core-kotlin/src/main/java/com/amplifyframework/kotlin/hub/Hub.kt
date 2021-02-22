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

package com.amplifyframework.kotlin.hub

import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.HubEventFilter
import com.amplifyframework.hub.HubEventFilters
import kotlinx.coroutines.flow.Flow

interface Hub {
    /**
     * Publish an event to a particular channel of the Hub.
     * @param channel Hub channel onto which to publish
     * @param event Event to publish
     */
    fun publish(channel: HubChannel, event: HubEvent<*>)

    /**
     * Subscribe to events on a particular channel.
     * @param channel Channel to listen to
     * @param filter Only subscribe to events that meet this criteria;
     *               if not provided, a "match all" criteria is used
     * @return A flow of matching Hub events
     */
    fun subscribe(
        channel: HubChannel,
        filter: HubEventFilter = HubEventFilters.always()
    ):
        Flow<HubEvent<*>>
}
