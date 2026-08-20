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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Decorates a [ProgressListener] with stall detection.
 *
 * When the wrapped listener has not observed any forward progress within [stallTimeoutSeconds],
 * [onStall] is invoked exactly once. Callers are expected to cancel the associated transfer and
 * surface a `ProgressStallTimeoutException` to the user in response to [onStall].
 *
 * Timer lifecycle:
 *  - Call [start] once the transfer has been enqueued to arm the initial timer.
 *  - Every call to [progressChanged] with `bytesTransferred > 0` re-arms the timer.
 *  - Call [close] when the transfer reaches a terminal state (completed, cancelled, failed) to
 *    cancel any pending timer and suppress further stall detection.
 *
 * A [stallTimeoutSeconds] of `0` disables detection — [onStall] will never be invoked, but
 * progress events are still forwarded to [delegate]. Callers typically avoid constructing this
 * decorator at all when detection is disabled, but the guard is kept here to make the decorator
 * safe to construct unconditionally.
 */
internal class StallDetectingProgressListener(
    private val delegate: ProgressListener,
    private val stallTimeoutSeconds: Long,
    private val onStall: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val ownsScope: Boolean = true
) : ProgressListener, AutoCloseable {

    private val timerJob = AtomicReference<Job?>(null)
    private val stalled = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    /**
     * Arms the stall timer. Intended to be called once when the transfer begins so that uploads
     * that never produce a single progress event still time out.
     */
    fun start() {
        if (!shouldMonitor()) return
        armTimer()
    }

    @Synchronized
    override fun progressChanged(bytesTransferred: Long) {
        delegate.progressChanged(bytesTransferred)
        if (!shouldMonitor()) return
        if (bytesTransferred > 0L) {
            armTimer()
        }
    }

    /**
     * Cancels the pending stall timer and prevents future stall detection. Safe to call multiple
     * times. When the decorator owns its [scope] (the default), the scope is also cancelled.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        timerJob.getAndSet(null)?.cancel()
        if (ownsScope) {
            scope.cancel()
        }
    }

    private fun shouldMonitor(): Boolean = !closed.get() && !stalled.get() && stallTimeoutSeconds > 0L

    private fun armTimer() {
        val next = scope.launch {
            delay(stallTimeoutSeconds.seconds)
            if (stalled.compareAndSet(false, true)) {
                onStall()
            }
        }
        val previous = timerJob.getAndSet(next)
        previous?.cancel()
    }
}
