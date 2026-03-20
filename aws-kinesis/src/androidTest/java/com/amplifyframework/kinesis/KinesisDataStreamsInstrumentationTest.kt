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
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Instrumented tests for [AmplifyKinesisClient].
 *
 * Inherits all shared stream-client tests from [BaseStreamClientInstrumentationTest]
 * and adds Kinesis-specific tests for partition key validation and large-payload batching.
 *
 * Requires:
 * - A provisioned Kinesis Data Stream
 * - Valid AWS credentials via Amplify Auth (Cognito)
 * - `amplify_outputs` and `credentials` raw resources
 */
class KinesisDataStreamsInstrumentationTest : BaseStreamClientInstrumentationTest() {

    override val streamName = "amplify-kinesis-test-stream"

    // 10 MiB per-record limit + 1 byte to exceed it
    override val oversizedRecordSize = 10 * 1_024 * 1_024 + 1

    override fun createDefaultClient(context: Context): TestableStreamClient =
        AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider
        ).asTestable()

    override fun createClientWithSmallCache(context: Context, cacheMaxBytes: Long): TestableStreamClient =
        AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions { this.cacheMaxBytes = cacheMaxBytes }
        ).asTestable()

    override fun createClientWithMaxRetries(context: Context, maxRetries: Int): TestableStreamClient =
        AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
                this.maxRetries = maxRetries
                flushStrategy = FlushStrategy.None
            }
        ).asTestable()

    override fun createClientWithAutoFlush(context: Context, intervalSeconds: Int): TestableStreamClient =
        AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
                flushStrategy = FlushStrategy.Interval(intervalSeconds.seconds)
            }
        ).asTestable()

    override fun createClientWithBadCredentials(context: Context): TestableStreamClient =
        AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = AwsCredentialsProvider {
                AwsCredentials.Static(
                    accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                    secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                )
            }
        ).asTestable()

    override fun assertLimitExceededError(result: Result<*, *>) {
        (result as Result.Failure).error.shouldBeInstanceOf<AmplifyKinesisLimitExceededException>()
    }

    override fun assertValidationError(result: Result<*, *>) {
        (result as Result.Failure).error.shouldBeInstanceOf<AmplifyKinesisValidationException>()
    }

    // ---------------------------------------------------------------
    // Kinesis-specific: partition key validation
    // ---------------------------------------------------------------

    /**
     * Record with a partition key containing exactly 256 emoji Unicode scalars
     * (the maximum allowed), then flush to verify the record is accepted by Kinesis.
     */
    @Test
    fun testRecordWithMax256EmojiCodePointsAndFlush(): Unit = runBlocking {
        val emoji = "\uD83D\uDE00" // 😀
        val emojiPartitionKey = emoji.repeat(256)

        val codePointCount = emojiPartitionKey.codePointCount(0, emojiPartitionKey.length)
        val utf8ByteCount = emojiPartitionKey.toByteArray(Charsets.UTF_8).size
        codePointCount shouldBe 256
        utf8ByteCount shouldBe 1024

        val context = ApplicationProvider.getApplicationContext<Context>()
        val kinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider
        )
        kinesis.enable()

        try {
            val result = kinesis.record(
                data = "test-data-with-emoji-partition-key".toByteArray(),
                partitionKey = emojiPartitionKey,
                streamName = streamName
            )
            result.shouldBeSuccess()

            val flushResult = kinesis.flush()
            flushResult.shouldBeSuccess().data.recordsFlushed shouldBe 1
        } finally {
            kinesis.disable()
            kinesis.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Kinesis-specific: PutRecords size limits with large partition keys
    // ---------------------------------------------------------------

    /**
     * Fills the cache with >5 MB of data using large partition keys, then flushes.
     * Exercises the PutRecords API limit of 10 MiB per request.
     */
    @Test
    fun testFlushLargePayloadWithLargePartitionKeys(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val largeKinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyKinesisClientOptions {
                cacheMaxBytes = 12L * 1_024 * 1_024
                flushStrategy = FlushStrategy.None
            }
        )
        largeKinesis.enable()

        try {
            val recordDataSize = 50 * 1_024 // 50 KB
            val recordCount = 210

            repeat(recordCount) { i ->
                val partitionKey = "k".repeat(200) + "-$i"
                val data = ByteArray(recordDataSize) { (i % 256).toByte() }
                val result = largeKinesis.record(
                    data = data,
                    partitionKey = partitionKey,
                    streamName = streamName
                )
                result.shouldBeSuccess()
            }

            val flush1 = largeKinesis.flush()
            flush1.shouldBeSuccess()
            val flushed1 = flush1.data.recordsFlushed
            flushed1 shouldBeGreaterThan 0

            val flush2 = largeKinesis.flush()
            flush2.shouldBeSuccess()
            val flushed2 = flush2.data.recordsFlushed
            flushed2 shouldBeGreaterThan 0

            (flushed1 + flushed2) shouldBe recordCount

            val flush3 = largeKinesis.flush()
            flush3.shouldBeSuccess().data.recordsFlushed shouldBe 0
        } finally {
            largeKinesis.disable()
            largeKinesis.clearCache()
        }
    }

    // ---------------------------------------------------------------
    // Escape hatch
    // ---------------------------------------------------------------

    @Test
    fun testGetKinesisClient() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val kinesis = AmplifyKinesisClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider
        )
        kinesis.kinesisClient.shouldNotBeNull()
    }
}
