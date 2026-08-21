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
import com.amplifyframework.cloudwatch.test.R
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toAwsCredentialsProvider
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.LogMessage
import com.amplifyframework.testutils.DeviceFarmTestBase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
        private var testLocalStoreMaxSizeInMB: Int = 1
        private var testFlushIntervalInSeconds: Long = 60
        private lateinit var testDefaultLogLevel: LogLevel
        private var configured = false

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            if (!configured) {
                Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(AmplifyOutputs(R.raw.amplify_outputs), context)
                configured = true
            }
            credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()

            val json = context.resources.openRawResource(R.raw.amplifyconfiguration_logging)
                .bufferedReader().use { it.readText() }
            val config = JSONObject(json).getJSONObject("cloudWatchClient")
            testRegion = config.getString("region")
            testLogGroupName = config.getString("logGroupName")
            testLocalStoreMaxSizeInMB = config.getInt("localStoreMaxSizeInMB")
            testFlushIntervalInSeconds = config.getLong("flushIntervalInSeconds")
            testDefaultLogLevel = logLevelOf(config.getJSONObject("loggingConstraints").getString("defaultLogLevel"))
        }

        // Config stores levels as e.g. "VERBOSE"; map to the LogLevel enum ("Verbose", "Error", ...).
        private fun logLevelOf(value: String): LogLevel =
            LogLevel.valueOf(value.lowercase().replaceFirstChar { it.uppercase() })
    }

    private lateinit var client: AmplifyCloudWatchClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        client = AmplifyCloudWatchClient(
            context = context,
            region = testRegion,
            credentialsProvider = credentialsProvider,
            options = AmplifyCloudWatchClientOptions {
                logGroupName = testLogGroupName
                localStoreMaxSizeInMB = testLocalStoreMaxSizeInMB
                flushStrategy = FlushStrategy.Interval(testFlushIntervalInSeconds.seconds)
                loggingConstraints = LoggingConstraints(defaultLogLevel = testDefaultLogLevel)
            }
        )
    }

    /** The escape hatch exposes the underlying AWS CloudWatch Logs client. */
    @Test
    fun testGetEscapeHatch() {
        client.getCloudWatchLogsClient().shouldNotBeNull()
    }

    /** Emitted messages are flushed to CloudWatch and appear in the log group. */
    @Test
    fun testFlushLogWithMessages(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val message = "this is an error message in the integration test ${System.currentTimeMillis()}"

        client.emit(LogMessage(LogLevel.Error, namespace, message, null))
        client.emit(LogMessage(LogLevel.Debug, namespace, message, null))
        client.emit(LogMessage(LogLevel.Warn, namespace, message, null))
        client.emit(LogMessage(LogLevel.Info, namespace, message, null))
        delay(SAVE_WAIT_MS)

        val events = awaitEvents(message, expectedCount = 4)

        events shouldHaveSize 4
        events.forEach { event ->
            event shouldContain message
            event shouldContain namespace
        }
    }

    /** A verbose message emitted while enabled is flushed to CloudWatch. */
    @Test
    fun testFlushLogWithVerboseMessageAfterEnabling(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val message = "this is a verbose message after enabling ${System.currentTimeMillis()}"

        client.enable()
        client.emit(LogMessage(LogLevel.Verbose, namespace, message, null))
        delay(SAVE_WAIT_MS)

        val events = awaitEvents(message, expectedCount = 1)

        events shouldHaveSize 1
        events.first().lowercase() shouldContain "verbose"
        events.first() shouldContain message
        events.first() shouldContain namespace
    }

    /** A verbose message emitted while disabled is dropped and never reaches CloudWatch. */
    @Test
    fun testFlushLogWithVerboseMessageAfterDisabling(): Unit = runBlocking {
        val namespace = UUID.randomUUID().toString()
        val message = "this is a verbose message after disabling ${System.currentTimeMillis()}"

        client.disable()
        client.emit(LogMessage(LogLevel.Verbose, namespace, message, null))
        delay(SAVE_WAIT_MS)

        awaitEvents(message, expectedCount = 0).shouldBeEmpty()
    }

    // region helpers

    /**
     * Flushes and polls CloudWatch until at least [expectedCount] events matching [message] are found
     * (or attempts are exhausted). For [expectedCount] == 0 a single poll after one flush is enough.
     */
    private suspend fun awaitEvents(message: String, expectedCount: Int): List<String> {
        var events = emptyList<String>()
        repeat(MAX_FLUSH_ATTEMPTS) { attempt ->
            client.flushLogs()
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
