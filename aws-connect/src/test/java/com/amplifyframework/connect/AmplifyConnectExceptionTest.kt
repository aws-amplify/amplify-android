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
package com.amplifyframework.connect

import aws.sdk.kotlin.services.customerprofiles.model.AccessDeniedException
import aws.sdk.kotlin.services.customerprofiles.model.BadRequestException
import aws.sdk.kotlin.services.customerprofiles.model.InternalServerException
import aws.sdk.kotlin.services.customerprofiles.model.ResourceNotFoundException
import aws.sdk.kotlin.services.customerprofiles.model.ThrottlingException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyConnectExceptionTest {

    @Test
    fun `from maps AccessDeniedException`() {
        val sdkException = AccessDeniedException { message = "Forbidden" }

        val result = AmplifyConnectException.from(sdkException)

        result.shouldBeInstanceOf<ConnectAccessDeniedException>()
        result.message shouldBe "Forbidden"
    }

    @Test
    fun `from maps ThrottlingException to service exception`() {
        val sdkException = ThrottlingException { message = "Rate limit" }

        val result = AmplifyConnectException.from(sdkException)

        result.shouldBeInstanceOf<ConnectServiceException>()
        result.message shouldBe "Rate limit"
    }

    @Test
    fun `from maps ResourceNotFoundException to service exception`() {
        val sdkException = ResourceNotFoundException { message = "Not found" }

        val result = AmplifyConnectException.from(sdkException)

        result.shouldBeInstanceOf<ConnectServiceException>()
    }

    @Test
    fun `from maps BadRequestException to validation exception`() {
        val sdkException = BadRequestException { message = "Invalid input" }

        val result = AmplifyConnectException.from(sdkException)

        result.shouldBeInstanceOf<ConnectValidationException>()
        result.message shouldBe "Invalid input"
    }

    @Test
    fun `from maps InternalServerException to service exception`() {
        val sdkException = InternalServerException { message = "Server error" }

        val result = AmplifyConnectException.from(sdkException)

        result.shouldBeInstanceOf<ConnectServiceException>()
    }

    @Test
    fun `from maps IOException to network exception`() {
        val ioException = java.io.IOException("Connection reset")

        val result = AmplifyConnectException.from(ioException)

        result.shouldBeInstanceOf<ConnectNetworkException>()
        result.message shouldBe "Connection reset"
    }

    @Test
    fun `from maps unknown exception to unknown`() {
        val unknownException = IllegalStateException("Something unexpected")

        val result = AmplifyConnectException.from(unknownException)

        result.shouldBeInstanceOf<ConnectUnknownException>()
        result.message shouldBe "Something unexpected"
    }

    @Test
    fun `from returns same exception if already AmplifyConnectException`() {
        val connectException = ConnectServiceException(
            message = "Already wrapped",
            recoverySuggestion = "test"
        )

        val result = AmplifyConnectException.from(connectException)

        result shouldBe connectException
    }
}
