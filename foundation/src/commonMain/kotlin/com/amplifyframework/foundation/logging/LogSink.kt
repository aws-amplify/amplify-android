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

package com.amplifyframework.foundation.logging

/**
 * A log sink sends log messages to a specific destination, for example by outputting to a system-appropriate logging
 * framework.
 */
interface LogSink {
    /**
     * Returns true if this sink will emit logs at the given level
     * @param level The LogLevel to check
     * @return True if a log at this level would be emitted, false otherwise
     */
    fun isEnabledFor(level: LogLevel): Boolean

    /**
     * Emit the given LogMessage
     * @param message The log message
     */
    fun emit(message: LogMessage)
}
