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

package com.amplifyframework.testutils.coroutines

import io.kotest.assertions.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Runs a blocking coroutine with a timeout.
 */
fun <T> runBlockingWithTimeout(timeout: Duration = 10.seconds, block: suspend () -> T): T = runBlocking {
    withTimeout(timeout) {
        block()
    }
}

fun <T> runBlockingWithTimeout(timeout: Duration = 10.seconds, message: String, block: suspend () -> T): T =
    runBlocking {
        try {
            withTimeout(timeout) { block() }
        } catch (_: TimeoutCancellationException) {
            fail(message)
        }
    }
