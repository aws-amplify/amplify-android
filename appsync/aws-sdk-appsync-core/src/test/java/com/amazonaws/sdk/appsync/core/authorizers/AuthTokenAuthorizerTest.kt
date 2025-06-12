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
package com.amazonaws.sdk.appsync.core.authorizers

import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.core.HeaderKeys
import io.kotest.matchers.maps.shouldContainAll
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthTokenAuthorizerTest {
    private val request = object : AppSyncRequest {
        override val method = AppSyncRequest.HttpMethod.POST
        override val url = "url"
        override val headers = emptyMap<String, String>()
        override val body = null
    }

    @Test
    fun `returns authorization header for requests`() = runTest {
        val delegate: () -> String = { "test" }

        val authorizer = AuthTokenAuthorizer(delegate)
        val result = authorizer.getAuthorizationHeaders(request)

        result shouldContainAll mapOf(HeaderKeys.AUTHORIZATION to "test")
    }
}
