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
package com.amplifyframework.cloudwatch

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.logging.LogLevel
import io.kotest.matchers.shouldBe
import org.junit.Test

@OptIn(ExperimentalAmplifyApi::class)
class CloudWatchLoggingFilterTest {

    private fun filter(constraints: LoggingConstraints) = CloudWatchLoggingFilter(constraints)

    @Test
    fun `default level allows messages at or above the threshold`() {
        val filter = filter(LoggingConstraints(defaultLogLevel = LogLevel.Warn))

        filter.canLog("Any", LogLevel.Error, null) shouldBe true
        filter.canLog("Any", LogLevel.Warn, null) shouldBe true
        filter.canLog("Any", LogLevel.Info, null) shouldBe false
    }

    @Test
    fun `default level of None blocks everything`() {
        val filter = filter(LoggingConstraints(defaultLogLevel = LogLevel.None))

        filter.canLog("Any", LogLevel.Error, null) shouldBe false
    }

    @Test
    fun `a None message level is never logged`() {
        val filter = filter(LoggingConstraints(defaultLogLevel = LogLevel.Verbose))

        filter.canLog("Any", LogLevel.None, null) shouldBe false
    }

    @Test
    fun `namespace override takes precedence over default`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Error,
                namespaceLogLevel = mapOf("Storage" to LogLevel.Debug)
            )
        )

        filter.canLog("Storage", LogLevel.Debug, null) shouldBe true
        filter.canLog("Storage", LogLevel.Verbose, null) shouldBe false
        filter.canLog("Other", LogLevel.Debug, null) shouldBe false
        filter.canLog("Other", LogLevel.Error, null) shouldBe true
    }

    @Test
    fun `namespace keys match case-insensitively`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Error,
                namespaceLogLevel = mapOf("Storage" to LogLevel.Debug)
            )
        )

        filter.canLog("storage", LogLevel.Debug, null) shouldBe true
        filter.canLog("STORAGE", LogLevel.Info, null) shouldBe true
    }

    @Test
    fun `user override takes precedence over namespace and default`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Error,
                namespaceLogLevel = mapOf("Storage" to LogLevel.Error),
                userLogLevel = mapOf("user1" to UserLogLevel(defaultLogLevel = LogLevel.Debug))
            )
        )

        // The user override loosens the threshold for user1...
        filter.canLog("Storage", LogLevel.Debug, "user1") shouldBe true
        // ...but the namespace level still applies when there is no user.
        filter.canLog("Storage", LogLevel.Debug, null) shouldBe false
    }

    @Test
    fun `user namespace override applies for that user`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Error,
                userLogLevel = mapOf(
                    "user1" to UserLogLevel(
                        defaultLogLevel = LogLevel.Error,
                        namespaceLogLevel = mapOf("Storage" to LogLevel.Verbose)
                    )
                )
            )
        )

        filter.canLog("Storage", LogLevel.Verbose, "user1") shouldBe true
        filter.canLog("Other", LogLevel.Verbose, "user1") shouldBe false
    }

    @Test
    fun `user keys match case-sensitively`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Error,
                userLogLevel = mapOf("User1" to UserLogLevel(defaultLogLevel = LogLevel.Verbose))
            )
        )

        // "user1" does not match the "User1" key, so it falls back to the default Error threshold.
        filter.canLog("Any", LogLevel.Info, "user1") shouldBe false
        filter.canLog("Any", LogLevel.Info, "User1") shouldBe true
    }

    @Test
    fun `user override with a None threshold blocks that user`() {
        val filter = filter(
            LoggingConstraints(
                defaultLogLevel = LogLevel.Verbose,
                userLogLevel = mapOf("user1" to UserLogLevel(defaultLogLevel = LogLevel.None))
            )
        )

        filter.canLog("Any", LogLevel.Error, "user1") shouldBe false
    }

    @Test
    fun `updating loggingConstraints changes filtering`() {
        val filter = filter(LoggingConstraints(defaultLogLevel = LogLevel.Error))
        filter.canLog("Any", LogLevel.Info, null) shouldBe false

        filter.loggingConstraints = LoggingConstraints(defaultLogLevel = LogLevel.Verbose)

        filter.canLog("Any", LogLevel.Info, null) shouldBe true
    }
}
