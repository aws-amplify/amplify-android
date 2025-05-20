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

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile

internal class AutoSignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val hubEmitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(): AuthSignInResult {
        val authState = waitForSignedOutState()
        val signUpData = getSignUpData(authState)
        val result = completeAutoSignIn(signUpData)
        return result
    }

    private suspend fun waitForSignedOutState(): AuthState {
        val authState = stateMachine.state.transformWhile { authState ->
            when (val authNState = authState.authNState) {
                is AuthenticationState.NotConfigured -> throw InvalidUserPoolConfigurationException()
                is AuthenticationState.SignedOut -> {
                    emit(authState)
                    false
                }
                is AuthenticationState.SigningOut -> true
                is AuthenticationState.SigningIn -> {
                    // Cancel the sign in
                    stateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                    true
                }
                is AuthenticationState.Error -> {
                    throw CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign in failed.")
                }
                else -> throw InvalidStateException()
            }
        }.first()
        return authState
    }

    private fun getSignUpData(authState: AuthState): SignUpData = when (val signUpState = authState.authSignUpState) {
        is SignUpState.SignedUp -> signUpState.signUpData
        else -> throw InvalidStateException()
    }

    private suspend fun completeAutoSignIn(signUpData: SignUpData): AuthSignInResult {
        val signInData = SignInData.AutoSignInData(
            signUpData.username,
            signUpData.session,
            signUpData.clientMetadata ?: mapOf(),
            signUpData.userId
        )

        val result = stateMachine.stateTransitions
            .onStart {
                val event = AuthenticationEvent(AuthenticationEvent.EventType.SignInRequested(signInData))
                stateMachine.send(event)
            }
            .transformWhile { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val signInState = authNState.signInState
                        if (signInState is SignInState.Error) {
                            throw CognitoAuthExceptionConverter.lookup(signInState.exception, "Sign in failed.")
                        }
                        true
                    }
                    authNState is AuthenticationState.SignedIn &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        // There are never any next steps for autoSignIn - if it succeeds then the user is fully
                        // signed in
                        val authSignInResult = AuthSignInResult(
                            true,
                            AuthNextSignInStep(
                                AuthSignInStep.DONE,
                                mapOf(),
                                null,
                                null,
                                null,
                                null
                            )
                        )
                        emit(authSignInResult)
                        hubEmitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
                        false
                    }
                    else -> true
                }
            }.first()
        return result
    }
}
