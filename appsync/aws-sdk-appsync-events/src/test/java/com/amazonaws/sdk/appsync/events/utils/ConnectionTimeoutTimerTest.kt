/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amazonaws.sdk.appsync.events.utils

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionTimeoutTimerTest {
    private var timeoutCallCount = 0
    private lateinit var timer: ConnectionTimeoutTimer

    @Before
    fun setup() = runTest {
        timeoutCallCount = 0
    }

    @Test
    fun `test timeout triggers after specified duration`() = runTest {
        // Given
        timer = ConnectionTimeoutTimer(this) { timeoutCallCount++ }
        val testTimeout = 1000L

        // When
        timer.resetTimeoutTimer(testTimeout)
        advanceTimeBy(testTimeout + 100) // Add small buffer

        // Then
        timeoutCallCount shouldBe 1
    }

    @Test
    fun `test reset cancels previous timer and starts new one`() = runTest {
        // Given
        timer = ConnectionTimeoutTimer(this) { timeoutCallCount++ }
        val initialTimeout = 1000L
        val newTimeout = 2000L

        // When
        timer.resetTimeoutTimer(initialTimeout)
        advanceTimeBy(500L) // Advance halfway through first timer
        timer.resetTimeoutTimer(newTimeout) // Reset with new timeout
        advanceTimeBy(initialTimeout) // This shouldn't trigger timeout

        // Then
        timeoutCallCount shouldBe 0

        // When
        advanceTimeBy(newTimeout + 100) // This should trigger timeout

        // Then
        timeoutCallCount shouldBe 1
    }

    @Test
    fun `test stop cancels timer`() = runTest {
        // Given
        timer = ConnectionTimeoutTimer(this) { timeoutCallCount++ }
        val testTimeout = 1000L

        // When
        timer.resetTimeoutTimer(testTimeout)
        timer.stop()
        advanceTimeBy(testTimeout + 100)

        // Then
        timeoutCallCount shouldBe 0
    }

    @Test
    fun `test multiple resets with same timeout`() = runTest {
        // Given
        timer = ConnectionTimeoutTimer(this) { timeoutCallCount++ }
        val testTimeout = 1000L

        // When
        timer.resetTimeoutTimer(testTimeout)
        advanceTimeBy(600L)
        timer.resetTimeoutTimer() // Reset with same timeout
        advanceTimeBy(600L) // Still shouldn't timeout

        // Then
        timeoutCallCount shouldBe 0

        // When
        advanceTimeBy(600L) // Now should timeout

        // Then
        timeoutCallCount shouldBe 1
    }
}
