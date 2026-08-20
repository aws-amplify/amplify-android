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

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.helpers.getMFASetupTypeOrNull
import com.amplifyframework.auth.cognito.helpers.getMFATypeOrNull
import com.amplifyframework.auth.cognito.helpers.isMfaSetupSelectionChallenge
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.requireAuthenticationState
import com.amplifyframework.auth.cognito.util.sendEventAndGetSignInResult
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.data.challengeNameType
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.WebAuthnSignInState
import java.lang.ref.WeakReference

internal class ConfirmSignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val hubEmitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        challengeResponse: String,
        options: AuthConfirmSignInOptions = AuthConfirmSignInOptions.defaults()
    ): AuthSignInResult {
        val signInState = checkCanSubmitChallengeResponse()
        val event = createStateMachineEvent(
            signInState = signInState,
            challengeResponse = challengeResponse,
            options = options
        )

        val result = stateMachine.sendEventAndGetSignInResult(event)

        if (result.isSignedIn) {
            hubEmitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
        }

        return result
    }

    private suspend fun checkCanSubmitChallengeResponse(): SignInState {
        val currentState = stateMachine.requireAuthenticationState<AuthenticationState.SigningIn>()
        return when (val signInState = currentState.signInState) {
            is SignInState.ResolvingChallenge -> when (signInState.challengeState) {
                is SignInChallengeState.WaitingForAnswer, is SignInChallengeState.Error -> signInState
                else -> throw InvalidStateException()
            }
            is SignInState.ResolvingTOTPSetup -> when (signInState.setupTOTPState) {
                is SetupTOTPState.WaitingForAnswer, is SetupTOTPState.Error -> signInState
                else -> throw InvalidStateException()
            }
            is SignInState.SigningInWithWebAuthn -> when (signInState.webAuthnSignInState) {
                is WebAuthnSignInState.Error -> signInState
                else -> throw InvalidStateException()
            }
            else -> throw InvalidStateException()
        }
    }

    private fun createStateMachineEvent(
        signInState: SignInState,
        challengeResponse: String,
        options: AuthConfirmSignInOptions
    ): StateMachineEvent {
        val cognitoOptions = options as? AWSCognitoAuthConfirmSignInOptions
        val metadata = cognitoOptions?.metadata ?: emptyMap()
        val userAttributes = cognitoOptions?.userAttributes ?: emptyList()

        return when (signInState) {
            is SignInState.ResolvingChallenge -> {
                val challengeState = signInState.challengeState

                if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.SelectMfaType &&
                    getMFATypeOrNull(challengeResponse) == null
                ) {
                    throw InvalidParameterException(
                        message = "Value for challengeResponse must be one of SMS_MFA, EMAIL_OTP or SOFTWARE_TOKEN_MFA"
                    )
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    isMfaSetupSelectionChallenge(challengeState.challenge) &&
                    getMFASetupTypeOrNull(challengeResponse) == null
                ) {
                    throw InvalidParameterException(
                        message = "Value for challengeResponse must be one of EMAIL_OTP or SOFTWARE_TOKEN_MFA"
                    )
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                    challengeResponse == AuthFactorType.WEB_AUTHN.challengeResponse
                ) {
                    val username = challengeState.challenge.username!!
                    val session = challengeState.challenge.session
                    val signInContext = WebAuthnSignInContext(
                        username = username,
                        callingActivity = cognitoOptions?.callingActivity ?: WeakReference(null),
                        session = session
                    )
                    SignInEvent(SignInEvent.EventType.InitiateWebAuthnSignIn(signInContext))
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                    challengeResponse == ChallengeNameType.Password.value
                ) {
                    SignInEvent(
                        SignInEvent.EventType.ReceivedChallenge(
                            AuthChallenge(
                                challengeName = ChallengeNameType.Password.value,
                                username = challengeState.challenge.username,
                                session = challengeState.challenge.session,
                                parameters = challengeState.challenge.parameters
                            ),
                            signInMethod = challengeState.signInMethod
                        )
                    )
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                    challengeResponse == ChallengeNameType.PasswordSrp.value
                ) {
                    SignInEvent(
                        SignInEvent.EventType.ReceivedChallenge(
                            AuthChallenge(
                                challengeName = ChallengeNameType.PasswordSrp.value,
                                username = challengeState.challenge.username,
                                session = challengeState.challenge.session,
                                parameters = challengeState.challenge.parameters
                            ),
                            signInMethod = challengeState.signInMethod
                        )
                    )
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.Password
                ) {
                    SignInEvent(
                        SignInEvent.EventType.InitiateMigrateAuth(
                            username = challengeState.challenge.username!!,
                            password = challengeResponse,
                            metadata = metadata,
                            authFlowType = AuthFlowType.USER_AUTH,
                            respondToAuthChallenge = AuthChallenge(
                                challengeName = ChallengeNameType.SelectChallenge.value,
                                username = challengeState.challenge.username,
                                session = challengeState.challenge.session!!,
                                parameters = null
                            )
                        )
                    )
                } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                    challengeState.challenge.challengeNameType == ChallengeNameType.PasswordSrp
                ) {
                    SignInEvent(
                        SignInEvent.EventType.InitiateSignInWithSRP(
                            username = challengeState.challenge.username!!,
                            password = challengeResponse,
                            metadata = metadata,
                            authFlowType = AuthFlowType.USER_AUTH,
                            respondToAuthChallenge = AuthChallenge(
                                challengeName = ChallengeNameType.SelectChallenge.value,
                                username = challengeState.challenge.username,
                                session = challengeState.challenge.session!!,
                                parameters = null
                            )
                        )
                    )
                } else {
                    SignInChallengeEvent(
                        SignInChallengeEvent.EventType.VerifyChallengeAnswer(
                            challengeResponse,
                            metadata,
                            userAttributes
                        )
                    )
                }
            }
            is SignInState.ResolvingTOTPSetup -> {
                when (val totpState = signInState.setupTOTPState) {
                    is SetupTOTPState.WaitingForAnswer -> SetupTOTPEvent(
                        SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                            challengeResponse,
                            totpState.signInTOTPSetupData.username,
                            totpState.signInTOTPSetupData.session,
                            cognitoOptions?.friendlyDeviceName,
                            totpState.signInMethod
                        )
                    )
                    is SetupTOTPState.Error -> SetupTOTPEvent(
                        SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                            challengeResponse,
                            totpState.username,
                            totpState.session,
                            cognitoOptions?.friendlyDeviceName,
                            totpState.signInMethod
                        )
                    )
                    else -> throw InvalidStateException()
                }
            }
            is SignInState.SigningInWithWebAuthn -> {
                val webAuthnState = signInState.webAuthnSignInState
                if (webAuthnState is WebAuthnSignInState.Error &&
                    challengeResponse == AuthFactorType.WEB_AUTHN.challengeResponse
                ) {
                    SignInEvent(SignInEvent.EventType.InitiateWebAuthnSignIn(webAuthnState.context))
                } else {
                    throw InvalidStateException()
                }
            }
            else -> throw InvalidStateException()
        }
    }
}
