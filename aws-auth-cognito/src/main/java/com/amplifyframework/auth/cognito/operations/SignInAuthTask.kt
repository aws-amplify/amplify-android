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

package com.amplifyframework.auth.cognito.operations

import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState

internal class SignInRequest(
    val username: String?,
    val password: String?,
    val options: AuthSignInOptions,
)

internal class SignInAuthTask(
    private val authStateMachine: AuthStateMachine,
    val configuration: AuthConfiguration,
    val request: SignInRequest,
) : AuthTask<AuthSignInResult> {

    override suspend fun validateStates(): AuthSignInResult {
        val authState = authStateMachine.getCurrentStateAsync().await()
        when (val authNState = authState.authNState) {
            is AuthenticationState.NotConfigured ->
                throw InvalidUserPoolConfigurationException()
            // Continue sign in
            is AuthenticationState.SignedOut, is AuthenticationState.Configured -> {
                return execute()
            }
            is AuthenticationState.SignedIn -> {
                if (request.username == authNState.signedInData.username) {
                    return AuthSignInResult(
                        true,
                        AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                    )
//                    onSuccess.accept(authSignInResult)
                } else {
                    throw SignedInException()
                }
            }
            else -> throw InvalidStateException()
        }
    }

    fun sendEvent() {
        val signInOptions =
            request.options as? AWSCognitoAuthSignInOptions ?: AWSCognitoAuthSignInOptions.builder()
                .authFlowType(configuration.authFlowType)
                .build()

        val flowType = signInOptions.authFlowType ?: AuthFlowType.USER_SRP_AUTH

        val signInData = when (flowType) {
            AuthFlowType.USER_SRP_AUTH -> {
                SignInData.SRPSignInData(request.username, request.password, signInOptions.metadata)
            }
            AuthFlowType.CUSTOM_AUTH, AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP -> {
                SignInData.CustomAuthSignInData(request.username, signInOptions.metadata)
            }
            AuthFlowType.CUSTOM_AUTH_WITH_SRP -> {
                SignInData.CustomSRPAuthSignInData(request.username, request.password, signInOptions.metadata)
            }
            AuthFlowType.USER_PASSWORD_AUTH -> {
                SignInData.MigrationAuthSignInData(request.username, request.password, signInOptions.metadata)
            }
        }
        val event = AuthenticationEvent(AuthenticationEvent.EventType.SignInRequested(signInData))
        authStateMachine.send(event)
    }

    override suspend fun execute(): AuthSignInResult {
        sendEvent()

        val channel = authStateMachine.listenAsync()

        for (authState in channel) {
            val authNState = authState.authNState
            val authZState = authState.authZState
            when {
                authNState is AuthenticationState.SigningIn -> {
                    val signInState = authNState.signInState
                    val srpSignInState = (signInState as? SignInState.SigningInWithSRP)?.srpSignInState
                    val challengeState = (signInState as? SignInState.ResolvingChallenge)?.challengeState
                    when {
                        srpSignInState is SRPSignInState.Error -> {
                            throw CognitoAuthExceptionConverter.lookup(srpSignInState.exception, "Sign in failed.")
                        }
                        signInState is SignInState.Error -> {
                            throw CognitoAuthExceptionConverter.lookup(signInState.exception, "Sign in failed.")
                        }
                        challengeState is SignInChallengeState.WaitingForAnswer -> {
//                            SignInChallengeHelper.getNextStep(challengeState.challenge, onSuccess, onError)
                            throw InvalidStateException()
                        }
                    }
                }
                authNState is AuthenticationState.SignedIn && authZState is AuthorizationState.SessionEstablished -> {
                    channel.close()
                    return AuthSignInResult(
                        true,
                        AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                    )
//                    sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
                }
            }
        }
        throw InvalidStateException()
    }

    override suspend operator fun invoke(): AuthSignInResult {
        return validateStates()
    }
}
