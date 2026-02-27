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
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.recordcache.FlushStrategy
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyKinesisClientUserAgentTest {

    private val fakeCredentials = AwsCredentialsProvider {
        AwsCredentials.Static(
            accessKeyId = "FAKE_ACCESS_KEY",
            secretAccessKey = "FAKE_SECRET_KEY"
        )
    }

    @Test
    fun `user agent header contains kinesis metadata`() = runTest {
        var capturedUserAgent: String? = null

        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = AmplifyKinesisClient(
            context = context,
            region = "us-east-1",
            credentialsProvider = fakeCredentials,
            options = AmplifyKinesisClientOptions {
                flushStrategy = FlushStrategy.None
                configureClient {
                    interceptors += object : HttpInterceptor {
                        override suspend fun readBeforeTransmit(
                            context: ProtocolRequestInterceptorContext<Any, HttpRequest>
                        ) {
                            capturedUserAgent = context.protocolRequest.headers["User-Agent"]
                        }
                    }
                }
            }
        )

        // Call putRecords directly on the SDK client to trigger the interceptor chain.
        // The request will fail (fake credentials), but the interceptor fires before transmission.
        val request = aws.sdk.kotlin.services.kinesis.model.PutRecordsRequest {
            streamName = "test-stream"
            records = listOf(
                aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry {
                    data = "test".toByteArray()
                    partitionKey = "key"
                }
            )
        }
        try {
            client.kinesisClient.putRecords(request)
        } catch (_: Exception) {
            // Expected — fake credentials will cause a failure
        }

        capturedUserAgent shouldContain "lib/amplify-android#${BuildConfig.VERSION_NAME}"
        capturedUserAgent shouldContain "md/kinesis#${BuildConfig.VERSION_NAME}"
    }
}
