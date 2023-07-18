/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.plugins.core

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Before
import org.junit.Test

class AuthHubEventEmitterTest {
    private val TIMEOUT = 10L
    private lateinit var emitter: AuthHubEventEmitter

    @Before
    fun setup() {
        emitter = AuthHubEventEmitter()
    }

    @Test
    fun testEmitMultipleEvents() {
        val latch = CountDownLatch(2)

        Amplify.Hub.subscribe(HubChannel.AUTH) {
            when (AuthChannelEventName.valueOf(it.name)) {
                AuthChannelEventName.SIGNED_IN, AuthChannelEventName.SIGNED_OUT -> latch.countDown()
                else -> fail()
            }
        }

        emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.name)
        emitter.sendHubEvent(AuthChannelEventName.SIGNED_OUT.name)

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS))
    }

    @Test
    fun testEmitDuplicateEvents() {
        val latch = CountDownLatch(2)
        val count = AtomicInteger(0)

        Amplify.Hub.subscribe(HubChannel.AUTH) {
            when (AuthChannelEventName.valueOf(it.name)) {
                AuthChannelEventName.SIGNED_IN -> {
                    count.getAndIncrement()
                    latch.countDown()
                }
                else -> fail()
            }
        }

        emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.name)
        emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.name)

        latch.await(TIMEOUT, TimeUnit.SECONDS)
        assertEquals(1, count.get())
    }

    @Test
    fun testEmitUnknownEvent() {
        val latch = CountDownLatch(1)

        Amplify.Hub.subscribe(HubChannel.AUTH) {
            when (it.name) {
                "test" -> latch.countDown()
                else -> fail()
            }
        }

        emitter.sendHubEvent("test")

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS))
    }

    @Test
    fun testSubscribeUnknownChannel() {
        val latch = CountDownLatch(1)

        Amplify.Hub.subscribe(HubChannel.STORAGE) {
            latch.countDown()
        }

        emitter.sendHubEvent("test")

        assertFalse(latch.await(TIMEOUT, TimeUnit.SECONDS))
    }
}
