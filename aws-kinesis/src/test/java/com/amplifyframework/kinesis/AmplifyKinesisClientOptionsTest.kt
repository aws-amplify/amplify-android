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
import com.amplifyframework.recordcache.FlushStrategy
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyKinesisClientOptionsTest {

    private val fakeCredentials = AwsCredentialsProvider {
        AwsCredentials.Static("FAKE_KEY", "FAKE_SECRET")
    }

    @Test
    fun `configureClient DSL configures SDK client`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val client = AmplifyKinesisClient(
            context = context,
            region = "us-east-1",
            credentialsProvider = fakeCredentials,
            options = AmplifyKinesisClientOptions {
                maxRetries = 3
                flushStrategy = FlushStrategy.None
                configureClient {
                    retryStrategy {
                        maxAttempts = 10
                    }
                }
            }
        )

        client.options.maxRetries shouldBe 3
        client.options.configureClient.shouldNotBeNull()
        client.kinesisClient.shouldNotBeNull()
        client.kinesisClient.config.retryStrategy.config.maxAttempts shouldBe 10
    }
}
