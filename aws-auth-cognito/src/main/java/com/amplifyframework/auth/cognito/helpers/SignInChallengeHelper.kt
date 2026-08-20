/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.app.Activity
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthCodeDeliveryDetails.DeliveryMedium
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.helpers.UserPoolSignInHelper.signInResult
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.ChallengeParameter
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.data.challengeNameType
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import java.lang.ref.WeakReference
import java.util.Date
import kotlin.time.Duration.Companion.seconds

internal object SignInChallengeHelper {
    fun evaluateNextStep(
        username: String,
        challengeNameType: ChallengeNameType?,
        session: String?,
        challengeParameters: Map<String, String>? = null,
        availableChallenges: List<String>? = null,
        authenticationResult: AuthenticationResultType?,
        callingActivity: WeakReference<Activity> = WeakReference(null),
        signInMethod: SignInMethod
    ): StateMachineEvent = when {
        authenticationResult != null -> {
            authenticationResult.let {
                val expiration = Instant.now().plus(it.expiresIn.seconds).epochSeconds
                val tokens = CognitoUserPoolTokens(it.idToken, it.accessToken, it.refreshToken, expiration)
                val userId = tokens.accessToken?.userSub ?: ""
                val signedInData = SignedInData(
                    userId,
                    username,
                    Date(),
                    signInMethod,
                    tokens
                )
                it.newDeviceMetadata?.let { metadata ->
                    SignInEvent(
                        SignInEvent.EventType.ConfirmDevice(
                            DeviceMetadata.Metadata(
                                metadata.deviceKey ?: "",
                                metadata.deviceGroupKey ?: ""
                            ),
                            signedInData
                        )
                    )
                } ?: AuthenticationEvent(
                    AuthenticationEvent.EventType.SignInCompleted(
                        signedInData,
                        DeviceMetadata.Empty
                    )
                )
            }
        }
        challengeNameType is ChallengeNameType.SmsMfa ||
            challengeNameType is ChallengeNameType.CustomChallenge ||
            challengeNameType is ChallengeNameType.NewPasswordRequired ||
            challengeNameType is ChallengeNameType.SoftwareTokenMfa ||
            challengeNameType is ChallengeNameType.SelectMfaType ||
            challengeNameType is ChallengeNameType.SmsOtp ||
            challengeNameType is ChallengeNameType.EmailOtp -> {
            val challenge =
                AuthChallenge(challengeNameType.value, username, session, challengeParameters)
            SignInEvent(SignInEvent.EventType.ReceivedChallenge(challenge, signInMethod))
        }
        challengeNameType is ChallengeNameType.MfaSetup -> {
            val allowedMFASetupTypes = getAllowedMFASetupTypesFromChallengeParameters(challengeParameters)
            val challenge = AuthChallenge(challengeNameType.value, username, session, challengeParameters)

            if (allowedMFASetupTypes.contains(MFAType.EMAIL)) {
                SignInEvent(SignInEvent.EventType.ReceivedChallenge(challenge, signInMethod))
            } else if (allowedMFASetupTypes.contains(MFAType.TOTP)) {
                val setupTOTPData = SignInTOTPSetupData("", session, username)
                SignInEvent(SignInEvent.EventType.InitiateTOTPSetup(setupTOTPData, challenge.parameters, signInMethod))
            } else {
                SignInEvent(
                    SignInEvent.EventType.ThrowError(
                        Exception("Cannot initiate MFA setup from available Types: $allowedMFASetupTypes")
                    )
                )
            }
        }
        challengeNameType is ChallengeNameType.DeviceSrpAuth -> {
            SignInEvent(SignInEvent.EventType.InitiateSignInWithDeviceSRP(username, mapOf()))
        }
        challengeNameType is ChallengeNameType.SelectChallenge -> {
            SignInEvent(
                SignInEvent.EventType.ReceivedChallenge(
                    AuthChallenge(
                        challengeName = ChallengeNameType.SelectChallenge.value,
                        username = username,
                        session = session,
                        parameters = null,
                        availableChallenges = availableChallenges
                    ),
                    signInMethod
                )
            )
        }
        challengeNameType is ChallengeNameType.WebAuthn -> {
            val requestOptions = challengeParameters?.get(ChallengeParameter.CredentialRequestOptions.key)
            val signInContext = WebAuthnSignInContext(
                username = username,
                callingActivity = callingActivity,
                session = session,
                requestJson = requestOptions
            )
            SignInEvent(SignInEvent.EventType.InitiateWebAuthnSignIn(signInContext))
        }
        else -> SignInEvent(SignInEvent.EventType.ThrowError(Exception("Response did not contain sign in info.")))
    }

    fun getNextStep(challenge: AuthChallenge): AuthSignInResult {
        val challengeParams = challenge.parameters ?: emptyMap()
        return when (challenge.challengeNameType) {
            ChallengeNameType.SmsMfa -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
                codeDeliveryDetails = AuthCodeDeliveryDetails(
                    challengeParams.getValue(ChallengeParameter.CodeDeliveryDestination.key),
                    DeliveryMedium.fromString(challengeParams.getValue(ChallengeParameter.CodeDeliveryMedium.key))
                )
            )
            ChallengeNameType.EmailOtp, ChallengeNameType.SmsOtp -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP,
                codeDeliveryDetails = AuthCodeDeliveryDetails(
                    challengeParams.getValue(ChallengeParameter.CodeDeliveryDestination.key),
                    DeliveryMedium.fromString(challengeParams.getValue(ChallengeParameter.CodeDeliveryMedium.key))
                )
            )
            ChallengeNameType.NewPasswordRequired -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD
            )
            ChallengeNameType.CustomChallenge -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE,
                additionalInfo = challengeParams
            )
            ChallengeNameType.SoftwareTokenMfa -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE
            )
            ChallengeNameType.MfaSetup -> {
                val allowedMFASetupTypes = getAllowedMFASetupTypesFromChallengeParameters(challengeParams)
                if (allowedMFASetupTypes.contains(MFAType.TOTP) && allowedMFASetupTypes.contains(MFAType.EMAIL)) {
                    signInResult(
                        signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION,
                        allowedMFATypes = allowedMFASetupTypes
                    )
                } else if (allowedMFASetupTypes.contains(MFAType.EMAIL)) {
                    signInResult(signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP)
                } else {
                    unknownException("Unsupported MFA type")
                }
            }
            ChallengeNameType.SelectMfaType -> signInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                allowedMFATypes = getAllowedMFATypesFromChallengeParameters(challengeParams)
            )
            ChallengeNameType.SelectChallenge -> signInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION,
                availableFactors = getAvailableFactors(challenge.availableChallenges)
            )
            ChallengeNameType.Password, ChallengeNameType.PasswordSrp -> signInResult(
                signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_PASSWORD
            )
            else -> unknownException("Challenge type not supported.")
        }
    }

    private fun unknownException(message: String): Nothing = throw UnknownException(message = message)

    private fun getAvailableFactors(possibleFactors: List<String>?): Set<AuthFactorType> {
        val result = mutableSetOf<AuthFactorType>()
        if (possibleFactors == null) {
            unknownException("Tried to parse available factors but found none.")
        } else {
            possibleFactors.forEach {
                try {
                    result.add(AuthFactorType.valueOf(it))
                } catch (exception: IllegalArgumentException) {
                    unknownException("Tried to parse an unrecognized AuthFactorType")
                }
            }
        }
        return result
    }
}
