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

package com.amplifyframework.pinpoint.core

import android.content.Context
import com.amplifyframework.annotations.InternalAmplifyApi

@InternalAmplifyApi
class SessionClient(
    private val context: Context,
    val targetingClient: TargetingClient,
    private val uniqueId: String,
    var analyticsClient: AnalyticsClient? = null,
) {

    var session: Session? = null
    private val sessionStopEvent = "_session.stop"
    private val sessionStartEvent = "_session.start"

    @Synchronized
    fun startSession() {
        executeStart()
    }

    @Synchronized
    fun stopSession() {
        executeStop()
    }

    internal fun setAnalyticsClient(analyticsClient: AnalyticsClient) {
        this.analyticsClient = analyticsClient
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
                    stopTime,
                    session.sessionDuration
                )
                it.recordEvent(pinpointEvent)
            }
        }
        session = null
    }

    private fun executeStart() {
        targetingClient.updateEndpointProfile()
        val newSession = Session(context, uniqueId)
        session = newSession
        analyticsClient?.let {
            val pinpointEvent = it.createEvent(
                sessionStartEvent,
                newSession.sessionId,
                newSession.startTime,
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
