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
     * Multi-user sign-out: signs out the user identified by [userId], or — when [userId] is null —
     * either every tracked user (default) or just the active user (opt-out).
     *
     * **No explicit [userId]:** routing is governed by
     * [AWSCognitoAuthSignOutOptions.isSignOutAllUsers] on [options]. The default is `true`, which
     * iterates every user returned by [AuthStateMachine.allUserIds] (the union of in-memory and
     * persisted users in [AuthStateRepo]) and signs each one out sequentially. Pass an
     * [AWSCognitoAuthSignOutOptions] with `signOutAllUsers = false` to revert to single-active-user
     * semantics for callers that haven't opted into multi-user. When the repo is empty, the active
     * state-machine path is used, preserving upstream single-user behaviour.
     *
     * **Explicit [userId]:** reads that user's state via [AuthStateMachine.getStateForUser],
     * dispatches the sign-out event scoped to [userId], and awaits the terminal transition through
     * the global state flow (per-user transitions surface there because the state machine's
     * single-thread context serialises them). [AWSCognitoAuthSignOutOptions.isSignOutAllUsers] has
     * no effect in this branch.
     *
     * Iteration result aggregation: returns [AWSCognitoAuthSignOutResult.CompleteSignOut] when every
     * user signs out cleanly; otherwise returns the most recent non-complete per-user result
     * (partial or failed). Each user's sign-out is independent — one user's failure does not abort
     * the iteration.
     */
    suspend fun execute(
        userId: String?,
        options: AuthSignOutOptions = AuthSignOutOptions.builder().build()
    ): AuthSignOutResult {
        if (userId.isNullOrEmpty()) {
            val signOutAllUsers = (options as? AWSCognitoAuthSignOutOptions)?.isSignOutAllUsers ?: true
            if (signOutAllUsers) {
                val userIds = stateMachine.allUserIds()
                if (userIds.isNotEmpty()) {
                    return signOutEachUser(userIds, options)
                }
                // Repo is empty — fall through to the active-user / upstream single-user path.
            }
        }
        return signOutOne(userId, options)
    }

    private suspend fun signOutEachUser(userIds: Set<String>, options: AuthSignOutOptions): AuthSignOutResult {
        var lastNonComplete: AuthSignOutResult? = null
        for (uid in userIds) {
            val result = signOutOne(uid, options)
            if (result !is AWSCognitoAuthSignOutResult.CompleteSignOut) {
                lastNonComplete = result
            }
        }
        return lastNonComplete ?: AWSCognitoAuthSignOutResult.CompleteSignOut
    }

    private suspend fun signOutOne(userId: String?, options: AuthSignOutOptions): AuthSignOutResult {
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
