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
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.HostedUIError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.auth.cognito.toAuthException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription

internal class SignOutUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {

    suspend fun execute(options: AuthSignOutOptions = AuthSignOutOptions.builder().build()): AuthSignOutResult =
        execute(userId = null, options = options)

    /**
     * Multi-user sign-out: signs out the user identified by [userId].
     *
     * When [userId] is null, behaves identically to the no-arg overload (active/single-user
     * sign-out via the global state machine — preserves upstream semantics).
     *
     * When [userId] is non-null, reads that user's state from [AuthStateRepo] via
     * [AuthStateMachine.getStateForUser], dispatches the sign-out event scoped to [userId], and
     * awaits the terminal transition through the global state flow (per-user transitions are
     * surfaced there because the state machine's single-thread context serialises them).
     */
    suspend fun execute(
        userId: String?,
        options: AuthSignOutOptions = AuthSignOutOptions.builder().build()
    ): AuthSignOutResult {
        val authState = if (userId.isNullOrEmpty()) {
            stateMachine.getCurrentState()
        } else {
            stateMachine.getStateForUser(userId)
        }
        return when (authState.authNState) {
            is AuthenticationState.NotConfigured -> AWSCognitoAuthSignOutResult.CompleteSignOut
            // Continue sign out and clear auth or guest credentials
            is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> {
                completeSignOut(
                    event = createSignOutEvent(options, userId),
                    sendHubEvent = true,
                    userId = userId
                )
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

    suspend fun completeSignOut(
        event: StateMachineEvent,
        sendHubEvent: Boolean,
        userId: String? = null
    ): AuthSignOutResult {
        var cancellationException: UserCancelledException? = null

        val result = stateMachine.state
            .onSubscription {
                if (userId.isNullOrEmpty()) {
                    stateMachine.send(event)
                } else {
                    stateMachine.send(event, userId)
                }
            }
            .drop(1) // Ignore current state
            .mapNotNull { authState ->
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
                                globalSignOutError = signedOutData.globalSignOutErrorData?.let {
                                    GlobalSignOutError(it)
                                },
                                revokeTokenError = signedOutData.revokeTokenErrorData?.let { RevokeTokenError(it) }
                            )
                        } else {
                            AWSCognitoAuthSignOutResult.CompleteSignOut
                        }
                    }
                    authNState is AuthenticationState.Error -> AWSCognitoAuthSignOutResult.FailedSignOut(
                        authNState.exception.toAuthException("Sign out failed.")
                    )
                    authNState is AuthenticationState.SigningOut -> {
                        val state = authNState.signOutState
                        if (state is SignOutState.Error && state.exception is UserCancelledException) {
                            cancellationException = state.exception
                        }
                        null
                    }
                    authNState is AuthenticationState.SignedIn && cancellationException != null ->
                        AWSCognitoAuthSignOutResult.FailedSignOut(cancellationException)
                    else -> null // no-op
                }
            }.first()

        return result
    }

    private fun createSignOutEvent(options: AuthSignOutOptions, userId: String? = null): StateMachineEvent =
        AuthenticationEvent(
            AuthenticationEvent.EventType.SignOutRequested(
                SignOutData(
                    globalSignOut = options.isGlobalSignOut,
                    browserPackage = (options as? AWSCognitoAuthSignOutOptions)?.browserPackage,
                    userId = userId
                )
            )
        )
}
