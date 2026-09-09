/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.cloudwatch

import aws.sdk.kotlin.services.cloudwatchlogs.model.CloudWatchLogsException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class AmplifyCloudWatchExceptionTest {

    @Test
    fun `from returns the same instance for an AmplifyCloudWatchException`() {
        val original = AmplifyCloudWatchStorageException("boom", "fix it")

        AmplifyCloudWatchException.from(original) shouldBe original
    }

    @Test
    fun `from maps a CloudWatchLogsException to a service exception`() {
        val sdkError = mockk<CloudWatchLogsException> {
            every { message } returns "throttled"
        }

        val result = AmplifyCloudWatchException.from(sdkError)

        result.shouldBeInstanceOf<AmplifyCloudWatchServiceException>()
        result.message shouldBe "throttled"
        result.cause shouldBe sdkError
    }

    @Test
    fun `from maps any other error to an unknown exception`() {
        val error = IllegalStateException("unexpected")

        val result = AmplifyCloudWatchException.from(error)

        result.shouldBeInstanceOf<AmplifyCloudWatchUnknownException>()
        result.message shouldBe "unexpected"
        result.cause shouldBe error
    }
}
