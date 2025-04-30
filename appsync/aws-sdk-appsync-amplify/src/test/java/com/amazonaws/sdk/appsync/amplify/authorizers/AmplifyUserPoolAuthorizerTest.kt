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

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AmplifyUserPoolAuthorizerTest {

    @Test
    fun `user pool authorizer gets token from amplify`() = runTest {
        val expectedValue = "test-signature"
        val slot = CapturingSlot<Consumer<String>>()
        val cognitoCredentialsProvider = mockk<AuthCredentialsProvider> {
            every { getAccessToken(capture(slot), any()) } answers {
                slot.captured.accept(expectedValue)
            }
        }
        val accessTokenProvider = AccessTokenProvider(cognitoCredentialsProvider)
        val authorizer = AmplifyUserPoolAuthorizer(accessTokenProvider)

        authorizer.getAuthorizationHeaders(mockk()) shouldContainExactly mapOf("Authorization" to expectedValue)
    }

    @Test
    fun `user pool authorizer throws if failed to fetch token from amplify`() = runTest {
        val cognitoCredentialsProvider = mockk<AuthCredentialsProvider> {
            every { getAccessToken(any(), any()) } throws IllegalStateException()
        }
        val accessTokenProvider = AccessTokenProvider(cognitoCredentialsProvider)
        val authorizer = AmplifyUserPoolAuthorizer(accessTokenProvider)

        shouldThrow<IllegalStateException> {
            authorizer.getAuthorizationHeaders(mockk())
        }
    }
}
