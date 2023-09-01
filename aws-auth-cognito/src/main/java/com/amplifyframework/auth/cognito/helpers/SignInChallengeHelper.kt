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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Consumer
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import java.util.Date
import kotlin.time.Duration.Companion.seconds

internal object SignInChallengeHelper {
    fun evaluateNextStep(
        username: String,
        challengeNameType: ChallengeNameType?,
        session: String?,
        challengeParameters: Map<String, String>?,
        authenticationResult: AuthenticationResultType?,
        signInMethod: SignInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
    ): StateMachineEvent {
        return when {
            authenticationResult != null -> {
                authenticationResult.let {
                    val userId = it.accessToken?.let { token -> SessionHelper.getUserSub(token) } ?: ""
                    val expiresIn = Instant.now().plus(it.expiresIn.seconds).epochSeconds
                    val tokens = CognitoUserPoolTokens(it.idToken, it.accessToken, it.refreshToken, expiresIn)
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
                challengeNameType is ChallengeNameType.SelectMfaType -> {
                val challenge =
                    AuthChallenge(challengeNameType.value, username, session, challengeParameters)
                SignInEvent(SignInEvent.EventType.ReceivedChallenge(challenge))
            }
            challengeNameType is ChallengeNameType.MfaSetup -> {
                val allowedMFASetupTypes = challengeParameters?.get("MFAS_CAN_SETUP")
                    ?.let { getAllowedMFATypes(it) } ?: emptySet()
                if (allowedMFASetupTypes.contains(MFAType.TOTP)) {
                    val setupTOTPData = SignInTOTPSetupData("", session, username)
                    SignInEvent(SignInEvent.EventType.InitiateTOTPSetup(setupTOTPData))
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
            else -> SignInEvent(SignInEvent.EventType.ThrowError(Exception("Response did not contain sign in info.")))
        }
    }

    fun getNextStep(
        challenge: AuthChallenge,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>,
        signInTOTPSetupData: SignInTOTPSetupData? = null,
        allowedMFAType: Set<MFAType>? = null
    ) {
        val challengeParams = challenge.parameters?.toMutableMap() ?: mapOf()

        when (ChallengeNameType.fromValue(challenge.challengeName)) {
            is ChallengeNameType.SmsMfa -> {
                val deliveryDetails = AuthCodeDeliveryDetails(
                    challengeParams.getValue("CODE_DELIVERY_DESTINATION"),
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                        challengeParams.getValue("CODE_DELIVERY_DELIVERY_MEDIUM")
                    )
                )
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
                        mapOf(),
                        deliveryDetails,
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
                        mapOf(),
                        null,
                        null,
                        null
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.MfaSetup -> {
                signInTOTPSetupData?.let {
                    val authSignInResult = AuthSignInResult(
                        false,
                        AuthNextSignInStep(
                            AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                            challengeParams,
                            null,
                            TOTPSetupDetails(it.secretCode, it.username),
                            allowedMFAType
                        )
                    )
                    onSuccess.accept(authSignInResult)
                } ?: onError.accept(UnknownException(cause = Exception("Challenge type not supported.")))
            }
            is ChallengeNameType.SelectMfaType -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(
                        AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                        mapOf(),
                        null,
                        null,
                        challengeParams["MFAS_CAN_CHOOSE"]?.let { getAllowedMFATypes(it) }
                    )
                )
                onSuccess.accept(authSignInResult)
            }
            else -> onError.accept(UnknownException(cause = Exception("Challenge type not supported.")))
        }
    }

    fun getAllowedMFATypes(allowedMFAType: String): Set<MFAType> {
        val result = mutableSetOf<MFAType>()
        allowedMFAType.replace(Regex("\\[|\\]|\""), "").split(",").forEach {
            when (it) {
                "SMS_MFA" -> result.add(MFAType.SMS)
                "SOFTWARE_TOKEN_MFA" -> result.add(MFAType.TOTP)
                else -> throw UnknownException(cause = Exception("MFA type not supported."))
            }
        }
        return result
    }
}
