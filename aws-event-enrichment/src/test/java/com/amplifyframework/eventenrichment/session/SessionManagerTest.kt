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

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class SessionManagerTest {

    /** Captures the scheduled timeout action so tests can fire it on demand. */
    private class FakeScheduler : TimeoutScheduler {
        var scheduledAction: (() -> Unit)? = null
        var cancelled = false

        override fun schedule(delay: Duration, action: () -> Unit): TimeoutHandle {
            scheduledAction = action
            cancelled = false
            return TimeoutHandle { cancelled = true }
        }

        fun fireTimeout() {
            scheduledAction?.invoke()
        }
    }

    private var clock = Instant.parse("2026-07-06T19:51:59.839Z")
    private val fakeScheduler = FakeScheduler()
    private var idSequence = 0

    private fun manager(appId: String = "my-app-id", sessionTimeout: Duration = 5.seconds) = SessionManager(
        appId = appId,
        sessionTimeout = sessionTimeout,
        now = { clock },
        generateId = { "abcdef${idSequence++}0-1111-2222-3333-444455556666" },
        scheduler = fakeScheduler
    )

    @Test
    fun `starts stopped with no session`() {
        val manager = manager()
        manager.state shouldBe SessionState.STOPPED
        manager.session shouldBe null
    }

    @Test
    fun `startSession activates a session with an ISO-8601 UTC start timestamp`() {
        val manager = manager()
        manager.startSession()

        manager.state shouldBe SessionState.ACTIVE
        val session = manager.session.shouldNotBeNull()
        session.startTimestamp shouldBe "2026-07-06T19:51:59.839Z"
        session.stopTimestamp shouldBe null
        session.duration shouldBe null
    }

    @Test
    fun `session id follows appId-uuid-date format`() {
        val manager = manager(appId = "my-app-id")
        manager.startSession()

        // {appId first 8}-{uuid first 8}-{yyyyMMdd-HHmmssSSS UTC}
        manager.session.shouldNotBeNull().id shouldMatch Regex("^my-app-i-[a-f0-9]{8}-20260706-195159839$")
    }

    @Test
    fun `short appId is left-padded with underscores to 8 chars`() {
        val manager = manager(appId = "abc")
        manager.startSession()
        manager.session.shouldNotBeNull().id shouldMatch Regex("^_____abc-[a-f0-9]{8}-.*$")
    }

    @Test
    fun `stopSession records stop timestamp and duration`() {
        val manager = manager()
        manager.startSession()
        clock = clock.plusMillis(10_000)
        manager.stopSession()

        manager.state shouldBe SessionState.STOPPED
        val session = manager.session.shouldNotBeNull()
        session.stopTimestamp shouldBe "2026-07-06T19:52:09.839Z"
        session.duration shouldBe 10_000L
    }

    @Test
    fun `clearSession drops the session entirely`() {
        val manager = manager()
        manager.startSession()
        manager.clearSession()
        manager.state shouldBe SessionState.STOPPED
        manager.session shouldBe null
    }

    @Test
    fun `handleAppPaused moves an active session to paused and schedules the timeout`() {
        val manager = manager()
        manager.startSession()
        manager.handleAppPaused()

        manager.state shouldBe SessionState.PAUSED
        fakeScheduler.scheduledAction.shouldNotBeNull()
    }

    @Test
    fun `handleAppResumed within timeout resumes the same session`() {
        val manager = manager()
        manager.startSession()
        val originalId = manager.session.shouldNotBeNull().id

        manager.handleAppPaused()
        manager.handleAppResumed()

        manager.state shouldBe SessionState.ACTIVE
        manager.session.shouldNotBeNull().id shouldBe originalId
        fakeScheduler.cancelled shouldBe true
    }

    @Test
    fun `timeout expiry while paused stops the session`() {
        val manager = manager()
        manager.startSession()
        manager.handleAppPaused()
        fakeScheduler.fireTimeout()

        manager.state shouldBe SessionState.STOPPED
    }

    @Test
    fun `handleAppResumed after timeout starts a fresh session`() {
        val manager = manager()
        manager.startSession()
        val firstId = manager.session.shouldNotBeNull().id

        manager.handleAppPaused()
        fakeScheduler.fireTimeout()
        manager.handleAppResumed()

        manager.state shouldBe SessionState.ACTIVE
        manager.session.shouldNotBeNull().id shouldNotBe firstId
    }

    @Test
    fun `startSession while active stops the previous session first`() {
        val manager = manager()
        manager.startSession()
        val firstId = manager.session.shouldNotBeNull().id
        clock = clock.plusMillis(1_000)
        manager.startSession()

        val session = manager.session.shouldNotBeNull()
        session.id shouldNotBe firstId
        // The new session is active and carries no stop metadata.
        session.stopTimestamp shouldBe null
        session.duration shouldBe null
    }
}
