package com.amplifyframework.statemachine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.amplifyframework.statemachine.state.Counter
import com.amplifyframework.statemachine.state.Counter.Event.EventType.Increment
import com.amplifyframework.statemachine.state.Counter.Event.EventType.AdjustBy
import com.amplifyframework.statemachine.state.CounterEnvironment
import com.amplifyframework.statemachine.state.CounterStateMachine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        stateMachine.listen({
            assertEquals(1, it.value)
            testLatch.countDown()
        }, {
            testLatch.countDown()
        })
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNotifyStateChange() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(2)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            listenLatch.countDown()
        }, {
            subscribeLatch.countDown()
        })
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyNoStateChange() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            listenLatch.countDown()
        }, {
            subscribeLatch.countDown()
        })
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(Counter.Event("2", eventType = AdjustBy(0)))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyUnsubscribe() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = stateMachine.listen({
            listenLatch.countDown()
        }, {
            subscribeLatch.countDown()
        })
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.cancel(token)
        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testNoNotifyImmediateCancel() {
        stateMachine.send(Counter.Event("1", eventType = Increment))
        val listenLatch = CountDownLatch(1)
        val token = stateMachine.listen({
            listenLatch.countDown()
        }, null)

        stateMachine.cancel(token)
        stateMachine.send(Counter.Event("2", eventType = Increment))
        assertFalse { listenLatch.await(5, TimeUnit.SECONDS) }
    }
}