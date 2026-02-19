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
import com.amplifyframework.foundation.logging.LogSink
import com.amplifyframework.foundation.logging.allows

/**
 * A sink to use with AmplifyLogging that sends log messages to logcat.
 * @param threshold The minimum level for emitted logs. Logs below this level will be ignored.
 */
class AndroidLogSink(
    private val threshold: LogLevel = LogLevel.Info
) : LogSink {
    override fun isEnabledFor(level: LogLevel) = threshold allows level

    override fun emit(message: LogMessage) {
        if (threshold allows message.level) {
            when (message.level) {
                LogLevel.Verbose -> Log.v(message.name, message.content, message.cause)
                LogLevel.Debug -> Log.d(message.name, message.content, message.cause)
                LogLevel.Info -> Log.i(message.name, message.content, message.cause)
                LogLevel.Warn -> Log.w(message.name, message.content, message.cause)
                LogLevel.Error -> Log.e(message.name, message.content, message.cause)
                LogLevel.None -> Unit // No-Op
            }
        }
    }
}
