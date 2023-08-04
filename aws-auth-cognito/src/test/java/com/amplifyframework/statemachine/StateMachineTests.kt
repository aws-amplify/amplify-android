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

import com.amplifyframework.statemachine.state.Color
import com.amplifyframework.statemachine.state.ColorCounter
import com.amplifyframework.statemachine.state.Counter
import com.amplifyframework.statemachine.state.CounterStateMachine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StateMachineTests {

    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        // reset main dispatcher to the original Main dispatcher
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun testDefaultState() {
        val testLatch = CountDownLatch(1)
        val testMachine = CounterStateMachine.logging()
        testMachine.getCurrentState {
            assertEquals(0, it.value)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testBasicReceive() {
        val testLatch = CountDownLatch(1)
        val testMachine = CounterStateMachine.logging()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        testMachine.send(increment)
        testMachine.getCurrentState {
            assertEquals(1, it.value)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    @Ignore("Fails randomly, needs fixing")
    fun testConcurrentReceive() {
        val testMachine = CounterStateMachine()
        val increment = Counter.Event("increment", Counter.Event.EventType.Increment)
        val decrement = Counter.Event("decrement", Counter.Event.EventType.Decrement)
        (1..1000)
            .map { i ->
                GlobalScope.launch {
                    // TODO: need atomic updates
                    if (i % 2 == 0) testMachine.send(increment) else testMachine.send(decrement)
                }
            }
        val testLatch = CountDownLatch(1)
        testMachine.getCurrentState {
            assertEquals(0, it.value)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testConcurrentReceiveAndRead() {
        val testLatch = CountDownLatch(10)
        val testMachine = CounterStateMachine()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        (1..10)
            .map { i ->
                testMachine.send(increment)
                testMachine.getCurrentState {
                    assertEquals(it.value, i)
                    testLatch.countDown()
                }
            }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testExecuteEffects() {
        val action1Latch = CountDownLatch(1)
        val action2Latch = CountDownLatch(1)
        val action1 = BasicAction("basic") { _, _ ->
            action1Latch.countDown()
        }
        val action2 = BasicAction("basic") { _, _ ->
            action2Latch.countDown()
        }
        val testMachine = CounterStateMachine.logging()
        val event = Counter.Event("1", Counter.Event.EventType.IncrementAndDoActions(listOf(action1, action2)))
        testMachine.send(event)
        assertTrue { action1Latch.await(5, TimeUnit.SECONDS) }
        assertTrue { action2Latch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testDispatchFromAction() {
        val action1Latch = CountDownLatch(1)
        val action2Latch = CountDownLatch(1)
        val action1 = BasicAction("basic") { dispatcher, _ ->
            action1Latch.countDown()
            val action2 = BasicAction("basic") { _, _ ->
                action2Latch.countDown()
            }
            val event = Counter.Event("2", Counter.Event.EventType.IncrementAndDoActions(listOf(action2)))
            dispatcher.send(event)
        }
        val testMachine = CounterStateMachine.logging()
        val event = Counter.Event("1", Counter.Event.EventType.IncrementAndDoActions(listOf(action1)))
        testMachine.send(event)

        assertTrue { action1Latch.await(5, TimeUnit.SECONDS) }
        assertTrue { action2Latch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testCombinedState() {
        val startState = ColorCounter(Color.red, Counter(0), false)
        val resolvedState = ColorCounter.Resolver().logging().resolve(startState, Color.Event.next)
        val expected = ColorCounter(Color.green, Counter(0), false)
        assertEquals(expected, resolvedState.newState)
    }

    @Test
    fun testCombinedResolve() {
        val startState = ColorCounter(Color.blue, Counter(2), false)
        val resolvedState = ColorCounter.Resolver().logging().resolve(startState, Color.Event.next)
        val expected = ColorCounter(Color.yellow, Counter(2), true)
        assertEquals(expected, resolvedState.newState)
    }
}
