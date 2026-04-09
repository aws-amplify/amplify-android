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

package com.amplifyframework.testutils

import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.core.Amplify
import com.amplifyframework.testutils.rules.RepeatKnownFailuresRule
import org.junit.BeforeClass
import org.junit.Rule

/**
 * A base class for all tests that run on device farm
 */
abstract class DeviceFarmTestBase {
    @get:Rule
    val testRule = RepeatKnownFailuresRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupDebugLogging() {
            try {
                Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.DEBUG))
            } catch (_: Exception) {
                // Already configured or plugin already added — safe to ignore
            }
        }
    }
}
