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
package com.amplifyframework.recordcache

import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AutoFlushScheduler(
    val interval: FlushStrategy.Interval,
    val client: RecordClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger: Logger = AmplifyLogging.logger<AutoFlushScheduler>()
    private val scope = CoroutineScope(dispatcher + CoroutineName("AutoFlushScheduler"))
    private var flushJob: Job? = null

    fun start() {
        flushJob?.cancel()
        flushJob = scope.launch {
            run()
        }
    }

    fun disable() {
        flushJob?.cancel()
        flushJob = null
    }

    private suspend fun run() {
        while (true) {
            delay(interval.interval)
            try {
                when (val result = client.flush()) {
                    is Result.Success -> logger.debug {
                        "Auto-flush completed: ${result.data.recordsFlushed} records flushed"
                    }
                    is Result.Failure -> logger.warn(result.error) { "Auto-flush failed" }
                }
            } catch (e: Exception) {
                // Defensive catch for unexpected exceptions to prevent scheduler from crashing
                logger.error(e) { "Unexpected error during auto-flush" }
            }
        }
    }
}
