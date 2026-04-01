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
package com.amplifyframework.recordcache

import android.content.Context
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toAwsCredentialsProvider
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.kinesis.test.R
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import com.amplifyframework.testutils.sync.SynchronousAuth
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Shared instrumentation tests for stream-based record clients.
 *
 * Both [AmplifyKinesisClient] and [com.amplifyframework.firehose.AmplifyFirehoseClient]
 * share the same [com.amplifyframework.recordcache.RecordClient] core with identical
 * flush, cache, retry, and lifecycle semantics. This base class captures those shared
 * behaviours so they are tested once and inherited by both concrete test classes.
 *
 * Subclasses provide:
 * - [streamName]: a real, provisioned stream/delivery-stream name
 * - [createDefaultClient]: builds a [TestableStreamClient] with default options
 * - [createClientWithSmallCache]: builds one with a tiny cache for limit tests
 * - [createClientWithMaxRetries]: builds one with a specific maxRetries + no auto-flush
 * - [createClientWithAutoFlush]: builds one with a short auto-flush interval
 * - [createClientWithBadCredentials]: builds one with invalid AWS credentials
 * - [assertLimitExceededError]: asserts the service-specific limit-exceeded exception
 * - [assertValidationError]: asserts the service-specific validation exception
 * - [oversizedRecordSize]: byte count that exceeds the per-record size limit
 */
abstract class BaseStreamClientInstrumentationTest {

    companion object {
        const val REGION = "us-east-1"

        lateinit var synchronousAuth: SynchronousAuth
        lateinit var credentialsProvider: AwsCredentialsProvider<AwsCredentials>

        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(AmplifyOutputs(R.raw.amplify_outputs), context)
            synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)

            @RawRes val resourceId = R.raw.credentials
            val (user, password) = readCredentialsFromResource(context, resourceId)
            synchronousAuth.signOut()
            synchronousAuth.signIn(user, password)

            credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()
        }

        private fun readCredentialsFromResource(context: Context, @RawRes resourceId: Int): Pair<String, String> {
            val resource = Resources.readAsJson(context, resourceId)
            return try {
                val credentials = resource.getJSONArray("credentials")
                val lastIndex = credentials.length() - 1
                val credential = credentials.getJSONObject(lastIndex)
                Pair(credential.getString("username"), credential.getString("password"))
            } catch (e: org.json.JSONException) {
                throw e
            }
        }
    }

    // ---------------------------------------------------------------
    // Abstract contract — subclasses provide these
    // ---------------------------------------------------------------

    abstract val streamName: String
    abstract val oversizedRecordSize: Int

    abstract fun createDefaultClient(context: Context): TestableStreamClient
    abstract fun createClientWithSmallCache(context: Context, cacheMaxBytes: Long): TestableStreamClient
    abstract fun createClientWithMaxRetries(context: Context, maxRetries: Int): TestableStreamClient
    abstract fun createClientWithAutoFlush(context: Context, intervalSeconds: Int): TestableStreamClient
    abstract fun createClientWithBadCredentials(context: Context): TestableStreamClient

    /** Assert that the result is a failure with the service-specific limit-exceeded exception. */
    abstract fun assertLimitExceededError(result: Result<*, *>)

    /** Assert that the result is a failure with the service-specific validation exception. */
    abstract fun assertValidationError(result: Result<*, *>)

    // ---------------------------------------------------------------
    // Setup / teardown
    // ---------------------------------------------------------------

    protected lateinit var client: TestableStreamClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        client = createDefaultClient(context)
        runBlocking { client.clearCache() }
        client.enable()
    }

    @After
    fun tearDown() {
        client.disable()
        runBlocking { client.clearCache() }
    }

    // ---------------------------------------------------------------
    // Core happy path
    // ---------------------------------------------------------------

    @Test
    fun testRecordAndFlush(): Unit = runBlocking {
        val result = client.record("test-record".toByteArray(), streamName)
        result.shouldBeSuccess()

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBeGreaterThan 0
    }

    @Test
    fun testFlushWhenEmpty(): Unit = runBlocking {
        val flushResult = client.flush()
        val data = flushResult.shouldBeSuccess().data
        data.recordsFlushed shouldBe 0
        data.flushInProgress.shouldBeFalse()
    }

    @Test
    fun testRecordWhileDisabledDropsRecords(): Unit = runBlocking {
        client.disable()

        val result = client.record("dropped-record".toByteArray(), streamName)
        result.shouldBeSuccess()

        client.enable()
        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
    }

    @Test
    fun testEnableDisableLifecycle(): Unit = runBlocking {
        client.record("before-disable".toByteArray(), streamName)
        client.disable()
        client.record("while-disabled".toByteArray(), streamName)
        client.enable()

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
    }

    @Test
    fun testConcurrentFlushReturnsInProgress(): Unit = runBlocking {
        repeat(10) { i ->
            client.record("record-$i".toByteArray(), streamName)
        }

        val results = listOf(
            async { client.flush() },
            async { client.flush() }
        ).awaitAll()

        results.size shouldBe 2
        val flushDatas = results.map { it.shouldBeSuccess().data }
        val anyFlushed = flushDatas.any { it.recordsFlushed > 0 }
        val anyInProgress = flushDatas.any { it.flushInProgress }
        (anyFlushed || anyInProgress).shouldBeTrue()
    }

    // ---------------------------------------------------------------
    // Cache behavior
    // ---------------------------------------------------------------

    @Test
    fun testCacheLimitExceeded(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val smallClient = createClientWithSmallCache(context, cacheMaxBytes = 100L)
        smallClient.enable()

        try {
            val bigData = ByteArray(60) { 0x41 }
            smallClient.record(bigData, streamName)

            val result = smallClient.record(bigData, streamName)
            assertLimitExceededError(result)
        } finally {
            smallClient.disable()
            smallClient.clearCache()
        }
    }

    @Test
    fun testClearCache(): Unit = runBlocking {
        client.record("to-be-cleared".toByteArray(), streamName)

        val clearResult = client.clearCache()
        clearResult.shouldBeSuccess().data.recordsCleared shouldBeGreaterThan 0

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
    }

    // ---------------------------------------------------------------
    // Error paths
    // ---------------------------------------------------------------

    @Test
    fun testFlushWithNonexistentStreamName(): Unit = runBlocking {
        client.record("wrong-stream-record".toByteArray(), "nonexistent-stream-name")
        client.record("valid-stream-record".toByteArray(), streamName)

        val flushResult = client.flush()
        flushResult.shouldBeSuccess()
        flushResult.data.recordsFlushed shouldBe 1

        client.clearCache()
    }

    @Test
    fun testFlushWithInvalidCredentials(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val badClient = createClientWithBadCredentials(context)
        badClient.enable()

        try {
            badClient.record("bad-creds-record".toByteArray(), streamName)

            val flushResult = badClient.flush()
            flushResult.shouldBeSuccess()
            flushResult.data.recordsFlushed shouldBe 0
        } finally {
            badClient.disable()
            badClient.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Retry exhaustion
    // ---------------------------------------------------------------

    @Test
    fun testInvalidStreamRecordIsDroppedAfterMaxRetries(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val maxRetries = 5
        val retryClient = createClientWithMaxRetries(context, maxRetries)
        retryClient.enable()

        try {
            retryClient.clearCache()

            retryClient.record("invalid-stream-record".toByteArray(), "nonexistent-stream-name")
            retryClient.record("valid-stream-record".toByteArray(), streamName)

            val firstFlush = retryClient.flush()
            firstFlush.shouldBeSuccess().data.recordsFlushed shouldBe 1

            repeat(maxRetries) {
                val result = retryClient.flush()
                result.shouldBeSuccess().data.recordsFlushed shouldBe 0
            }

            val clearResult = retryClient.clearCache()
            clearResult.shouldBeSuccess().data.recordsCleared shouldBe 0
        } finally {
            retryClient.disable()
            retryClient.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Stress tests
    // ---------------------------------------------------------------

    @Test
    fun testHighVolumeRecordAndFlush(): Unit = runBlocking {
        val count = 50
        repeat(count) { i ->
            val result = client.record("stress-record-$i".toByteArray(), streamName)
            result.shouldBeSuccess()
        }

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe count
    }

    @Test
    fun testRepeatedFlushCycles(): Unit = runBlocking {
        val cycles = 5
        val recordsPerCycle = 5
        var totalFlushed = 0

        repeat(cycles) { cycle ->
            repeat(recordsPerCycle) { i ->
                client.record("cycle-$cycle-record-$i".toByteArray(), streamName)
            }
            val flushResult = client.flush()
            flushResult.shouldBeSuccess()
            totalFlushed += flushResult.data.recordsFlushed
        }

        totalFlushed shouldBe (cycles * recordsPerCycle)
    }

    @Test
    fun testConcurrentRecordAndFlushStress(): Unit = runBlocking {
        val producers = 5
        val recordsPerProducer = 20
        val totalExpected = producers * recordsPerProducer
        val totalFlushed = java.util.concurrent.atomic.AtomicInteger(0)
        val producersDone = java.util.concurrent.atomic.AtomicBoolean(false)

        val flusher = async {
            while (!producersDone.get()) {
                val result = client.flush()
                if (result is Result.Success) {
                    totalFlushed.addAndGet(result.data.recordsFlushed)
                }
                delay(500)
            }
        }

        val producerJobs = (0 until producers).map { p ->
            async {
                repeat(recordsPerProducer) { i ->
                    client.record("stress-p$p-r$i".toByteArray(), streamName)
                }
            }
        }

        producerJobs.awaitAll()
        producersDone.set(true)
        flusher.await()

        val drainResult = client.flush()
        drainResult.shouldBeSuccess()
        totalFlushed.addAndGet(drainResult.data.recordsFlushed)

        val finalResult = client.flush()
        finalResult.shouldBeSuccess().data.recordsFlushed shouldBe 0

        totalFlushed.get() shouldBe totalExpected
    }

    // ---------------------------------------------------------------
    // Multi-batch flush
    // ---------------------------------------------------------------

    /**
     * Records more than 500 entries (the per-request record limit) to a single
     * stream, then calls flush() once. Verifies that a single flush drains all
     * records across multiple batches.
     */
    @Test
    fun testSingleFlushDrainsMultipleBatches(): Unit = runBlocking {
        val recordCount = 1100

        repeat(recordCount) { i ->
            val result = client.record("batch-record-$i".toByteArray(), streamName)
            result.shouldBeSuccess()
        }

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe recordCount

        val secondFlush = client.flush()
        secondFlush.shouldBeSuccess().data.recordsFlushed shouldBe 0
    }

    // ---------------------------------------------------------------
    // Auto-flush
    // ---------------------------------------------------------------

    @Test
    fun testAutoFlushStartsWithoutExplicitEnable(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val autoClient = createClientWithAutoFlush(context, intervalSeconds = 3)
        // No explicit enable() — scheduler should auto-start from init

        try {
            autoClient.record("auto-start-record".toByteArray(), streamName)
            delay(6_000)

            val flushResult = autoClient.flush()
            flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            autoClient.disable()
            autoClient.clearCache()
        }
    }

    @Test
    fun testAutoFlush(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val autoClient = createClientWithAutoFlush(context, intervalSeconds = 5)
        autoClient.enable()

        try {
            autoClient.record("auto-flush-record".toByteArray(), streamName)
            delay(8_000)

            val flushResult = autoClient.flush()
            flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            autoClient.disable()
            autoClient.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Oversized record validation
    // ---------------------------------------------------------------

    @Test
    fun testOversizedRecordIsRejectedAndFlushStillWorks(): Unit = runBlocking {
        val oversizedData = ByteArray(oversizedRecordSize) { 0x42 }

        val oversizedResult = client.record(oversizedData, streamName)
        assertValidationError(oversizedResult)

        val validResult = client.record("still-works".toByteArray(), streamName)
        validResult.shouldBeSuccess()

        val flushResult = client.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
    }
}
