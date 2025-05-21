/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.sdk.appsync.events.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ConnectionTimeoutTimer(
    private val scope: CoroutineScope,
    val onTimeout: () -> Unit
) {

    private var timeoutInMillis: Long = 300_000L
    private var timeoutJob: Job? = null
    private val lock = Object()

    fun resetTimeoutTimer(timeoutInMillis: Long = this.timeoutInMillis) {
        synchronized(lock) {
            if (this.timeoutInMillis != timeoutInMillis) {
                this.timeoutInMillis = timeoutInMillis
            }

            timeoutJob?.cancel() // Cancel existing timer if any
            timeoutJob = scope.launch {
                delay(timeoutInMillis)
                // If this code executes, it means no events were received for the duration of timeoutInMillis
                onTimeout()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            timeoutJob?.cancel()
            timeoutJob = null
        }
    }
}
