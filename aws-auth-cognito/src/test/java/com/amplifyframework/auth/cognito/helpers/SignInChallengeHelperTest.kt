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
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.mockAuthNextSignInStep
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SignInChallengeHelperTest {
    private val username = "username"
    private val email = "test@testdomain.com"
    private val phoneNumber = "+15555555555"

    // MFA Setup
    @Test
    fun `User needs to select either Email OTP or TOTP to setup`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"EMAIL_OTP\",\"SOFTWARE_TOKEN_MFA\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION,
                    allowedMFATypes = setOf(MFAType.EMAIL, MFAType.TOTP)
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    @Test
    fun `User needs to setup TOTP`() {
        val totpSetupData = SignInTOTPSetupData(
            secretCode = "secretCode",
            session = "session",
            username = username
        )

        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"SOFTWARE_TOKEN_MFA\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = totpSetupData,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                    additionalInfo = mapOf("MFAS_CAN_SETUP" to "\"SOFTWARE_TOKEN_MFA\""),
                    totpSetupDetails = TOTPSetupDetails(
                        sharedSecret = totpSetupData.secretCode,
                        username = totpSetupData.username
                    )
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    @Test
    fun `User needs to setup email OTP`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"EMAIL_OTP\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // MFA Selection
    @Test
    fun `User needs to select which MFA type to use`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SELECT_MFA_TYPE",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_OTP\",\"SOFTWARE_TOKEN_MFA\",\"SMS_MFA\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                    allowedMFATypes = setOf(MFAType.EMAIL, MFAType.TOTP, MFAType.SMS)
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // Email OTP (User Auth) / MFA
    @Test
    fun `User is asked to confirm an email OTP or email MFA code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(email, AuthCodeDeliveryDetails.DeliveryMedium.EMAIL)

        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "EMAIL_OTP",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "email",
                    "CODE_DELIVERY_DESTINATION" to email
                )
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP,
                    authCodeDeliveryDetails = deliveryDetails
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // SMS OTP (User Auth)
    @Test
    fun `User is asked to confirm an SMS OTP code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(phoneNumber, AuthCodeDeliveryDetails.DeliveryMedium.SMS)

        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SMS_OTP",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                    "CODE_DELIVERY_DESTINATION" to phoneNumber
                )
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP,
                    authCodeDeliveryDetails = deliveryDetails
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // TOTP
    @Test
    fun `User is asked to input the TOTP code`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SOFTWARE_TOKEN_MFA",
                username = username,
                session = "session",
                parameters = null
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // SMS MFA
    @Test
    fun `User is asked to confirm an SMS MFA code`() {
        val deliveryDetails = AuthCodeDeliveryDetails(phoneNumber, AuthCodeDeliveryDetails.DeliveryMedium.SMS)

        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SMS_MFA",
                username = username,
                session = "session",
                parameters = mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                    "CODE_DELIVERY_DESTINATION" to phoneNumber
                )
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
                    authCodeDeliveryDetails = deliveryDetails
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // Custom Challenge
    @Test
    fun `User is asked to confirm a custom challenge`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "CUSTOM_CHALLENGE",
                username = username,
                session = "session",
                parameters = null
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // New Password Required
    @Test
    fun `User is asked to input a new password`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "NEW_PASSWORD_REQUIRED",
                username = username,
                session = "session",
                parameters = null
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // Select Challenge (User Auth)
    @Test
    fun `User is asked to select an available challenge`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        val availableFactors = listOf(AuthFactorType.EMAIL_OTP, AuthFactorType.SMS_OTP, AuthFactorType.WEB_AUTHN)
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "SELECT_CHALLENGE",
                username = username,
                session = "session",
                parameters = null,
                availableChallenges = availableFactors.map { it.challengeResponse }
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertEquals(
            AuthSignInResult(
                false,
                mockAuthNextSignInStep(
                    authSignInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION,
                    availableFactors = availableFactors.toSet()
                )
            ),
            signInResult
        )
        assertNull(errorResult)
    }

    // Exceptions
    @Test
    fun `Exception when setting up unknown challenge type`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_SETUP" to "\"UNKNOWN_CHALLENGE\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertIs<UnknownException>(errorResult)
        assertNull(signInResult)
    }

    @Test
    fun `Exception when choosing an unknown challenge type`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "MFA_SETUP",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_MFA\",\"UNKNOWN\",\"SMS_MFA\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertIs<UnknownException>(errorResult)
        assertNull(signInResult)
    }

    @Test
    fun `Exception when receiving an unsupported challenge name`() {
        var signInResult: AuthSignInResult? = null
        var errorResult: AuthException? = null
        SignInChallengeHelper.getNextStep(
            challenge = AuthChallenge(
                challengeName = "UNKNOWN",
                username = username,
                session = "session",
                parameters = mapOf("MFAS_CAN_CHOOSE" to "\"EMAIL_MFA\",\"UNKNOWN\",\"SMS_MFA\"")
            ),
            onSuccess = {
                signInResult = it
            },
            onError = {
                errorResult = it
            },
            signInTOTPSetupData = null,
            allowedMFAType = null
        )
        assertIs<UnknownException>(errorResult)
        assertNull(signInResult)
    }
}
