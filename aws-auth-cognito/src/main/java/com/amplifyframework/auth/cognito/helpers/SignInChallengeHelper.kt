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
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Consumer
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

    fun getNextStep(
        challenge: AuthChallenge,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>,
        signInTOTPSetupData: SignInTOTPSetupData? = null,
        allowedMFAType: Set<MFAType>? = null
    ) {
        val challengeParams = challenge.parameters ?: emptyMap()

        when (challenge.challengeNameType) {
            is ChallengeNameType.SmsMfa,
            ChallengeNameType.EmailOtp,
            ChallengeNameType.SmsOtp -> {
                val deliveryDetails = AuthCodeDeliveryDetails(
                    challengeParams.getValue(ChallengeParameter.CodeDeliveryDestination.key),
                    DeliveryMedium.fromString(challengeParams.getValue(ChallengeParameter.CodeDeliveryMedium.key))
                )
                val signInStep = if (challenge.challengeNameType == ChallengeNameType.SmsMfa) {
                    AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE
                } else {
                    AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP
                }
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        signInStep,
                        mapOf(),
                        deliveryDetails,
                        null,
                        null,
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.NewPasswordRequired -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD,
                        challengeParams,
                        null,
                        null,
                        null,
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.CustomChallenge -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE,
                        challengeParams,
                        null,
                        null,
                        null,
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.SoftwareTokenMfa -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE,
                        emptyMap(),
                        null,
                        null,
                        null,
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.MfaSetup -> {
                val allowedMFASetupTypes = getAllowedMFASetupTypesFromChallengeParameters(challengeParams)

                if (allowedMFASetupTypes.contains(MFAType.TOTP) && allowedMFASetupTypes.contains(MFAType.EMAIL)) {
                    val authSignInResult = AuthSignInResult(
                        false,
                        AuthNextSignInStep(
                            AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION,
                            emptyMap(),
                            null,
                            null,
                            allowedMFASetupTypes,
                            null
                        )
                    )
                    onSuccess.accept(authSignInResult)
                } else if (allowedMFASetupTypes.contains(MFAType.TOTP) && signInTOTPSetupData != null) {
                    val authSignInResult = AuthSignInResult(
                        false,
                        AuthNextSignInStep(
                            AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                            challengeParams,
                            null,
                            TOTPSetupDetails(signInTOTPSetupData.secretCode, signInTOTPSetupData.username),
                            allowedMFAType,
                            null
                        )
                    )
                    onSuccess.accept(authSignInResult)
                } else if (allowedMFASetupTypes.contains(MFAType.EMAIL)) {
                    val authSignInResult = AuthSignInResult(
                        false,
                        AuthNextSignInStep(
                            AuthSignInStep.CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP,
                            emptyMap(),
                            null,
                            null,
                            allowedMFAType,
                            null
                        )
                    )
                    onSuccess.accept(authSignInResult)
                } else {
                    onError.accept(UnknownException(cause = Exception("Challenge type not supported.")))
                }
            }
            is ChallengeNameType.SelectMfaType -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                        mapOf(),
                        null,
                        null,
                        getAllowedMFATypesFromChallengeParameters(challengeParams),
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.SelectChallenge -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION,
                        mapOf(),
                        null,
                        null,
                        null,
                        getAvailableFactors(challenge.availableChallenges)
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            else -> onError.accept(UnknownException(cause = Exception("Challenge type not supported.")))
        }
    }

    private fun getAvailableFactors(possibleFactors: List<String>?): Set<AuthFactorType> {
        val result = mutableSetOf<AuthFactorType>()
        if (possibleFactors == null) {
            throw UnknownException(cause = Exception("Tried to parse available factors but found none."))
        } else {
            possibleFactors.forEach {
                try {
                    result.add(AuthFactorType.valueOf(it))
                } catch (exception: IllegalArgumentException) {
                    throw UnknownException(cause = Exception("Tried to parse an unrecognized AuthFactorType"))
                }
            }
        }
        return result
    }
}
