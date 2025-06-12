/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync.util

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import com.amplifyframework.auth.AuthCredentialsProvider
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import io.kotest.matchers.maps.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApolloRequestSignerTest {
    @OptIn(InternalApi::class)
    @Test
    fun `signs request`() = runTest {
        val credentialProvider = mockk<AuthCredentialsProvider> {
            coEvery { resolve(any()) } returns mockk()
        }
        val signer = mockk<AwsSigner> {
            coEvery { sign(any(), any()) } returns mockk {
                every { output.headers.entries() } returns mapOf("test" to listOf("value")).entries
            }
        }
        val request = HttpRequest.Builder(HttpMethod.Post, "http://example.com").build()

        val requestSigner = ApolloRequestSigner(credentialProvider, signer)
        val result = requestSigner.signAppSyncRequest(request, "us-east-1")
        result shouldContain ("test" to "value")
    }
}
