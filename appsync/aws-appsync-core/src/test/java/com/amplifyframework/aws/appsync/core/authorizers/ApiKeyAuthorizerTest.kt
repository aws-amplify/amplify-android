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
package com.amplifyframework.aws.appsync.core.authorizers

import com.amplifyframework.aws.appsync.core.AppSyncRequest
import com.amplifyframework.aws.appsync.core.HeaderKeys
import com.amplifyframework.aws.appsync.core.util.Iso8601Timestamp
import io.kotest.matchers.maps.shouldContainAll
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApiKeyAuthorizerTest {

    private val request = object : AppSyncRequest {
        override val method = AppSyncRequest.HttpMethod.POST
        override val url = "url"
        override val headers = emptyMap<String, String>()
        override val body = null
    }

    @Test
    fun `returns static apiKey for headers`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer(apiKey)
            val result = authorizer.getAuthorizationHeaders(request)

            result shouldContainAll mapOf(
                HeaderKeys.API_KEY to apiKey,
                HeaderKeys.AMAZON_DATE to "now"
            )
        }
    }

    @Test
    fun `returns provided apiKey for headers`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer { apiKey }
            val result = authorizer.getAuthorizationHeaders(request)

            result shouldContainAll mapOf(
                HeaderKeys.API_KEY to apiKey,
                HeaderKeys.AMAZON_DATE to "now"
            )
        }
    }
}