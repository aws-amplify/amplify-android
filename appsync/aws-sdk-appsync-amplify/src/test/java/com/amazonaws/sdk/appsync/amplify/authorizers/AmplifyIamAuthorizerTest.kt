/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amazonaws.sdk.appsync.amplify.authorizers

import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.amplify.util.AppSyncRequestSigner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AmplifyIamAuthorizerTest {
    private val region = "us-east-1"

    @Test
    fun `iam authorizer gets token from amplify`() = runTest {
        val request = mockk<AppSyncRequest>()
        val signer = mockk<AppSyncRequestSigner> {
            coEvery {
                signAppSyncRequest(request, region)
            } returns mapOf("Authorization" to "test-signature")
        }

        val authorizer = AmplifyIamAuthorizer(region, signer)

        authorizer.getAuthorizationHeaders(request) shouldContainExactly mapOf("Authorization" to "test-signature")
    }

    @Test
    fun `iam authorizer throws if failed to fetch token from amplify`() = runTest {
        val request = mockk<AppSyncRequest>()
        val signer = mockk<AppSyncRequestSigner> {
            coEvery {
                signAppSyncRequest(request, region)
            } throws IllegalStateException()
        }

        val authorizer = AmplifyIamAuthorizer(region, signer)

        shouldThrow<IllegalStateException> {
            authorizer.getAuthorizationHeaders(request)
        }
    }
}
