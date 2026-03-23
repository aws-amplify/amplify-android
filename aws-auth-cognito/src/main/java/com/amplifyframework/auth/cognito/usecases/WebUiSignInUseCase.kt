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

import android.app.Activity
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.helpers.HostedUIHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription

internal class WebUiSignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val configuration: AuthConfiguration,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        callingActivity: Activity,
        provider: AuthProvider? = null,
        options: AuthWebUISignInOptions = AWSCognitoAuthWebUISignInOptions.builder().build()
    ): AuthSignInResult {
        waitForStateThatAllowsSignIn()

        if (configuration.oauth == null) {
            throw InvalidOauthConfigurationException()
        }

        val hostedUIOptions = HostedUIHelper.createHostedUIOptions(callingActivity, provider, options)
        val event = AuthenticationEvent(
            AuthenticationEvent.EventType.SignInRequested(
                SignInData.HostedUISignInData(hostedUIOptions)
            )
        )

        val result = stateMachine.state
            .onSubscription { stateMachine.send(event) }
            .drop(1)
            .mapNotNull { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val hostedUISignInState = authNState.signInState.hostedUISignInState
                        if (hostedUISignInState is HostedUISignInState.Error) {
                            val exception = hostedUISignInState.exception
                            stateMachine.send(
                                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
                            )
                            throw if (exception is AuthException) {
                                exception
                            } else {
                                UnknownException("Sign in failed", exception)
                            }
                        }
                        null
                    }
                    authNState is AuthenticationState.SignedIn &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        AuthSignInResult(
                            true,
                            AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null, null, null, null)
                        )
                    }
                    else -> null
                }
            }.first()

        emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
        return result
    }

    private suspend fun waitForStateThatAllowsSignIn() {
        stateMachine.state.mapNotNull { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> throw InvalidUserPoolConfigurationException()
                is AuthenticationState.SignedOut -> authState
                is AuthenticationState.SignedIn -> throw SignedInException()
                is AuthenticationState.SigningIn -> {
                    stateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                    null
                }
                else -> throw InvalidStateException()
            }
        }.first()
    }
}
