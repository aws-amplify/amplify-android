/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.amplifyframework.eventenrichment

import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.SessionManager
import com.amplifyframework.eventenrichment.session.TimeoutHandle
import com.amplifyframework.eventenrichment.session.TimeoutScheduler
import com.amplifyframework.foundation.result.get
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.junit.Test

class EventEnrichmentClientSessionEventsTest {

    private class CapturingSink : EventSink {
        val events = CopyOnWriteArrayList<EnrichedEvent>()
        override fun send(event: EnrichedEvent) {
            events += event
        }
    }

    /** Dispatcher that queues every block until [drain] is called. */
    private class QueueingDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.addLast(block)
        }
        fun drain() {
            while (queue.isNotEmpty()) queue.removeFirst().run()
        }
    }

    private val sink = CapturingSink()
    private var clock = Instant.parse("2026-07-06T19:51:59.839Z")
    private var idSequence = 0

    // Captured so a test can fire the pause timeout by hand.
    private var scheduledTimeout: (() -> Unit)? = null

    private fun sessionManager() = SessionManager(
        appId = "my-app",
        sessionTimeout = 5.seconds,
        now = { clock },
        generateId = { "session${idSequence++}0-1111-2222-3333-444455556666" },
        scheduler = TimeoutScheduler { _, action ->
            scheduledTimeout = action
            TimeoutHandle { }
        }
    )

    private fun client(
        sink: EventSink = this.sink,
        autoSessionTracking: Boolean = true,
        sinkDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    ) = EventEnrichmentClient(
        appMetadata = AppMetadata(appId = "my-app"),
        deviceMetadata = DeviceMetadata(platform = "Android"),
        sdkMetadata = SdkMetadata(name = "amplify-android", version = "2.0.0"),
        clientId = "device-uuid",
        sink = sink,
        sessionManager = sessionManager(),
        autoSessionTracking = autoSessionTracking,
        application = null,
        clock = { clock },
        generateEventId = { "event${idSequence++}" },
        scope = scope,
        sinkDispatcher = sinkDispatcher
    )

    private fun types() = sink.events.map { it.eventType }

    @Test
    fun `constructor queues the launch start without calling the sink`() {
        val dispatcher = QueueingDispatcher()
        client(sinkDispatcher = dispatcher)

        sink.events.shouldBeEmpty()
        dispatcher.drain()

        types() shouldBe listOf(START)
        val start = sink.events.single()
        start.session.stopTimestamp shouldBe null
        start.session.duration shouldBe null
        start.attributes shouldBe emptyMap()
    }

    @Test
    fun `record without a session sends the start first`() {
        val client = client(autoSessionTracking = false)
        sink.events.shouldBeEmpty()

        val event = client.record("x").get()

        types() shouldBe listOf(START, "x")
        sink.events[0].session.id shouldBe event.session.id
    }

    @Test
    fun `stopSession sends one stop carrying stop timestamp and duration`() {
        val client = client()
        clock += 10_000.milliseconds
        client.stopSession()
        client.stopSession()

        types() shouldBe listOf(START, STOP)
        val stop = sink.events[1].session
        stop.id shouldBe sink.events[0].session.id
        stop.stopTimestamp shouldBe "2026-07-06T19:52:09.839Z"
        stop.duration shouldBe 10_000L
    }

    @Test
    fun `startSession while running sends the stop before the new start`() {
        val client = client()
        clock += 1_000.milliseconds
        client.startSession()

        types() shouldBe listOf(START, STOP, START)
        sink.events[1].session.id shouldBe sink.events[0].session.id
        sink.events[2].session.id shouldNotBe sink.events[0].session.id
    }

    @Test
    fun `quick restarts alternate stop and start with matching ids`() {
        val client = client()
        repeat(3) { client.startSession() }

        types() shouldBe listOf(START, STOP, START, STOP, START, STOP, START)
        sink.events.zipWithNext().filter { (a, _) -> a.eventType == START }.forEach { (start, stop) ->
            stop.session.id shouldBe start.session.id
        }
    }

    @Test
    fun `pause and resume within the timeout sends nothing`() {
        val client = client()
        client.handleAppPaused()
        clock += 4_999.milliseconds
        client.handleAppResumed()

        types() shouldBe listOf(START)
    }

    @Test
    fun `timeout stop is stamped at the pause and resume then sends a start`() {
        val client = client()
        clock += 2_000.milliseconds
        client.handleAppPaused()
        clock += 5_000.milliseconds
        scheduledTimeout.shouldNotBeNull().invoke()
        clock += 60_000.milliseconds
        client.handleAppResumed()

        types() shouldBe listOf(START, STOP, START)
        sink.events[1].session.stopTimestamp shouldBe "2026-07-06T19:52:01.839Z"
        sink.events[1].session.duration shouldBe 2_000L
        sink.events[2].session.id shouldNotBe sink.events[0].session.id
    }

    @Test
    fun `resume after an expired pause stops and starts even if the timer never fired`() {
        val client = client()
        clock += 2_000.milliseconds
        client.handleAppPaused()
        clock += 5_000.milliseconds
        client.handleAppResumed()

        types() shouldBe listOf(START, STOP, START)
        sink.events[1].session.duration shouldBe 2_000L
    }

    @Test
    fun `record during an expired pause stops and starts before the record`() {
        val client = client()
        client.handleAppPaused()
        clock += 5_000.milliseconds

        val event = client.record("late").get()

        types() shouldBe listOf(START, STOP, START, "late")
        event.session.id shouldBe sink.events[2].session.id
    }

    @Test
    fun `resume after an explicit stop sends nothing`() {
        val client = client()
        client.stopSession()
        client.handleAppPaused()
        client.handleAppResumed()

        types() shouldBe listOf(START, STOP)
    }

    @Test
    fun `close sends the final stop and nothing after it`() {
        val client = client()
        client.record("x")
        client.close()
        client.record("late")
        client.startSession()
        client.stopSession()
        client.handleAppResumed()
        client.close()

        types() shouldBe listOf(START, "x", STOP)
    }

    @Test
    fun `close does not drop sends that have not run yet`() {
        val dispatcher = QueueingDispatcher()
        val client = client(sinkDispatcher = dispatcher)
        client.record("in_flight")
        sink.events.shouldBeEmpty()

        client.close()
        dispatcher.drain()

        types() shouldBe listOf(START, "in_flight", STOP)
    }

    @Test
    fun `close after an explicit stop does not send a second stop`() {
        val client = client()
        client.stopSession()
        client.close()

        types() shouldBe listOf(START, STOP)
    }

    @Test
    fun `a boundary raised after close never reaches the sink`() {
        val client = client()
        client.close()

        // Bypasses the client guard, like a lifecycle callback racing close().
        client.sessionManager.startSession()

        types() shouldBe listOf(START, STOP)
    }

    @Test
    fun `timeout that fires after a resume sends no stop`() {
        val client = client()
        client.handleAppPaused()
        val staleTimeout = scheduledTimeout.shouldNotBeNull()
        client.handleAppResumed()

        staleTimeout()

        types() shouldBe listOf(START)
    }

    @Test
    fun `boundary events carry user id and globals as of the transition`() {
        val client = client()
        client.setUserId("user-1")
        client.addGlobalAttribute("theme", "dark")
        client.addGlobalMetric("battery", 0.5)
        client.stopSession()
        client.setUserId("user-2")
        client.addGlobalAttribute("theme", "light")

        val (start, stop) = sink.events
        start.userId shouldBe null
        start.attributes shouldBe emptyMap()
        stop.userId shouldBe "user-1"
        stop.attributes shouldBe mapOf("theme" to "dark")
        stop.metrics shouldBe mapOf("battery" to 0.5)
    }

    @Test
    fun `a failing sink send does not block later sends`() {
        val arrivals = CopyOnWriteArrayList<String>()
        val client = client(
            sink = EventSink { event ->
                arrivals += event.eventType
                if (event.eventType == "boom") error("sink failure")
            }
        )

        client.record("boom").get().eventType shouldBe "boom"
        client.record("after")

        arrivals shouldBe listOf(START, "boom", "after")
    }

    @Test
    fun `gated sink receives records and boundaries one at a time in call order`() {
        val gate = CountDownLatch(1)
        val arrivals = CopyOnWriteArrayList<String>()
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        val gatedSink = EventSink { event ->
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            if (event.eventType == START && arrivals.isEmpty()) gate.await(5, TimeUnit.SECONDS)
            arrivals += event.eventType
            inFlight.decrementAndGet()
        }
        val client = client(sink = gatedSink, sinkDispatcher = Dispatchers.IO)

        client.record("a")
        client.startSession()
        client.record("b")
        client.stopSession()
        client.record("c")
        gate.countDown()
        client.close()

        awaitSize(arrivals, 9)
        arrivals shouldBe listOf(START, "a", STOP, START, "b", STOP, START, "c", STOP)
        maxInFlight.get() shouldBe 1
    }

    @Test
    fun `close returns without waiting and the queue drains before the scope is cancelled`() {
        val gate = CountDownLatch(1)
        val arrivals = CopyOnWriteArrayList<String>()
        val job = SupervisorJob()
        val client = client(
            sink = EventSink { event ->
                gate.await(5, TimeUnit.SECONDS)
                arrivals += event.eventType
            },
            sinkDispatcher = Dispatchers.IO,
            scope = CoroutineScope(job + Dispatchers.Default)
        )
        client.record("in_flight")

        client.close()
        arrivals.shouldBeEmpty()
        job.isCancelled shouldBe false

        gate.countDown()
        awaitSize(arrivals, 3)
        arrivals shouldBe listOf(START, "in_flight", STOP)
        awaitCancelled(job)
    }

    private fun awaitSize(list: List<*>, size: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (list.size < size && System.currentTimeMillis() < deadline) Thread.sleep(5)
        // Give a stray late send a chance to show up.
        Thread.sleep(50)
    }

    private fun awaitCancelled(job: Job) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!job.isCancelled && System.currentTimeMillis() < deadline) Thread.sleep(5)
        job.isCancelled shouldBe true
    }

    private companion object {
        const val START = "_session.start"
        const val STOP = "_session.stop"
    }
}
