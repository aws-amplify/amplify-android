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
package com.amplifyframework.aws.appsync.events.data

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Test

class PublishResultTest {

    @Test
    fun `status should be Successful when only successful events exist`() {
        val response = PublishResult.Response(
            successfulEvents = listOf(
                mockk()
            ),
            failedEvents = emptyList()
        )

        response.status shouldBe PublishResult.Response.Status.Successful
    }

    @Test
    fun `status should be Failed when only failed events exist`() {
        val response = PublishResult.Response(
            successfulEvents = emptyList(),
            failedEvents = listOf(
                mockk()
            )
        )

        response.status shouldBe PublishResult.Response.Status.Failed
    }

    @Test
    fun `status should be PartialSuccess when both successful and failed events exist`() {
        val response = PublishResult.Response(
            successfulEvents = listOf(
                mockk()
            ),
            failedEvents = listOf(
                mockk()
            )
        )

        response.status shouldBe PublishResult.Response.Status.PartialSuccess
    }
}
