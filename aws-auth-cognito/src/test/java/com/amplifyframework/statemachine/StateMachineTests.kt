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

import app.cash.turbine.test
import com.amplifyframework.statemachine.state.Color
import com.amplifyframework.statemachine.state.ColorCounter
import com.amplifyframework.statemachine.state.Counter
import com.amplifyframework.statemachine.state.CounterStateMachine
import com.amplifyframework.testutils.await
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StateMachineTests {

    private val timeout = 1.seconds
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
        testMachine.getCurrentState { state ->
            state.value shouldBe 0
            testLatch.countDown()
        }
        testLatch.await(timeout) shouldBe true
    }

    @Test
    fun `test default state suspending`() = runTest {
        val testMachine = CounterStateMachine.logging()
        val currentState = testMachine.getCurrentState()
        currentState.value shouldBe 0
    }

    @Test
    fun testBasicReceive() = runTest {
        val testLatch = CountDownLatch(1)
        val testMachine = CounterStateMachine.logging()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        testMachine.send(increment)
        testMachine.getCurrentState { state ->
            state.value shouldBe 1
            testLatch.countDown()
        }
        testLatch.await(timeout) shouldBe true
    }

    @Test
    fun `test basic receive suspending`() = runTest {
        val testMachine = CounterStateMachine.logging()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        testMachine.send(increment)
        val state = testMachine.getCurrentState()
        state.value shouldBe 1
    }

    @Test
    fun testConcurrentReceiveAndRead() {
        val testLatch = CountDownLatch(10)
        val testMachine = CounterStateMachine()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        for (i in 1..10) {
            testMachine.send(increment)
            testMachine.getCurrentState { state ->
                state.value shouldBe i
                testLatch.countDown()
            }
        }
        testLatch.await(timeout) shouldBe true
    }

    @Test
    fun `test concurrent receive and read suspending`() = runTest {
        val testMachine = CounterStateMachine()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)
        for (i in 1..10) {
            testMachine.send(increment)
            val state = testMachine.getCurrentState()
            state.value shouldBe i
        }
    }

    @Test
    fun `test collecting state from flow`() = runTest {
        val testMachine = CounterStateMachine.logging()
        val increment = Counter.Event("1", Counter.Event.EventType.Increment)

        testMachine.state.test {
            awaitItem().value shouldBe 0
            testMachine.send(increment)
            awaitItem().value shouldBe 1
            testMachine.send(increment)
            awaitItem().value shouldBe 2
        }
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
        action1Latch.await(timeout) shouldBe true
        action2Latch.await(timeout) shouldBe true
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

        action1Latch.await(timeout) shouldBe true
        action2Latch.await(timeout) shouldBe true
    }

    @Test
    fun testCombinedState() {
        val startState = ColorCounter(Color.red, Counter(0), false)
        val resolvedState = ColorCounter.Resolver().logging().resolve(startState, Color.Event.next)
        val expected = ColorCounter(Color.green, Counter(0), false)
        resolvedState.newState shouldBe expected
    }

    @Test
    fun testCombinedResolve() {
        val startState = ColorCounter(Color.blue, Counter(2), false)
        val resolvedState = ColorCounter.Resolver().logging().resolve(startState, Color.Event.next)
        val expected = ColorCounter(Color.yellow, Counter(2), true)
        resolvedState.newState shouldBe expected
    }
}
