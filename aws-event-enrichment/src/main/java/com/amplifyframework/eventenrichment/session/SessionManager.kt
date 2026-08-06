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

package com.amplifyframework.eventenrichment.session

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

private const val SESSION_ID_PREFIX_LENGTH = 8
private const val SESSION_ID_UNIQUE_LENGTH = 8

/**
 * Manages session lifecycle with [SessionState.ACTIVE], [SessionState.PAUSED],
 * and [SessionState.STOPPED] states.
 *
 * When the app backgrounds, the session enters [SessionState.PAUSED]. If the
 * app returns to the foreground within the configured [sessionTimeout], the
 * same session resumes. If the timeout expires, the session stops and a new one
 * starts on the next foreground.
 *
 * All session mutations acquire [lock] so that concurrent `record`/lifecycle
 * calls cannot observe or produce a torn session. The client shares the same
 * lock to serialize its own close/record sections against session mutations.
 *
 * The clock ([now]), id generator ([generateId]), and [scheduler] are injectable
 * so tests can drive session behavior deterministically.
 *
 * @param appId Application id used to build session ids.
 * @param sessionTimeout Time the app may stay backgrounded before the session stops.
 * @param lock Lock guarding all session mutations. Injected by the client so
 *   both sides serialize on the same lock.
 * @param now Clock supplying the current instant.
 * @param generateId Supplier of unique ids for session id generation.
 * @param scheduler Schedules the background pause timeout.
 */
internal class SessionManager(
    private val appId: String,
    private val sessionTimeout: Duration,
    private val lock: ReentrantLock = ReentrantLock(),
    private val now: () -> Instant = Clock.System::now,
    private val generateId: () -> String = { UUID.randomUUID().toString() },
    private val scheduler: TimeoutScheduler
) {
    /** Current session state. */
    var state: SessionState = SessionState.STOPPED
        private set

    /** Current session, or null if stopped and cleared. */
    var session: Session? = null
        private set

    private var sessionStart: Instant? = null
    private var pauseTimeout: TimeoutHandle? = null

    /**
     * Returns the current session, starting a fresh one if none is active. A
     * stopped session is still exposed for inspection, so a fresh one starts
     * instead of stamping the stopped session (which carries a stop_timestamp)
     * onto a new event.
     */
    fun ensureActiveSession(): Session = lock.withLock {
        if (session == null || state == SessionState.STOPPED) startSession() // startSession also takes lock (reentrant)
        session!! // safe: we hold the lock, startSession just set it
    }

    /** Starts a new session. If one is active, stops it first. */
    fun startSession() {
        lock.withLock {
            if (state != SessionState.STOPPED) {
                stopSession()
            }
            val start = now()
            sessionStart = start
            session = Session(
                id = generateSessionId(start),
                startTimestamp = start.toIsoUtc()
            )
            state = SessionState.ACTIVE
        }
    }

    /** Stops the current session, recording stop time and duration. */
    fun stopSession() {
        lock.withLock {
            cancelTimeout()
            val current = session ?: return
            val start = sessionStart
            val stop = now()
            session = current.copy(
                stopTimestamp = stop.toIsoUtc(),
                duration = if (start != null) (stop - start).inWholeMilliseconds else null
            )
            state = SessionState.STOPPED
        }
    }

    /**
     * Clears the current session without recording stop metadata.
     *
     * Unlike [stopSession], which records a stop timestamp and duration and
     * leaves the stopped session readable, this drops the session entirely and
     * returns the manager to the stopped state. Used when closing the client so
     * no stale session remains readable.
     */
    fun clearSession() {
        lock.withLock {
            cancelTimeout()
            session = null
            sessionStart = null
            state = SessionState.STOPPED
        }
    }

    /** Called when the app moves to the background. */
    fun handleAppPaused() {
        lock.withLock {
            if (state != SessionState.ACTIVE) return
            state = SessionState.PAUSED
            pauseTimeout = scheduler.schedule(sessionTimeout) { onTimeoutExpired() }
        }
    }

    /** Called when the app returns to the foreground. */
    fun handleAppResumed() {
        lock.withLock {
            when (state) {
                SessionState.PAUSED -> {
                    cancelTimeout()
                    state = SessionState.ACTIVE
                }
                SessionState.STOPPED -> startSession()
                SessionState.ACTIVE -> Unit
            }
        }
    }

    private fun onTimeoutExpired() {
        stopSession()
    }

    private fun cancelTimeout() {
        pauseTimeout?.cancel()
        pauseTimeout = null
    }

    private fun generateSessionId(timestamp: Instant): String {
        val prefix = if (appId.length > SESSION_ID_PREFIX_LENGTH) {
            appId.substring(0, SESSION_ID_PREFIX_LENGTH)
        } else {
            appId.padStart(SESSION_ID_PREFIX_LENGTH, '_')
        }
        val uniqueId = generateId().take(SESSION_ID_UNIQUE_LENGTH)
        val date = SESSION_ID_DATE_FORMAT.format(timestamp.toLocalDateTime(TimeZone.UTC))
        return "$prefix-$uniqueId-$date"
    }

    private fun Instant.toIsoUtc(): String = ISO_UTC_FORMAT.format(toLocalDateTime(TimeZone.UTC))

    private companion object {
        // Millisecond-precision ISO-8601 UTC, matching the reference envelope.
        val ISO_UTC_FORMAT = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char('T')
            hour()
            char(':')
            minute()
            char(':')
            second()
            char('.')
            secondFraction(3)
            char('Z')
        }

        // Session id date component: YYYYMMdd-HHmmssSSS in UTC.
        val SESSION_ID_DATE_FORMAT = LocalDateTime.Format {
            year()
            monthNumber()
            day()
            char('-')
            hour()
            minute()
            second()
            secondFraction(3)
        }
    }
}
