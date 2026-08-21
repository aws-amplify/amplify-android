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

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.LogMessage
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class, ExperimentalCoroutinesApi::class)
class AmplifyCloudWatchClientTest {

    private val logManager = mockk<CloudWatchLogManager>(relaxed = true)
    private val sdkClient = mockk<CloudWatchLogsClient>(relaxed = true)

    private fun TestScope.createClient(
        constraints: LoggingConstraints = LoggingConstraints(defaultLogLevel = LogLevel.Verbose)
    ) = AmplifyCloudWatchClient(
        cloudWatchLogsClient = sdkClient,
        initialConstraints = constraints,
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    ) { _, _ -> logManager }

    private fun message(
        level: LogLevel = LogLevel.Info,
        name: String = "Storage",
        content: String = "hello",
        cause: Throwable? = null
    ) = LogMessage(level, name, content, cause)

    @Test
    fun `isEnabledFor reflects the enabled state`() = runTest {
        val client = createClient()

        client.isEnabledFor(LogLevel.Info) shouldBe true
        client.disable()
        client.isEnabledFor(LogLevel.Info) shouldBe false
        client.enable()
        client.isEnabledFor(LogLevel.Info) shouldBe true
    }

    @Test
    fun `emit forwards a passing message to the log manager`() = runTest {
        val client = createClient()

        client.emit(message(level = LogLevel.Info, name = "Storage", content = "hello"))

        coVerify(exactly = 1) { logManager.saveLogEvent(withArg { it.message shouldBe "info/Storage: hello" }) }
    }

    @Test
    fun `emit appends the error when the message has a cause`() = runTest {
        val client = createClient()

        client.emit(message(content = "failed", cause = RuntimeException("bad")))

        coVerify {
            logManager.saveLogEvent(
                withArg {
                    it.message shouldContain "failed"
                    it.message shouldContain "error:"
                }
            )
        }
    }

    @Test
    fun `emit drops the message when the client is disabled`() = runTest {
        val client = createClient()
        client.disable()

        client.emit(message())

        coVerify(exactly = 0) { logManager.saveLogEvent(any()) }
    }

    @Test
    fun `emit drops messages filtered out by constraints`() = runTest {
        val client = createClient(LoggingConstraints(defaultLogLevel = LogLevel.Error))

        client.emit(message(level = LogLevel.Info))
        coVerify(exactly = 0) { logManager.saveLogEvent(any()) }

        client.emit(message(level = LogLevel.Error))
        coVerify(exactly = 1) { logManager.saveLogEvent(any()) }
    }

    @Test
    fun `flushLogs returns success when the manager flush succeeds`() = runTest {
        val client = createClient()

        client.flushLogs().shouldBeSuccess()

        coVerify(exactly = 1) { logManager.syncLogEventsWithCloudwatch() }
    }

    @Test
    fun `flushLogs returns failure when the manager flush throws`() = runTest {
        coEvery { logManager.syncLogEventsWithCloudwatch() } throws IllegalStateException("boom")
        val client = createClient()

        val failure = client.flushLogs().shouldBeFailure()

        failure.error.shouldBeInstanceOf<AmplifyCloudWatchUnknownException>()
    }

    @Test
    fun `disable stops the sync`() = runTest {
        val client = createClient()

        client.disable()

        verify(exactly = 1) { logManager.stopSync() }
    }

    @Test
    fun `enable starts the sync`() = runTest {
        val client = createClient() // construction triggers one startSync

        client.enable()

        coVerify(exactly = 2) { logManager.startSync() }
    }

    @Test
    fun `setUserIdentifier updates the manager`() = runTest {
        val client = createClient()

        client.setUserIdentifier("user-1")

        verify(exactly = 1) { logManager.setUserIdentifier("user-1") }
    }

    @Test
    fun `setLoggingConstraints changes emit filtering`() = runTest {
        val client = createClient(LoggingConstraints(defaultLogLevel = LogLevel.Error))

        client.emit(message(level = LogLevel.Info))
        coVerify(exactly = 0) { logManager.saveLogEvent(any()) }

        client.setLoggingConstraints(LoggingConstraints(defaultLogLevel = LogLevel.Verbose))
        client.emit(message(level = LogLevel.Info))
        coVerify(exactly = 1) { logManager.saveLogEvent(any()) }
    }

    @Test
    fun `getCloudWatchLogsClient returns the underlying client`() = runTest {
        val client = createClient()

        client.getCloudWatchLogsClient() shouldBe sdkClient
    }
}
