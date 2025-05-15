/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.amplify.util

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.toHttpBody
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amplifyframework.auth.AuthCredentialsProvider
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldContain
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppSyncRequestSignerTest {
    @OptIn(InternalApi::class)
    @Test
    fun `signs request`() = runTest {
        val expectedUrl = "https://amazon.com"
        val expectedBody = "hello"
        val expectedHeaders = mapOf("k1" to "v1")
        val credentialProvider = mockk<AuthCredentialsProvider> {
            coEvery { resolve(any()) } returns mockk()
        }
        val slot = CapturingSlot<HttpRequest>()
        val signer = mockk<AwsSigner> {
            coEvery { sign(capture(slot), any()) } returns mockk {
                every { output.headers.entries() } returns mapOf("test" to listOf("value")).entries
            }
        }
        val request = object : AppSyncRequest {
            override val method = AppSyncRequest.HttpMethod.POST
            override val url = expectedUrl
            override val headers = expectedHeaders
            override val body = expectedBody
        }
        val requestSigner = AppSyncRequestSigner(credentialProvider, signer)

        val result = requestSigner.signAppSyncRequest(request, "us-east-1")
        val signedRequest = slot.captured
        signedRequest.url.toString() shouldBeEqual expectedUrl
        signedRequest.method shouldBeEqual HttpMethod.POST
        signedRequest.body shouldBeEqual expectedBody.toHttpBody()
        signedRequest.headers shouldBeEqual Headers { append("k1", "v1") }
        result shouldContain ("test" to "value")
    }
}
