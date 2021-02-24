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

import com.amplifyframework.hub.AWSHubPlugin
import com.amplifyframework.hub.HubChannel.HUB
import com.amplifyframework.hub.HubEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Tests the KotlinHubFacade.
 */
@ExperimentalCoroutinesApi
@FlowPreview
class KotlinHubFacadeTest {
    private val hub = KotlinHubFacade(AWSHubPlugin())

    /**
     * Test publishing and subscribing to events on the Hub.
     */
    @Test
    fun publishAndSubscribe(): Unit = runBlocking {
        awaitAll(
            async {
                hub.subscribe(HUB)
                    .take(100)
                    .toList()
            },
            async {
                delay(200) // allow subscription to start, first.
                for (value in 0..100) {
                    hub.publish(HUB, HubEvent.create("$value"))
                }
            }
        )
    }
}
