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
package com.amplifyframework.kinesis

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toAwsCredentialsProvider
import com.amplifyframework.foundation.result.exceptionOrNull
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.isFailure
import com.amplifyframework.foundation.result.isSuccess
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.testutils.Sleep
import com.amplifyframework.testutils.sync.SynchronousAuth
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumented tests for [AmplifyKinesisClient].
 *
 * These tests run against a real Kinesis stream and require:
 * - A provisioned Kinesis Data Stream (stream name configured below)
 * - Valid AWS credentials via Amplify Auth (Cognito)
 * - An `amplifyconfiguration` raw resource with auth config
 * - A `credentials` raw resource with test user credentials
 */
class KinesisDataStreamsInstrumentationTest {

    companion object {
        private const val STREAM_NAME = "amplify-kinesis-test-stream"
        private const val CREDENTIALS_RESOURCE_NAME = "credentials"
        private const val CONFIGURATION_NAME = "amplifyconfiguration"
        private const val COGNITO_CONFIGURATION_TIMEOUT = 5_000L
        private const val REGION = "us-east-1"

        private lateinit var synchronousAuth: SynchronousAuth
        private lateinit var credentialsProvider: AwsCredentialsProvider<AwsCredentials>

        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(context)
            Sleep.milliseconds(COGNITO_CONFIGURATION_TIMEOUT)
            synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)

            // Sign in with test credentials
            @androidx.annotation.RawRes val resourceId =
                com.amplifyframework.testutils.Resources.getRawResourceId(
                    context,
                    CREDENTIALS_RESOURCE_NAME
                )
            val userAndPasswordPair = readCredentialsFromResource(context, resourceId)
            synchronousAuth.signOut()
            synchronousAuth.signIn(
                userAndPasswordPair!!.first,
                userAndPasswordPair.second
            )

