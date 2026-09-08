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
@file:OptIn(ExperimentalAmplifyApi::class)

package com.amplifyframework.cloudwatch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import aws.sdk.kotlin.services.cloudwatchlogs.model.FilterLogEventsRequest
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Resources
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toAwsCredentialsProvider
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.LogMessage
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// Give the async on-device write a moment to land before the first flush.
private const val SAVE_WAIT_MS = 2_000L

// CloudWatch ingestion is eventually consistent; poll with a re-flush between attempts.
private const val INGEST_WAIT_MS = 20_000L
private const val MAX_FLUSH_ATTEMPTS = 6

/**
 * Integration tests for [AmplifyCloudWatchClient] against a real CloudWatch Logs backend.
 *
 * Ported from the Amplify Swift `CloudWatchLoggingClientIntegrationTests`, and structured like the
 * `:aws-kinesis` client instrumentation tests. Credentials come from the provisioned backend's
 * Cognito identity pool (guest access — no sign-in required).
 *
 * Backend config is supplied at test time (not committed) under `src/androidTest/res/raw/`:
 * - `amplify_outputs.json` — Auth (Cognito user pool + identity pool)
 * - `amplifyconfiguration_logging.json` — `{ "cloudWatchClient": { region, logGroupName, ... } }`
 *
 * The target log group must be provisioned in the test account.
 */
@RunWith(AndroidJUnit4::class)
class AmplifyCloudWatchClientInstrumentationTest : DeviceFarmTestBase() {

    companion object {
        private lateinit var credentialsProvider: AwsCredentialsProvider<AwsCredentials>
        private lateinit var testRegion: String
        private lateinit var testLogGroupName: String

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(
                AmplifyOutputs(Resources.getRawResourceId(context, "amplify_outputs")),
                context
            )
            credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()

            val config = Resources.readJsonResource(context, "amplifyconfiguration_logging")
                .getJSONObject("cloudWatchClient")
            testRegion = config.getString("region")
            testLogGroupName = config.getString("logGroupName")
        }
    }

    private lateinit var client: AmplifyCloudWatchClient

    @Before
    fun setUp() {
        client = newClient()
    }

    @After
    fun tearDown() {
        client.disable()
    }

    /** The escape hatch exposes the underlying AWS CloudWatch Logs client. */
    @Test
    fun testGetEscapeHatch() {
        client.getCloudWatchLogsClient().shouldNotBeNull()
    }

    /** Emitted messages at each level are flushed to CloudWatch and appear in the log group. */
    @Test
    fun testFlushLogWithMessages(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val marker = "flush test ${System.currentTimeMillis()}"
        // A distinct payload per level so filterLogEvents returns a stable set (not one message x4).
        val levels = listOf(LogLevel.Error, LogLevel.Debug, LogLevel.Warn, LogLevel.Info)
        levels.forEach { level ->
            client.emit(LogMessage(level, namespace, "$marker/$level", null))
        }
        delay(SAVE_WAIT_MS)

        val events = awaitEvents(marker, expectedCount = levels.size)

        events shouldHaveSize levels.size
        events.forEach { event ->
            event shouldContain marker
            event shouldContain namespace
        }
    }

    /** A verbose message emitted while enabled is flushed to CloudWatch. */
    @Test
    fun testFlushLogWithVerboseMessageAfterEnabling(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val message = "verbose message after enabling ${System.currentTimeMillis()}"

        client.enable()
        client.emit(LogMessage(LogLevel.Verbose, namespace, message, null))
        delay(SAVE_WAIT_MS)

        val events = awaitEvents(message, expectedCount = 1)

        events shouldHaveSize 1
        // The emit format is "$level/$name: $content"; matching the prefix confirms we captured this
        // exact level, not just any message that happened to include the word "verbose" in its body.
        events.first() shouldStartWith "verbose/$namespace: "
        events.first() shouldContain message
    }

    /**
     * A verbose message emitted while disabled is dropped and never reaches CloudWatch.
     *
     * Uses a positive control emitted on the same client BEFORE `disable()` — this proves the flush
     * pipeline actually works, so the negative assertion below has teeth. Without the control, a
     * broken client that never emitted anything would pass this test vacuously.
     */
    @Test
    fun testFlushLogWithVerboseMessageAfterDisabling(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val controlMessage = "positive-control error before disable $timestamp"
        val disabledMessage = "verbose message after disabling $timestamp"

        // Positive control: emitted while enabled, must land in CloudWatch.
        client.emit(LogMessage(LogLevel.Error, namespace, controlMessage, null))
        delay(SAVE_WAIT_MS)
        awaitEvents(controlMessage, expectedCount = 1) shouldHaveSize 1

        // Negative case: emit while disabled, flush, and confirm nothing lands.
        client.disable()
        client.emit(LogMessage(LogLevel.Verbose, namespace, disabledMessage, null))
        delay(SAVE_WAIT_MS)
        client.flushLogs().shouldBeSuccess()
        delay(INGEST_WAIT_MS)

        filterEvents(disabledMessage, windowMinutes = 3).shouldBeEmpty()
    }

    // region helpers

    private fun newClient(): AmplifyCloudWatchClient = AmplifyCloudWatchClient(
        context = ApplicationProvider.getApplicationContext<Context>(),
        region = testRegion,
        credentialsProvider = credentialsProvider,
        options = AmplifyCloudWatchClientOptions {
            logGroupName = testLogGroupName
            // Manual flush only — matches the kinesis integration tests; avoids the interval auto-flush
            // racing the explicit flushes below.
            flushStrategy = FlushStrategy.None
            // Test-only baseline: capture everything (config's default level is unrelated to what
            // this suite exercises).
            loggingConstraints = LoggingConstraints(defaultLogLevel = LogLevel.Verbose)
        }
    )

    /**
     * Flushes and polls CloudWatch until at least [expectedCount] events matching [message] are found
     * (or attempts are exhausted). Each flush result is asserted to be a success so a broken flush
     * pipeline can't silently return an empty event list.
     */
    private suspend fun awaitEvents(message: String, expectedCount: Int): List<String> {
        var events = emptyList<String>()
        repeat(MAX_FLUSH_ATTEMPTS) { attempt ->
            client.flushLogs().shouldBeSuccess()
            delay(INGEST_WAIT_MS)
            events = filterEvents(message, windowMinutes = attempt + 2)
            if (events.size >= expectedCount) return events
        }
        return events
    }

    /** Returns the messages of CloudWatch log events matching [message] within the last [windowMinutes]. */
    private suspend fun filterEvents(message: String, windowMinutes: Int): List<String> {
        val group = testLogGroupName
        val end = System.currentTimeMillis()
        val start = end - windowMinutes * 60_000L
        val response = client.getCloudWatchLogsClient().filterLogEvents(
            FilterLogEventsRequest {
                logGroupName = group
                filterPattern = "\"$message\""
                startTime = start
                endTime = end
            }
        )
        return response.events?.mapNotNull { it.message } ?: emptyList()
    }

    // endregion
}
