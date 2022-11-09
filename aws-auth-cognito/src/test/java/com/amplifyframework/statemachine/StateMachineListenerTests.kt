/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine

import com.amplifyframework.statemachine.state.Counter
import com.amplifyframework.statemachine.state.Counter.Event.EventType.AdjustBy
import com.amplifyframework.statemachine.state.Counter.Event.EventType.Increment
import com.amplifyframework.statemachine.state.CounterEnvironment
import com.amplifyframework.statemachine.state.CounterStateMachine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StateMachineListenerTests {
    private val mainThreadSurrogate = newSingleThreadContext("Main thread")
    private lateinit var stateMachine: CounterStateMachine

    @Before
    fun setUp() {
        stateMachine = CounterStateMachine(Counter.Resolver(), CounterEnvironment.empty)
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        // reset main dispatcher to the original Main dispatcher
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun testNotifyOnListen() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val testLatch = CountDownLatch(2)
        stateMachine.listen(
            StateChangeListenerToken.create(),
            {
                assertEquals(1, it.value)
                testLatch.countDown()
            },
            {
                testLatch.countDown()
            }
        )
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNotifyStateChange() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(2)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen(
            StateChangeListenerToken.create(),
            {
                listenLatch.countDown()
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyNoStateChange() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen(
            StateChangeListenerToken.create(),
            {
                listenLatch.countDown()
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(Counter.Event("2", eventType = AdjustBy(0)))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyUnsubscribe() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken.create()
        stateMachine.listen(
            token,
            {
                listenLatch.countDown()
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.cancel(token)
        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyImmediateCancel() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val token = StateChangeListenerToken.create()
        stateMachine.listen(
            token,
            {
                listenLatch.countDown()
            },
            null
        )

        stateMachine.cancel(token)
        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertFalse { listenLatch.await(5, TimeUnit.SECONDS) }
    }
}
