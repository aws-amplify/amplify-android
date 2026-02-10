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

package com.amplifyframework.foundation

import android.util.Log
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.LogMessage
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.junit.Test

class AndroidLogSinkTest {
    @BeforeTest
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any(), any()) } returns 1
        every { Log.d(any(), any(), any()) } returns 1
        every { Log.i(any(), any(), any()) } returns 1
        every { Log.w(any(), any(), any()) } returns 1
        every { Log.e(any(), any(), any()) } returns 1
    }

    @AfterTest
    fun teardown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `verbose logs sent to logcat`() {
        val sink = AndroidLogSink(LogLevel.Verbose)

        sink.emit(
            LogMessage(
                level = LogLevel.Verbose,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.v("testLogger", "test message", null)
        }
    }

    @Test
    fun `debug logs sent to logcat`() {
        val sink = AndroidLogSink(LogLevel.Verbose)

        sink.emit(
            LogMessage(
                level = LogLevel.Debug,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.d("testLogger", "test message", null)
        }
    }

    @Test
    fun `info logs sent to logcat`() {
        val sink = AndroidLogSink()

        sink.emit(
            LogMessage(
                level = LogLevel.Info,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.i("testLogger", "test message", null)
        }
    }

    @Test
    fun `warn logs sent to logcat`() {
        val sink = AndroidLogSink()

        sink.emit(
            LogMessage(
                level = LogLevel.Warn,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.w("testLogger", "test message", null)
        }
    }

    @Test
    fun `error logs sent to logcat`() {
        val sink = AndroidLogSink()

        sink.emit(
            LogMessage(
                level = LogLevel.Error,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.e("testLogger", "test message", null)
        }
    }

    @Test
    fun `multiple logs are emitted`() {
        val sink = AndroidLogSink()

        sink.emit(
            LogMessage(
                level = LogLevel.Info,
                name = "infoLogger",
                content = "info1",
                cause = null
            )
        )
        sink.emit(
            LogMessage(
                level = LogLevel.Info,
                name = "infoLogger2",
                content = "info2",
                cause = null
            )
        )
        sink.emit(
            LogMessage(
                level = LogLevel.Error,
                name = "errorLogger",
                content = "error",
                cause = null
            )
        )

        verifySequence {
            Log.i("infoLogger", "info1", null)
            Log.i("infoLogger2", "info2", null)
            Log.e("errorLogger", "error", null)
        }
    }

    @Test
    fun `logs not emitted if below level`() {
        val sink = AndroidLogSink(threshold = LogLevel.Warn)

        sink.emit(
            LogMessage(
                level = LogLevel.Error,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )
        sink.emit(
            LogMessage(
                level = LogLevel.Info,
                name = "testLogger",
                content = "test message",
                cause = null
            )
        )

        verify {
            Log.e("testLogger", "test message", null)
        }
        verify(exactly = 0) {
            Log.i(any(), any(), any())
        }
    }

    @Test
    fun `throwables are passed to logcat`() {
        val sink = AndroidLogSink()
        val error = RuntimeException("error")

        sink.emit(
            LogMessage(
                level = LogLevel.Info,
                name = "testLogger",
                content = "test message",
                cause = error
            )
        )

        verify {
            Log.i("testLogger", "test message", error)
        }
    }

    @Test
    fun `sink respects supplied threshold`() {
        val sink = AndroidLogSink(threshold = LogLevel.Warn)

        sink.isEnabledFor(LogLevel.Verbose).shouldBeFalse()
        sink.isEnabledFor(LogLevel.Debug).shouldBeFalse()
        sink.isEnabledFor(LogLevel.Info).shouldBeFalse()
        sink.isEnabledFor(LogLevel.Warn).shouldBeTrue()
        sink.isEnabledFor(LogLevel.Error).shouldBeTrue()
    }
}
