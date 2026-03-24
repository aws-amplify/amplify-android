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
package com.amplifyframework.firehose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.kinesis.BaseStreamClientInstrumentationTest
import com.amplifyframework.kinesis.TestableStreamClient
import com.amplifyframework.recordcache.FlushStrategy
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/**
 * Instrumented tests for [AmplifyFirehoseClient].
 *
 * Inherits all shared stream-client tests from [BaseStreamClientInstrumentationTest].
 * Firehose has no partition key concept, so no partition-key-specific tests are needed.
 *
 * Requires:
 * - A provisioned Firehose delivery stream
 * - Valid AWS credentials via Amplify Auth (Cognito)
 * - `amplify_outputs` and `credentials` raw resources
 */
class FirehoseInstrumentationTest : BaseStreamClientInstrumentationTest() {

    override val streamName = "amplify-firehose-test-stream"

    // 1,000 KiB per-record limit + 1 byte to exceed it
    override val oversizedRecordSize = 1_000 * 1_024 + 1

    override fun createDefaultClient(context: Context): TestableStreamClient = AmplifyFirehoseClient(
        context = context,
        region = REGION,
        credentialsProvider = credentialsProvider
    ).asTestable()

    override fun createClientWithSmallCache(context: Context, cacheMaxBytes: Long): TestableStreamClient =
        AmplifyFirehoseClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyFirehoseClientOptions { this.cacheMaxBytes = cacheMaxBytes }
        ).asTestable()

    override fun createClientWithMaxRetries(context: Context, maxRetries: Int): TestableStreamClient =
        AmplifyFirehoseClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyFirehoseClientOptions {
                this.maxRetries = maxRetries
                flushStrategy = FlushStrategy.None
            }
        ).asTestable()

    override fun createClientWithAutoFlush(context: Context, intervalSeconds: Int): TestableStreamClient =
        AmplifyFirehoseClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider,
            options = AmplifyFirehoseClientOptions {
                flushStrategy = FlushStrategy.Interval(intervalSeconds.seconds)
            }
        ).asTestable()

    override fun createClientWithBadCredentials(context: Context): TestableStreamClient = AmplifyFirehoseClient(
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
        (result as Result.Failure).error.shouldBeInstanceOf<AmplifyFirehoseLimitExceededException>()
    }

    override fun assertValidationError(result: Result<*, *>) {
        (result as Result.Failure).error.shouldBeInstanceOf<AmplifyFirehoseValidationException>()
    }

    // ---------------------------------------------------------------
    // Escape hatch
    // ---------------------------------------------------------------

    @Test
    fun testGetFirehoseClient() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firehose = AmplifyFirehoseClient(
            context = context,
            region = REGION,
            credentialsProvider = credentialsProvider
        )
        firehose.firehoseClient.shouldNotBeNull()
    }
}
