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
package com.amplifyframework.storage.s3.transfer

import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StallDetectingProgressListenerTest {

    private class RecordingListener : ProgressListener {
        val totalBytes = AtomicLong(0L)
        val callCount = AtomicInteger(0)
        override fun progressChanged(bytesTransferred: Long) {
            totalBytes.addAndGet(bytesTransferred)
            callCount.incrementAndGet()
        }
    }

    /**
     * Test that a timeout of 0 suppresses stall detection.
     *
     * - Given: a listener constructed with `stallTimeoutSeconds = 0`
     * - When: time passes beyond any plausible timeout without progress events
     * - Then: `onStall` is never invoked
     */
    @Test
    fun `stall timeout of 0 never fires onStall`() = runTest {
        val stallCount = AtomicInteger(0)
        val listener = StallDetectingProgressListener(
            delegate = RecordingListener(),
            stallTimeoutSeconds = 0L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        advanceTimeBy(10.seconds)
        runCurrent()

        stallCount.get() shouldBe 0
    }

    /**
     * Test that stall fires once after the configured interval with no progress.
     *
     * - Given: a listener started with `stallTimeoutSeconds = 5`
     * - When: 5 seconds of virtual time pass with no progress events
     * - Then: `onStall` is invoked exactly once
     */
    @Test
    fun `fires onStall exactly once after timeout when no progress`() = runTest {
        val stallCount = AtomicInteger(0)
        val delegate = RecordingListener()
        val listener = StallDetectingProgressListener(
            delegate = delegate,
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        advanceTimeBy(5.seconds)
        runCurrent()

        stallCount.get() shouldBe 1
        delegate.callCount.get() shouldBe 0
    }

    /**
     * Test that progress events reset the stall timer.
     *
     * - Given: a listener with a 5 second timeout
     * - When: a progress event fires every 3 seconds
     * - Then: `onStall` is never invoked and each event is forwarded to the delegate
     */
    @Test
    fun `progress before timeout resets timer and prevents stall`() = runTest {
        val stallCount = AtomicInteger(0)
        val delegate = RecordingListener()
        val listener = StallDetectingProgressListener(
            delegate = delegate,
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        repeat(4) {
            advanceTimeBy(3.seconds)
            listener.progressChanged(1024L)
            runCurrent()
        }

        stallCount.get() shouldBe 0
        delegate.callCount.get() shouldBe 4
        delegate.totalBytes.get() shouldBe 4096L
    }

    /**
     * Test that a zero-byte progress event does not reset the stall timer.
     *
     * `progressChanged(0)` is used in multipart flows to signal a part reset without transferring
     * bytes; it should not count as "forward progress" for stall detection.
     *
     * - Given: a listener with a 5 second timeout
     * - When: zero-byte progress events are dispatched for the full timeout window
     * - Then: `onStall` still fires exactly once and the delegate sees every event
     */
    @Test
    fun `zero byte progress events do not reset stall timer`() = runTest {
        val stallCount = AtomicInteger(0)
        val delegate = RecordingListener()
        val listener = StallDetectingProgressListener(
            delegate = delegate,
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        repeat(4) {
            advanceTimeBy(1.seconds)
            listener.progressChanged(0L)
        }
        advanceTimeBy(2.seconds)
        runCurrent()

        stallCount.get() shouldBe 1
        delegate.callCount.get() shouldBe 4
        delegate.totalBytes.get() shouldBe 0L
    }

    /**
     * Test that closing the listener prevents a pending stall fire.
     *
     * - Given: a listener that has armed its stall timer
     * - When: `close()` is called before the timeout elapses
     * - Then: `onStall` is never invoked even if virtual time advances past the timeout
     */
    @Test
    fun `close before timeout prevents stall`() = runTest {
        val stallCount = AtomicInteger(0)
        val listener = StallDetectingProgressListener(
            delegate = RecordingListener(),
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        advanceTimeBy(3.seconds)
        listener.close()
        advanceTimeBy(10.seconds)
        runCurrent()

        stallCount.get() shouldBe 0
    }

    /**
     * Test that close is idempotent and subsequent progress events still reach the delegate.
     *
     * - Given: a listener that has been closed
     * - When: `close()` is called again and a progress event is dispatched
     * - Then: neither call throws; `onStall` never fires; and the delegate continues to receive
     *   progress events (progress forwarding is independent of stall detection)
     */
    @Test
    fun `close is idempotent and delegate still receives progress after close`() = runTest {
        val stallCount = AtomicInteger(0)
        val delegate = RecordingListener()
        val listener = StallDetectingProgressListener(
            delegate = delegate,
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        listener.close()
        listener.close()
        listener.progressChanged(2048L)

        stallCount.get() shouldBe 0
        delegate.callCount.get() shouldBe 1
        delegate.totalBytes.get() shouldBe 2048L
    }

    /**
     * Test that a rapid burst of progress events does not accumulate concurrent timers.
     *
     * This is a regression guard: before rearming, the listener must cancel the previous job.
     * If it did not, many timers would be in flight and the first one to fire would trigger
     * [onStall] even though progress was still actively arriving.
     *
     * - Given: a listener with a 5 second timeout
     * - When: a flurry of progress events is dispatched and then progress stops for one interval
     * - Then: exactly one stall fires, not N
     */
    @Test
    fun `rapid progress events do not accumulate timers`() = runTest {
        val stallCount = AtomicInteger(0)
        val listener = StallDetectingProgressListener(
            delegate = RecordingListener(),
            stallTimeoutSeconds = 5L,
            onStall = { stallCount.incrementAndGet() },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
            ownsScope = false
        )

        listener.start()
        repeat(20) {
            listener.progressChanged(512L)
        }
        advanceTimeBy(5.seconds)
        runCurrent()

        stallCount.get() shouldBe 1
    }
}
