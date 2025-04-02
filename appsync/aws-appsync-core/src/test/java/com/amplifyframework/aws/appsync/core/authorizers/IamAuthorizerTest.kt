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
import io.kotest.matchers.maps.shouldContainAll
import kotlinx.coroutines.test.runTest
import org.junit.Test

class IamAuthorizerTest {

    private val request = object : AppSyncRequest {
        override val method = AppSyncRequest.HttpMethod.POST
        override val url = "https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/event"
        override val headers = emptyMap<String, String>()
        override val body = null
    }

    private val delegate: (
        AppSyncRequest
    ) -> Map<String, String> = { mapOf("header1" to "header1Value", "header2" to "header2Value") }

    @Test
    fun `returns authorization header for requests`() = runTest {
        val authorizer = IamAuthorizer(delegate)
        val result = authorizer.getAuthorizationHeaders(request)
        result shouldContainAll mapOf("header1" to "header1Value", "header2" to "header2Value")
    }
}