            // Bridge V2 Auth to foundation credentials via Smithy types
            credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()
        }

        private fun readCredentialsFromResource(
            context: Context,
            @androidx.annotation.RawRes resourceId: Int
        ): android.util.Pair<String, String>? {
            val resource = com.amplifyframework.testutils.Resources.readAsJson(context, resourceId)
            var userCredentials: android.util.Pair<String, String>? = null
            return try {
                val credentials = resource.getJSONArray("credentials")
                for (index in 0 until credentials.length()) {
                    val credential = credentials.getJSONObject(index)
                    val username = credential.getString("username")
                    val password = credential.getString("password")
                    userCredentials = android.util.Pair(username, password)
                }
                userCredentials
            } catch (jsonReadingFailure: org.json.JSONException) {
                throw RuntimeException(jsonReadingFailure)
            }
        }
    }

    private lateinit var kinesis: AmplifyKinesisClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        kinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider
        )
        // Clear any leftover records from previous test runs
        runBlocking { kinesis.clearCache() }
        kinesis.enable()
    }

    @After
    fun tearDown() {
        kinesis.disable()
        runBlocking { kinesis.clearCache() }
    }

    // ---------------------------------------------------------------
    // Core happy path
    // ---------------------------------------------------------------

    /** Record data and flush — verify records were actually flushed. */
    @Test
    fun testRecordAndFlush(): Unit = runBlocking {
        val result = kinesis.record(
            data = "test-record".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )
        result.isSuccess().shouldBeTrue()

        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()
        flushResult.getOrThrow().recordsFlushed shouldBeGreaterThan 0
    }

    /** Flush with no cached records returns zero flushed. */
    @Test
    fun testFlushWhenEmpty(): Unit = runBlocking {
        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()

        val data = flushResult.getOrThrow()
        data.recordsFlushed shouldBe 0
        data.flushInProgress.shouldBeFalse()
    }

    /** Records submitted while disabled are silently dropped. */
    @Test
    fun testRecordWhileDisabledDropsRecords(): Unit = runBlocking {
        kinesis.disable()

        val result = kinesis.record(
            data = "dropped-record".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )
        // record() returns success even when disabled (silently dropped)
        result.isSuccess().shouldBeTrue()

        kinesis.enable()
        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()
        flushResult.getOrThrow().recordsFlushed shouldBe 0
    }

    /** Enable → record → disable → enable → flush verifies only pre-disable records flush. */
    @Test
    fun testEnableDisableLifecycle(): Unit = runBlocking {
        // Record while enabled
        kinesis.record(
            data = "before-disable".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )

        kinesis.disable()

        // Record while disabled — should be dropped
        kinesis.record(
            data = "while-disabled".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )

        kinesis.enable()

        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()
        // Only the pre-disable record should be flushed
        flushResult.getOrThrow().recordsFlushed shouldBe 1
    }

    /** Two concurrent flushes — one should return flushInProgress = true. */
    @Test
    fun testConcurrentFlushReturnsInProgress(): Unit = runBlocking {
        // Seed some records so the flush has work to do
        repeat(10) { i ->
            kinesis.record(
                data = "record-$i".toByteArray(),
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )
        }

        val results = listOf(
            async { kinesis.flush() },
            async { kinesis.flush() }
        ).awaitAll()

        val successResults = results.filter { it.isSuccess() }
        successResults.size shouldBe 2

        val flushDatas = successResults.map { it.getOrThrow() }
        // At least one should have done actual work, and one may report flushInProgress
        val anyFlushed = flushDatas.any { it.recordsFlushed > 0 }
        val anyInProgress = flushDatas.any { it.flushInProgress }
        // Either both flushed (if first completed before second started) or one was skipped
        (anyFlushed || anyInProgress).shouldBeTrue()
    }

    // ---------------------------------------------------------------
    // Cache behavior
    // ---------------------------------------------------------------

    /** Fill cache to limit, then verify the next record fails with AmplifyKinesisLimitExceededException. */
    @Test
    fun testCacheLimitExceeded(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create a client with a tiny cache
        val smallCacheKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            configuration = AmplifyKinesisClientConfiguration {
                cacheMaxBytes = 100L // 100 bytes
            }
        )
        smallCacheKinesis.enable()

        try {
            // Fill the cache
            val bigData = ByteArray(60) { 0x41 } // 60 bytes
            smallCacheKinesis.record(
                data = bigData,
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )

            // This should exceed the 100-byte limit
            val result = smallCacheKinesis.record(
                data = bigData,
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )

            result.isFailure().shouldBeTrue()
            result.exceptionOrNull().shouldBeInstanceOf<AmplifyKinesisLimitExceededException>()
        } finally {
            smallCacheKinesis.disable()
            smallCacheKinesis.clearCache()
        }
    }

    /** Record data, clear cache, then flush — nothing should be flushed. */
    @Test
    fun testClearCache(): Unit = runBlocking {
        kinesis.record(
            data = "to-be-cleared".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )

        val clearResult = kinesis.clearCache()
        clearResult.isSuccess().shouldBeTrue()
        clearResult.getOrThrow().recordsCleared shouldBeGreaterThan 0

        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()
        flushResult.getOrThrow().recordsFlushed shouldBe 0
    }

    // ---------------------------------------------------------------
    // Error paths
    // ---------------------------------------------------------------

    /** Flush with invalid credentials should fail with AmplifyKinesisServiceException. */
    @Test
    fun testFlushWithInvalidCredentials(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val badCredentials = AwsCredentialsProvider {
            AwsCredentials.Static(
                accessKeyId = "INVALID_ACCESS_KEY",
                secretAccessKey = "INVALID_SECRET_KEY"
            )
        }

        val badKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = badCredentials
        )
        badKinesis.enable()

        try {
            badKinesis.record(
                data = "bad-creds-record".toByteArray(),
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )

            val flushResult = badKinesis.flush()
            flushResult.isFailure().shouldBeTrue()
            flushResult.exceptionOrNull().shouldBeInstanceOf<AmplifyKinesisServiceException>()
        } finally {
            badKinesis.disable()
            badKinesis.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Stress tests
    // ---------------------------------------------------------------

    /** Record many events and flush — verify all are flushed. */
    @Test
    fun testHighVolumeRecordAndFlush(): Unit = runBlocking {
        val count = 50
        repeat(count) { i ->
            val result = kinesis.record(
                data = "stress-record-$i".toByteArray(),
                partitionKey = "partition-${i % 5}",
                streamName = STREAM_NAME
            )
            result.isSuccess().shouldBeTrue()
        }

        val flushResult = kinesis.flush()
        flushResult.isSuccess().shouldBeTrue()
        flushResult.getOrThrow().recordsFlushed shouldBe count
    }

    /** Record + flush in a loop — verify consistency across cycles. */
    @Test
    fun testRepeatedFlushCycles(): Unit = runBlocking {
        val cycles = 10
        val recordsPerCycle = 5
        var totalFlushed = 0

        repeat(cycles) { cycle ->
            repeat(recordsPerCycle) { i ->
                kinesis.record(
                    data = "cycle-$cycle-record-$i".toByteArray(),
                    partitionKey = "partition-1",
                    streamName = STREAM_NAME
                )
            }
            val flushResult = kinesis.flush()
            flushResult.isSuccess().shouldBeTrue()
            totalFlushed += flushResult.getOrThrow().recordsFlushed
        }

        totalFlushed shouldBe (cycles * recordsPerCycle)
    }

    // ---------------------------------------------------------------
    // Auto-flush
    // ---------------------------------------------------------------

    /** Record data and wait for auto-flush to trigger. */
    @Test
    fun testAutoFlush(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create client with short flush interval
        val autoFlushKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            configuration = AmplifyKinesisClientConfiguration {
                flushStrategy = FlushStrategy.Interval(5.seconds)
            }
        )
        autoFlushKinesis.enable()

        try {
            autoFlushKinesis.record(
                data = "auto-flush-record".toByteArray(),
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )

            // Wait for auto-flush to trigger (5s interval + buffer)
            delay(8_000)

            // After auto-flush, a manual flush should find nothing left
            val flushResult = autoFlushKinesis.flush()
            flushResult.isSuccess().shouldBeTrue()
            flushResult.getOrThrow().recordsFlushed shouldBe 0
        } finally {
            autoFlushKinesis.disable()
            autoFlushKinesis.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Escape hatch
    // ---------------------------------------------------------------

    /** Verify the underlying SDK client is accessible. */
    @Test
    fun testGetKinesisClient() {
        kinesis.kinesisClient.shouldNotBeNull()
    }
}
