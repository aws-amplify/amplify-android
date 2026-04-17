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

import aws.sdk.kotlin.services.firehose.FirehoseClient
import aws.sdk.kotlin.services.firehose.model.PutRecordBatchRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import com.amplifyframework.foundation.useragent.AmplifyUserAgentInterceptor
import com.amplifyframework.kinesis.BuildConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyFirehoseClientUserAgentTest {

    @Test
    fun `user agent header contains firehose and amplify metadata`() = runTest {
        var capturedUserAgent: String? = null

        val firehoseClient = FirehoseClient {
            region = "us-east-1"
            credentialsProvider = object : CredentialsProvider {
                override suspend fun resolve(attributes: Attributes) = Credentials("fake-access-key", "fake-secret-key")
            }
            interceptors += AmplifyUserAgentInterceptor("amplify-firehose", BuildConfig.VERSION_NAME)
            interceptors += object : HttpInterceptor {
                override suspend fun modifyBeforeTransmit(
                    context: ProtocolRequestInterceptorContext<Any, HttpRequest>
                ): HttpRequest {
                    capturedUserAgent = context.protocolRequest.headers["User-Agent"]
                    // Throw to short-circuit the actual HTTP call
                    throw CapturedUserAgentException()
                }
            }
        }

        val request = PutRecordBatchRequest {
            deliveryStreamName = "test-stream"
            records = listOf(
                aws.sdk.kotlin.services.firehose.model.Record {
                    data = "test".toByteArray()
                }
            )
        }
        try {
            firehoseClient.putRecordBatch(request)
        } catch (_: Exception) {
            // Expected — interceptor throws to avoid network call
        }

        capturedUserAgent.shouldNotBeNull()
        capturedUserAgent shouldContain "lib/amplify-android#${BuildConfig.VERSION_NAME}"
        capturedUserAgent shouldContain "md/amplify-firehose#${BuildConfig.VERSION_NAME}"
    }

    private class CapturedUserAgentException : RuntimeException("User-Agent captured")
}
