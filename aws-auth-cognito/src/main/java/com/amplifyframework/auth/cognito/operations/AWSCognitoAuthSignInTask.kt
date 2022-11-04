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

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
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

internal class AWSCognitoAuthSignInRequest(val username: String?, val password: String?, val options: AuthSignInOptions)

internal class AWSCognitoAuthSignInTask(
    authStateMachine: AuthStateMachine,
    configuration: AuthConfiguration,
    val request: AWSCognitoAuthSignInRequest,
) : AuthTask<AuthSignInResult>(authStateMachine, configuration) {

    override suspend fun validateStates(): AuthSignInResult {
        val authState = authStateMachine.getCurrentStateAsync().await()
        return when (authState.authNState) {
            is AuthenticationState.NotConfigured -> throw InvalidUserPoolConfigurationException()
            is AuthenticationState.SignedOut, is AuthenticationState.Configured -> {
                // Send event and continue sign in
                sendSignInEvent()
                listenAndComplete()
            }
            is AuthenticationState.SignedIn -> throw SignedInException()
            else -> throw InvalidStateException()
        }
    }

    private fun sendSignInEvent() {
        val signInOptions =
            request.options as? AWSCognitoAuthSignInOptions ?: AWSCognitoAuthSignInOptions.builder()
                .authFlowType(configuration.authFlowType)
                .build()

        val flowType = signInOptions.authFlowType ?: configuration.authFlowType

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

    override suspend fun listenAndComplete(): AuthSignInResult {
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
                            return SignInChallengeHelper.getNextSignInStepResult(challengeState.challenge)
                        }
                    }
                }
                authNState is AuthenticationState.SignedIn && authZState is AuthorizationState.SessionEstablished -> {
                    channel.close()
                    sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
                    return AuthSignInResult(
                        true,
                        AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                    )
                }
            }
        }
        throw InvalidStateException()
    }

    override suspend operator fun invoke(): AuthSignInResult {
        return validateStates()
    }
}
