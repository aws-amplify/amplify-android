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

import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyKinesisClientUserAgentTest {

    @Test
    fun `user agent header contains kinesis and amplify metadata`() = runTest {
        var capturedUserAgent: String? = null

        // Build a KinesisClient the same way AmplifyKinesisClient does:
        // KinesisUserAgentInterceptor first, then a capturing interceptor after it.
        val kinesisClient = KinesisClient {
            region = "us-east-1"
            interceptors += KinesisUserAgentInterceptor()
            interceptors += object : HttpInterceptor {
                override suspend fun modifyBeforeTransmit(
                    context: ProtocolRequestInterceptorContext<Any, HttpRequest>
                ): HttpRequest {
                    capturedUserAgent = context.protocolRequest.headers["User-Agent"]
                    return context.protocolRequest
                }
            }
        }

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
            kinesisClient.putRecords(request)
        } catch (_: Exception) {
            // Expected — no real credentials
        }

        capturedUserAgent.shouldNotBeNull()
        capturedUserAgent shouldContain "lib/amplify-android#${BuildConfig.VERSION_NAME}"
        capturedUserAgent shouldContain "md/kinesis#${BuildConfig.VERSION_NAME}"
    }
}
