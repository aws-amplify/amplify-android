/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.analytics.pinpoint

import android.content.Context

internal class SessionClient(
    private val context: Context,
    var analyticsClient: AnalyticsClient?
    // TODO: Pass shared-preferences
) {

    var session: Session? = null
    private val sessionStopEvent = "_session.stop"
    private val sessionStartEvent = "_session.start"
    private val sessionPauseEvent = "_session.pause"
    private val sessionResumeEvent = "_session.resume"

    init {
        // TODO: Initialize session from shared prefs
    }

    @Synchronized
    fun startSession() {
        executeStart()
    }

    @Synchronized
    fun stopSession() {
        executeStop()
    }

    internal fun setAnalyticsClient(analyticsClient: AnalyticsClient) {
        this@SessionClient.analyticsClient = analyticsClient
    }

    private fun executeStop() {
        session?.let { session ->
            if (!session.isPaused()) {
                session.pause()
            }
            val stopTime = session.stopTime ?: 0L
            analyticsClient?.let {
                val pinpointEvent = it.createEvent(
                    sessionStopEvent,
                    session.sessionId,
                    session.startTime,
                    session.sessionDuration
                )
                it.recordEvent(pinpointEvent)
            }
            // TODO: Clear event srouce attributes:this.pinpointContext.getAnalyticsClient().clearEventSourceAttributes();
        }
        session = null
    }

    private fun executeStart() {
        // TODO: Update endpoint profile:getTargetingClient().updateEndpointProfile();
        val newSession = Session(context)
        session = newSession
        analyticsClient?.let {
            val pinpointEvent = it.createEvent(
                sessionStartEvent,
                newSession.sessionId,
                newSession.startTime,
                newSession.sessionDuration
            )
            it.recordEvent(pinpointEvent)
        }
    }

    internal fun getSessionState(): SessionState {
        session?.let { session ->
            return takeIf { session.isPaused() }?.let {
                SessionState.PAUSED
            } ?: SessionState.ACTIVE
        }
        return SessionState.INACTIVE
    }

    internal enum class SessionState {
        INACTIVE, ACTIVE, PAUSED
    }
}
