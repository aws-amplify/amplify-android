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

package com.amplifyframework.storage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ProgressStallTimeoutTest {

    /**
     * `ProgressStallTimeout.Disabled` represents the legacy "no stall detection" mode and must
     * report a `secondsForStallTimer` of `0` so downstream code skips arming the timer.
     *
     * - Given: the singleton [ProgressStallTimeout.Disabled]
     * - When: `secondsForStallTimer` is read
     * - Then: the value is `0`
     */
    @Test
    fun `Disabled secondsForStallTimer is zero`() {
        ProgressStallTimeout.Disabled.secondsForStallTimer shouldBe 0L
    }

    /**
     * A positive `Interval` must surface its configured seconds so the worker layer arms a timer
     * with that exact duration.
     *
     * - Given: an [ProgressStallTimeout.Interval] of 30 seconds
     * - When: `secondsForStallTimer` is read
     * - Then: the value is `30`
     */
    @Test
    fun `Interval with positive seconds returns same value`() {
        val timeout = ProgressStallTimeout.Interval(seconds = 30L)
        timeout.secondsForStallTimer shouldBe 30L
        timeout.seconds shouldBe 30L
    }

    /**
     * An `Interval` of zero must be normalized to `0` so it behaves as if stall detection were
     * disabled. This guards callers that pass `0` as a "no override" sentinel.
     *
     * - Given: an [ProgressStallTimeout.Interval] of 0 seconds
     * - When: `secondsForStallTimer` is read
     * - Then: the value is `0`
     */
    @Test
    fun `Interval with zero seconds disables the stall timer`() {
        val timeout = ProgressStallTimeout.Interval(seconds = 0L)
        timeout.secondsForStallTimer shouldBe 0L
    }

    /**
     * A negative `Interval` is treated the same as `Disabled` so a misconfigured value never
     * cancels uploads with a near-immediate timer. The original `seconds` is still preserved on
     * the value object for diagnostics.
     *
     * - Given: an [ProgressStallTimeout.Interval] of -5 seconds
     * - When: `secondsForStallTimer` is read
     * - Then: the value is `0` even though `seconds` itself is negative
     */
    @Test
    fun `Interval with negative seconds disables the stall timer but preserves raw seconds`() {
        val timeout = ProgressStallTimeout.Interval(seconds = -5L)
        timeout.secondsForStallTimer shouldBe 0L
        timeout.seconds shouldBe -5L
    }

    /**
     * The Java-friendly `disabled()` factory must return the singleton [ProgressStallTimeout.Disabled]
     * so Java callers and Kotlin callers compare as identical references.
     *
     * - Given: a call to [ProgressStallTimeout.disabled]
     * - When: the returned value is compared to [ProgressStallTimeout.Disabled]
     * - Then: it is the same singleton instance
     */
    @Test
    fun `disabled factory returns the Disabled singleton`() {
        val factoryValue = ProgressStallTimeout.disabled()
        factoryValue shouldBeSameInstanceAs ProgressStallTimeout.Disabled
        factoryValue.shouldBeInstanceOf<ProgressStallTimeout.Disabled>()
    }

    /**
     * The Java-friendly `interval(seconds)` factory must produce an [ProgressStallTimeout.Interval]
     * with the supplied value so Java callers do not need to import the Kotlin data class
     * constructor directly.
     *
     * - Given: a call to [ProgressStallTimeout.interval] with 45 seconds
     * - When: the returned value is downcast
     * - Then: it is an [ProgressStallTimeout.Interval] of 45 seconds
     */
    @Test
    fun `interval factory wraps seconds in Interval`() {
        val factoryValue = ProgressStallTimeout.interval(45L)
        factoryValue.shouldBeInstanceOf<ProgressStallTimeout.Interval>()
        factoryValue.seconds shouldBe 45L
        factoryValue.secondsForStallTimer shouldBe 45L
    }

    /**
     * `Interval` is a data class, so two intervals with the same `seconds` must compare as equal
     * and share a hash code. This matters when diffing equivalent options objects.
     *
     * - Given: two [ProgressStallTimeout.Interval] values with identical `seconds`
     * - When: `equals`/`hashCode` are evaluated
     * - Then: the values are equal and have the same hash
     */
    @Test
    fun `Interval data class equality and hashCode use seconds`() {
        val a = ProgressStallTimeout.Interval(seconds = 12L)
        val b = ProgressStallTimeout.Interval(seconds = 12L)
        val c = ProgressStallTimeout.Interval(seconds = 13L)

        (a == b) shouldBe true
        a.hashCode() shouldBe b.hashCode()
        (a == c) shouldBe false
    }

    /**
     * `Disabled` is a distinct subtype from `Interval` and the two must never compare as equal,
     * even when `secondsForStallTimer` happens to coincide (both are `0` for `Interval(0)`).
     * This prevents callers that branch on the type from being misled by a numerically equal
     * value.
     *
     * - Given: [ProgressStallTimeout.Disabled] and [ProgressStallTimeout.Interval] of 0 seconds
     * - When: they are compared with `==`
     * - Then: they are not equal even though their `secondsForStallTimer` matches
     */
    @Test
    fun `Disabled is distinct from Interval of zero seconds`() {
        val disabled: ProgressStallTimeout = ProgressStallTimeout.Disabled
        val zeroInterval: ProgressStallTimeout = ProgressStallTimeout.Interval(seconds = 0L)

        disabled.secondsForStallTimer shouldBe zeroInterval.secondsForStallTimer
        disabled shouldNotBe zeroInterval
    }
}
