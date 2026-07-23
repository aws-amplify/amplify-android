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
package com.amplifyframework.eventenrichment.session

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Handle for a scheduled timeout that can be cancelled before it fires. */
fun interface TimeoutHandle {
    /** Cancels the pending timeout. No-op if it has already fired. */
    fun cancel()
}

/**
 * Schedules a one-shot action to run after a delay.
 *
 * Injected into [SessionManager] so tests can drive session timeouts
 * deterministically instead of waiting on wall-clock time.
 */
fun interface TimeoutScheduler {
    /**
     * Schedules [action] to run once after [delay], returning a handle that can
     * cancel it before it fires.
     */
    fun schedule(delay: Duration, action: () -> Unit): TimeoutHandle
}

/**
 * Default [TimeoutScheduler] backed by a coroutine [delay] on the provided
 * [scope]. The scope defaults to a supervised scope on [Dispatchers.Default].
 */
class CoroutineTimeoutScheduler(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TimeoutScheduler {
    override fun schedule(delay: Duration, action: () -> Unit): TimeoutHandle {
        val job = scope.launch {
            delay(delay)
            if (isActive) action()
        }
        return TimeoutHandle { job.cancel() }
    }
}
