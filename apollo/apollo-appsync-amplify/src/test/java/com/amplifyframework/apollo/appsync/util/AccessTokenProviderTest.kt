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

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccessTokenProviderTest {
    @Test
    fun `Retrieves access token`() = runTest {
        val credentialProvider = mockk<AuthCredentialsProvider> {
            every { getAccessToken(any(), any()) } answers {
                firstArg<Consumer<String>>().accept("testToken")
            }
        }
        val provider = AccessTokenProvider(credentialProvider)

        val token = provider.fetchLatestCognitoAuthToken()

        token shouldBe "testToken"
    }
}
