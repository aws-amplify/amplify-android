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
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
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
        private const val REGION = "us-east-1"

        private lateinit var synchronousAuth: SynchronousAuth
        private lateinit var credentialsProvider: AwsCredentialsProvider<AwsCredentials>

        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val outputsResourceId = Resources.getRawResourceId(context, "amplify_outputs")
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(AmplifyOutputs(outputsResourceId), context)
            synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)

            // Sign in with test credentials
            @RawRes val resourceId = Resources.getRawResourceId(
                context,
                CREDENTIALS_RESOURCE_NAME
            )
            val (user, password) = readCredentialsFromResource(context, resourceId)
            synchronousAuth.signOut()
            synchronousAuth.signIn(user, password)

            // Bridge V2 Auth to foundation credentials via Smithy types
            credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()
        }

        private fun readCredentialsFromResource(context: Context, @RawRes resourceId: Int): Pair<String, String> {
            val resource = Resources.readAsJson(context, resourceId)
            return try {
                val credentials = resource.getJSONArray("credentials")
                val lastIndex = credentials.length() - 1
                val credential = credentials.getJSONObject(lastIndex)
                val username = credential.getString("username")
                val password = credential.getString("password")
                Pair(username, password)
            } catch (jsonReadingFailure: org.json.JSONException) {
                throw jsonReadingFailure
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
        result.shouldBeSuccess()

        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBeGreaterThan 0
    }

    /** Flush with no cached records returns zero flushed. */
    @Test
    fun testFlushWhenEmpty(): Unit = runBlocking {
        val flushResult = kinesis.flush()
        val data = flushResult.shouldBeSuccess().data
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
        result.shouldBeSuccess()

        kinesis.enable()
        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
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
        // Only the pre-disable record should be flushed
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
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

        val successResults = results.filter { it is Result.Success }
        successResults.size shouldBe 2

        val flushDatas = successResults.map { (it as Result.Success).data }
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
            options = AmplifyKinesisClientOptions {
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

            result.shouldBeFailure().error.shouldBeInstanceOf<AmplifyKinesisLimitExceededException>()
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
        clearResult.shouldBeSuccess().data.recordsCleared shouldBeGreaterThan 0

        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
    }

    // ---------------------------------------------------------------
    // Error paths
    // ---------------------------------------------------------------

    /**
     * Flush with a nonexistent stream name should return Success (SDK errors are handled silently).
     * The valid stream's record should still be flushed, proving one bad stream
     * doesn't block others.
     */
    @Test
    fun testFlushWithNonexistentStreamName(): Unit = runBlocking {
        kinesis.record(
            data = "wrong-stream-record".toByteArray(),
            partitionKey = "partition-1",
            streamName = "nonexistent-stream-name"
        )
        kinesis.record(
            data = "valid-stream-record".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )

        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess()
        flushResult.data.recordsFlushed shouldBe 1

        kinesis.clearCache()
    }

    /** 
     * Flush with invalid credentials should return Success (SDK errors are handled silently).
     * Records are incremented and potentially deleted if they exceed retry limits.
     */
    @Test
    fun testFlushWithInvalidCredentials(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val badCredentials = AwsCredentialsProvider {
            AwsCredentials.Static(
                accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
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

            // SDK exceptions are handled silently - flush returns Success
            val flushResult = badKinesis.flush()
            flushResult.shouldBeSuccess()
            // Records are not flushed (SDK error), but the operation succeeds
            flushResult.data.recordsFlushed shouldBe 0
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
            result.shouldBeSuccess()
        }

        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe count
    }

    /** Record + flush in a loop — verify consistency across cycles. */
    @Test
    fun testRepeatedFlushCycles(): Unit = runBlocking {
        val cycles = 5
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
            flushResult.shouldBeSuccess()
            totalFlushed += flushResult.data.recordsFlushed
        }

        totalFlushed shouldBe (cycles * recordsPerCycle)
    }

    /**
     * Stress test: N producers record concurrently while a flusher calls flush()
     * every 500ms. Simulates real-world usage where the app records analytics
     * events while the auto-flush timer fires.
     *
     * Asserts that every recorded event is eventually flushed — no records lost
     * under concurrent read/write pressure on the cache.
     */
    @Test
    fun testConcurrentRecordAndFlushStress(): Unit = runBlocking {
        val producers = 5
        val recordsPerProducer = 20
        val totalExpected = producers * recordsPerProducer
        val totalFlushed = java.util.concurrent.atomic.AtomicInteger(0)
        val producersDone = java.util.concurrent.atomic.AtomicBoolean(false)

        // Flusher: calls flush() every 500ms until all producers are done + one final drain
        val flusher = async {
            while (!producersDone.get()) {
                val result = kinesis.flush()
                if (result is Result.Success) {
                    totalFlushed.addAndGet(result.data.recordsFlushed)
                }
                delay(500)
            }
        }

        // Producers: each records M events concurrently
        val producerJobs = (0 until producers).map { p ->
            async {
                repeat(recordsPerProducer) { i ->
                    kinesis.record(
                        data = "stress-p$p-r$i".toByteArray(),
                        partitionKey = "partition-${p % 3}",
                        streamName = STREAM_NAME
                    )
                }
            }
        }

        // Wait for all producers to finish, then signal the flusher
        producerJobs.awaitAll()
        producersDone.set(true)
        flusher.await()

        // Final drain flush to pick up anything the periodic flusher missed
        val drainResult = kinesis.flush()
        drainResult.shouldBeSuccess()
        totalFlushed.addAndGet(drainResult.data.recordsFlushed)

        // Second drain to confirm nothing is left
        val finalResult = kinesis.flush()
        finalResult.shouldBeSuccess().data.recordsFlushed shouldBe 0

        totalFlushed.get() shouldBe totalExpected
    }

    // ---------------------------------------------------------------
    // Auto-flush
    // ---------------------------------------------------------------

    /**
     * Verify that creating a client with default options (no explicit flushStrategy)
     * auto-starts the scheduler. Default is FlushStrategy.Interval(30s), so we override
     * to a short interval to keep the test fast.
     */
    @Test
    fun testDefaultConfigAutoStartsScheduler(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Default options use FlushStrategy.Interval(30s). We use a short interval
        // to verify the scheduler is auto-started without waiting 30 seconds.
        val defaultKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
                flushStrategy = FlushStrategy.Interval(3.seconds)
            }
        )
        // Note: no explicit enable() call — scheduler should auto-start from init

        try {
            defaultKinesis.record(
                data = "auto-start-record".toByteArray(),
                partitionKey = "partition-1",
                streamName = STREAM_NAME
            )

            // Wait for auto-flush to trigger (3s interval + buffer)
            delay(6_000)

            // After auto-flush, a manual flush should find nothing left
            val flushResult = defaultKinesis.flush()
            flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            defaultKinesis.disable()
            defaultKinesis.clearCache()
        }
    }

    /** Record data and wait for auto-flush to trigger. */
    @Test
    fun testAutoFlush(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create client with short flush interval
        val autoFlushKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
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
            flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            autoFlushKinesis.disable()
            autoFlushKinesis.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Partition key validation
    // ---------------------------------------------------------------

    /**
     * E2E test: Record with a partition key containing exactly 256 emoji Unicode scalars
     * (the maximum allowed), then flush to verify the record is accepted by Kinesis.
     *
     * Each emoji (😀) is 1 Unicode code point but 4 bytes in UTF-8. This test validates
     * that our code point counting is correct and that Kinesis accepts the
     * maximum-length partition key.
     */
    @Test
    fun testRecordWithMax256EmojiCodePointsAndFlush(): Unit = runBlocking {
        // Create partition key with exactly 256 emoji code points
        // Each emoji is 1 code point, 4 UTF-8 bytes
        val emoji = "\uD83D\uDE00" // 😀
        val emojiPartitionKey = emoji.repeat(256)

        // Verify our assumptions about the partition key
        val codePointCount = emojiPartitionKey.codePointCount(0, emojiPartitionKey.length)
        val utf8ByteCount = emojiPartitionKey.toByteArray(Charsets.UTF_8).size

        codePointCount shouldBe 256
        utf8ByteCount shouldBe 1024 // 256 emojis × 4 bytes each

        // Record with the emoji partition key
        val result = kinesis.record(
            data = "test-data-with-emoji-partition-key".toByteArray(),
            partitionKey = emojiPartitionKey,
            streamName = STREAM_NAME
        )
        result.shouldBeSuccess()

        // Flush and verify the record was sent successfully
        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
    }

    // ---------------------------------------------------------------
    // PutRecords size limits
    // ---------------------------------------------------------------

    /**
     * Fills the cache with >5 MB of data for a single stream using large partition
     * keys, then flushes. This exercises the PutRecords API limit of 10 MiB per
     * request and verifies the client handles batching/size correctly.
     *
     * Per the API spec, record size = partition key + data blob. Large partition
     * keys increase the effective record size beyond just the data blob.
     */
    @Test
    fun testFlushLargePayloadWithLargePartitionKeys(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val largeKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
                cacheMaxBytes = 12L * 1_024 * 1_024 // 12 MB cache to hold >10 MB
                flushStrategy = FlushStrategy.None
            }
        )
        largeKinesis.enable()

        try {
            // Each record: ~50 KB data + ~200-char partition key ≈ 51 KB
            // 210 records ≈ 10.5 MB total (exceeds the 10 MiB PutRecords request limit)
            val recordDataSize = 50 * 1_024 // 50 KB
            val recordCount = 210

            repeat(recordCount) { i ->
                val partitionKey = "k".repeat(200) + "-$i"
                val data = ByteArray(recordDataSize) { (i % 256).toByte() }
                val result = largeKinesis.record(
                    data = data,
                    partitionKey = partitionKey,
                    streamName = STREAM_NAME
                )
                result.shouldBeSuccess()
            }

            // First flush: sends up to 10 MiB worth of records
            val flush1 = largeKinesis.flush()
            flush1.shouldBeSuccess()
            val flushed1 = flush1.data.recordsFlushed
            flushed1 shouldBeGreaterThan 0

            // Second flush: sends the remaining records
            val flush2 = largeKinesis.flush()
            flush2.shouldBeSuccess()
            val flushed2 = flush2.data.recordsFlushed
            flushed2 shouldBeGreaterThan 0

            (flushed1 + flushed2) shouldBe recordCount

            // Third flush: nothing left
            val flush3 = largeKinesis.flush()
            flush3.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            largeKinesis.disable()
            largeKinesis.clearCache()
        }
    }

    /**
     * Attempts to record a single entry whose total size (partition key + data blob)
     * exceeds the 10 MiB per-record limit. The record call should fail, and a
     * subsequent flush of a valid record should still succeed — proving the client
     * is not left in a broken state.
     */
    @Test
    fun testOversizedRecordIsRejectedAndFlushStillWorks(): Unit = runBlocking {
        // 10 MiB = 10_485_760 bytes. Use a 256-char partition key (~256 bytes UTF-8)
        // plus a data blob that pushes the total over 10 MiB.
        val largePartitionKey = "k".repeat(256)
        val oversizedData = ByteArray(10_485_760) { 0x42 } // 10 MiB data + 256 bytes key > 10 MiB

        val oversizedResult = kinesis.record(
            data = oversizedData,
            partitionKey = largePartitionKey,
            streamName = STREAM_NAME
        )
        oversizedResult.shouldBeFailure().error
            .shouldBeInstanceOf<AmplifyKinesisValidationException>()

        // Now record a valid small record and flush — client should still work
        val validResult = kinesis.record(
            data = "still-works".toByteArray(),
            partitionKey = "partition-1",
            streamName = STREAM_NAME
        )
        validResult.shouldBeSuccess()

        val flushResult = kinesis.flush()
        flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
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
