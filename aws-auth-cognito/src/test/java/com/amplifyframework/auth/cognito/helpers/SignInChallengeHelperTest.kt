/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.mockAuthNextSignInStep
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SignInChallengeHelperTest {
    private val username = "username"
    private val email = "test@testdomain.com"
    private val phoneNumber = "+15555555555"

    // MFA Setup
    @Test
    fun `User needs to select either Email OTP or TOTP to setup`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"EMAIL_OTP\",\"SOFTWARE_TOKEN_MFA\"")
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION,
                allowedMFATypes = setOf(MFAType.EMAIL, MFAType.TOTP)
            )
        )
    }

    @Test
    fun `User needs to setup email OTP`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"EMAIL_OTP\"")
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP
            )
        )
    }

    // MFA Selection
    @Test
    fun `User needs to select which MFA type to use`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SELECT_MFA_TYPE",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_OTP\",\"SOFTWARE_TOKEN_MFA\",\"SMS_MFA\"")
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                allowedMFATypes = setOf(MFAType.EMAIL, MFAType.TOTP, MFAType.SMS)
            )
        )
    }

    // Email OTP (User Auth) / MFA
    @Test
    fun `User is asked to confirm an email OTP or email MFA code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(email, AuthCodeDeliveryDetails.DeliveryMedium.EMAIL)

        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "EMAIL_OTP",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "email",
                    "CODE_DELIVERY_DESTINATION" to email
                )
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP,
                authCodeDeliveryDetails = deliveryDetails
            )
        )
    }

    // SMS OTP (User Auth)
    @Test
    fun `User is asked to confirm an SMS OTP code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(phoneNumber, AuthCodeDeliveryDetails.DeliveryMedium.SMS)

        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SMS_OTP",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                    "CODE_DELIVERY_DESTINATION" to phoneNumber
                )
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP,
                authCodeDeliveryDetails = deliveryDetails
            )
        )
    }

    // TOTP
    @Test
    fun `User is asked to input the TOTP code`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SOFTWARE_TOKEN_MFA",
                username = username,
                session = "session",
                parameters = null
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE
            )
        )
    }

    // SMS MFA
    @Test
    fun `User is asked to confirm an SMS MFA code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(phoneNumber, AuthCodeDeliveryDetails.DeliveryMedium.SMS)

        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SMS_MFA",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                    "CODE_DELIVERY_DESTINATION" to phoneNumber
                )
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
                authCodeDeliveryDetails = deliveryDetails
            )
        )
    }

    // Custom Challenge
    @Test
    fun `User is asked to confirm a custom challenge`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "CUSTOM_CHALLENGE",
                username = username,
                session = "session",
                parameters = null
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE
            )
        )
    }

    // New Password Required
    @Test
    fun `User is asked to input a new password`() {
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "NEW_PASSWORD_REQUIRED",
                username = username,
                session = "session",
                parameters = null
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD
            )
        )
    }

    // Select Challenge (User Auth)
    @Test
    fun `User is asked to select an available challenge`() {
        val availableFactors = listOf(AuthFactorType.EMAIL_OTP, AuthFactorType.SMS_OTP, AuthFactorType.WEB_AUTHN)
        val signInResult = SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SELECT_CHALLENGE",
                username = username,
                session = "session",
                parameters = null,
                availableChallenges = availableFactors.map { it.challengeResponse }
            )
        )
        signInResult shouldBe AuthSignInResult(
            false,
            mockAuthNextSignInStep(
                authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION,
                availableFactors = availableFactors.toSet()
            )
        )
    }

    // Exceptions
    @Test
    fun `Exception when setting up unknown challenge type`() {
        shouldThrow<UnknownException> {
            SignInChallengeHelper.getNextStep(
                challenge = AuthChallenge(
                    challengeName = "MFA_SETUP",
                    username = username,
                    session = "session",
                    parameters = mapOf("MFAS_CAN_SETUP" to "\"UNKNOWN_CHALLENGE\"")
                )
            )
        }
    }

    @Test
    fun `Exception when choosing an unknown challenge type`() {
        shouldThrow<UnknownException> {
            SignInChallengeHelper.getNextStep(
                challenge = AuthChallenge(
                    challengeName = "MFA_SETUP",
                    username = username,
                    session = "session",
                    parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_MFA\",\"UNKNOWN\",\"SMS_MFA\"")
                )
            )
        }
    }

    @Test
    fun `Exception when receiving an unsupported challenge name`() {
        shouldThrow<UnknownException> {
            SignInChallengeHelper.getNextStep(
                challenge = AuthChallenge(
                    challengeName = "UNKNOWN",
                    username = username,
                    session = "session",
                    parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_MFA\",\"UNKNOWN\",\"SMS_MFA\"")
                )
            )
        }
    }
}
