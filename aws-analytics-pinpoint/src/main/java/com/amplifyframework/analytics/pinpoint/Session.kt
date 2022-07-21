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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal class Session {

    private val maxSessionIdLength = 8
    private val sessionIdPaddingChar = "_"
    private val sessionIdDelimiter = "-"
    internal val sessionId: String
    internal val startTime: Long
    internal var stopTime: Long?

    internal val sessionDuration: Long
        get() {
            val time = stopTime ?: System.currentTimeMillis()
            var duration = 0L
            if (time > startTime) {
                duration = time - startTime
            }
            return duration
        }

    constructor(
        sessionId: String,
        startTime: Long,
        stopTime: Long?
    ) {
        this@Session.sessionId = sessionId
        this@Session.startTime = startTime
        this@Session.stopTime = stopTime
    }

    constructor(
        context: Context
    ) {
        this@Session.sessionId = generateSessionId()
        this@Session.startTime = System.currentTimeMillis()
        this@Session.stopTime = null
    }

    fun isPaused(): Boolean = stopTime != null

    fun pause() {
        if (!isPaused()) {
            stopTime = System.currentTimeMillis()
        }
    }

    private fun generateSessionId(): String {
        val sessionIdTimeFormat = SimpleDateFormat("yyyyMMdd-HHmmssSSS", Locale.US)
        sessionIdTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
        val uniqueId = "" // TODO(": Get uniqueId from shared preferences.")
        val time = sessionIdTimeFormat.format(startTime)
        return "${trimOrPad(uniqueId, maxSessionIdLength, sessionIdPaddingChar)}$sessionIdDelimiter$time"
    }

    private fun trimOrPad(
        input: String,
        outputLength: Int,
        paddingCharacter: String
    ): String {
        val stringBuffer = StringBuffer()
        if (input.length > outputLength - 1) {
            stringBuffer.append(input.substring(input.length - outputLength))
        } else {
            for (i in 0 until (maxSessionIdLength.minus(input.length))) {
                stringBuffer.append(paddingCharacter)
            }
            stringBuffer.append(input)
        }
        return stringBuffer.toString()
    }
}
