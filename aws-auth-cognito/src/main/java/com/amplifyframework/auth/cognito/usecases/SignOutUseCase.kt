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
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.HostedUIError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

internal class SignOutUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {

    suspend fun execute(options: AuthSignOutOptions = AuthSignOutOptions.builder().build()): AuthSignOutResult {
        val authState = stateMachine.getCurrentState()
        return when (authState.authNState) {
            is AuthenticationState.NotConfigured -> AWSCognitoAuthSignOutResult.CompleteSignOut
            // Continue sign out and clear auth or guest credentials
            is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> {
                // Send SignOut event here instead of OnSubscribedCallback handler to ensure we do not fire
                // onComplete immediately, which would happen if calling signOut while signed out
                sendSignOutRequest(options)
                completeSignOut(sendHubEvent = true)
            }
            is AuthenticationState.FederatedToIdentityPool -> {
                AWSCognitoAuthSignOutResult.FailedSignOut(
                    InvalidStateException(
                        "The user is currently federated to identity pool. " +
                            "You must call clearFederationToIdentityPool to clear credentials."
                    )
                )
            }
            else -> AWSCognitoAuthSignOutResult.FailedSignOut(InvalidStateException())
        }
    }

    suspend fun completeSignOut(sendHubEvent: Boolean): AuthSignOutResult {
        var cancellationException: UserCancelledException? = null

        val result = stateMachine.stateTransitions.mapNotNull { authState ->
            if (authState !is AuthState.Configured) {
                return@mapNotNull null
            }

            val (authNState, authZState) = authState

            when {
                authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                    if (sendHubEvent) {
                        emitter.sendHubEvent(AuthChannelEventName.SIGNED_OUT.toString())
                    }
                    if (authNState.signedOutData.hasError) {
                        val signedOutData = authNState.signedOutData
                        AWSCognitoAuthSignOutResult.PartialSignOut(
                            hostedUIError = signedOutData.hostedUIErrorData?.let { HostedUIError(it) },
                            globalSignOutError = signedOutData.globalSignOutErrorData?.let { GlobalSignOutError(it) },
                            revokeTokenError = signedOutData.revokeTokenErrorData?.let { RevokeTokenError(it) }
                        )
                    } else {
                        AWSCognitoAuthSignOutResult.CompleteSignOut
                    }
                }
                authNState is AuthenticationState.Error -> {
                    AWSCognitoAuthSignOutResult.FailedSignOut(
                        CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign out failed.")
                    )
                }
                authNState is AuthenticationState.SigningOut -> {
                    val state = authNState.signOutState
                    if (state is SignOutState.Error && state.exception is UserCancelledException) {
                        cancellationException = state.exception
                    }
                    null
                }
                authNState is AuthenticationState.SignedIn && cancellationException != null -> {
                    AWSCognitoAuthSignOutResult.FailedSignOut(cancellationException!!)
                }
                else -> null // no-op
            }
        }.first()

        return result
    }

    private fun sendSignOutRequest(options: AuthSignOutOptions) {
        val event = AuthenticationEvent(
            AuthenticationEvent.EventType.SignOutRequested(
                SignOutData(
                    options.isGlobalSignOut,
                    (options as? AWSCognitoAuthSignOutOptions)?.browserPackage
                )
            )
        )
        stateMachine.send(event)
    }
}
