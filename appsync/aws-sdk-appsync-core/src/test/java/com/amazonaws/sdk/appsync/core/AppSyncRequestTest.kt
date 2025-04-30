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
package com.amazonaws.sdk.appsync.core

import io.kotest.matchers.shouldBe
import org.junit.Test

class AppSyncRequestTest {

    @Test
    fun `test request implementation`() {
        val testRequest = object : AppSyncRequest {
            override val method = AppSyncRequest.HttpMethod.POST
            override val url = "https://amazon.com"
            override val headers = mapOf(
                HeaderKeys.API_KEY to "123",
                HeaderKeys.AUTHORIZATION to "345",
                HeaderKeys.AMAZON_DATE to "2025"
            )
            override val body = "b"
        }

        testRequest.method shouldBe AppSyncRequest.HttpMethod.POST
        testRequest.url shouldBe "https://amazon.com"
        testRequest.headers shouldBe mapOf(
            HeaderKeys.API_KEY to "123",
            HeaderKeys.AUTHORIZATION to "345",
            HeaderKeys.AMAZON_DATE to "2025"
        )
        testRequest.body shouldBe "b"
    }
}
