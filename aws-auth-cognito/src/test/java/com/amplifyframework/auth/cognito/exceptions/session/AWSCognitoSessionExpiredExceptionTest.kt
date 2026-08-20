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

package com.amplifyframework.auth.cognito.exceptions.session

import com.amplifyframework.auth.exceptions.SessionExpiredException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * The expected strings below are the messages Cognito actually returned for each scenario when
 * exercised against a device-tracking user pool.
 */
class AWSCognitoSessionExpiredExceptionTest {

    private fun classify(message: String?) = AWSCognitoSessionExpiredException.classify(message)

    @Test
    fun `classifies genuine expiry`() {
        classify("Refresh Token has expired") shouldBe RefreshFailureReason.Expired
    }

    @Test
    fun `classifies revocation`() {
        classify("Refresh Token has been revoked") shouldBe RefreshFailureReason.Revoked
    }

    @Test
    fun `classifies device binding failure by the trailing period`() {
        classify("Invalid Refresh Token.") shouldBe RefreshFailureReason.DeviceBindingFailed
    }

    @Test
    fun `classifies an unvalidatable token, which has no trailing period`() {
        classify("Invalid Refresh Token") shouldBe RefreshFailureReason.Invalid
    }

    @Test
    fun `classifies a disabled user`() {
        classify("User is disabled.") shouldBe RefreshFailureReason.UserDisabled
    }

    @Test
    fun `classifies a deleted user`() {
        classify("The user has been deleted for the associated refresh token") shouldBe
            RefreshFailureReason.UserDeleted
    }

    @Test
    fun `unrecognised and absent messages are Unknown`() {
        classify("Something new from the service") shouldBe RefreshFailureReason.Unknown
        classify(null) shouldBe RefreshFailureReason.Unknown
        classify("") shouldBe RefreshFailureReason.Unknown
    }

    @Test
    fun `retains the raw service message`() {
        val exception = AWSCognitoSessionExpiredException(
            reason = RefreshFailureReason.DeviceBindingFailed,
            serviceMessage = "Invalid Refresh Token.",
            cause = null
        )

        exception.serviceMessage shouldBe "Invalid Refresh Token."
        exception.reason shouldBe RefreshFailureReason.DeviceBindingFailed
    }

    @Test
    fun `remains catchable as SessionExpiredException so existing handling keeps working`() {
        val exception = AWSCognitoSessionExpiredException(
            reason = RefreshFailureReason.Expired,
            serviceMessage = "Refresh Token has expired",
            cause = null
        )

        exception.shouldBeInstanceOf<SessionExpiredException>()
        exception.message shouldBe "Your session has expired."
    }
}
