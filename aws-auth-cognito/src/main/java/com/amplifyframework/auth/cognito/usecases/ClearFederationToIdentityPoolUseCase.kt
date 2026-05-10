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

import com.amplifyframework.auth.cognito.AWSCognitoAuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState

internal class ClearFederationToIdentityPoolUseCase(
    private val stateMachine: AuthStateMachine,
    private val signOut: SignOutUseCase,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute() = execute(userId = null)

    /**
     * Multi-user clear federation: clears the federated-identity-pool entry for [userId].
     *
     * When [userId] is null, behaves identically to the no-arg overload (active / single-user
     * legacy path).
     *
     * When [userId] is non-null, reads that user's state via getStateForUser, builds a
     * userId-bearing ClearFederationToIdentityPool event, and routes the sign-out through
     * SignOutUseCase.completeSignOut(event, sendHubEvent=false, userId).
     */
    suspend fun execute(userId: String?) {
        val authState = if (userId.isNullOrEmpty()) {
            stateMachine.getCurrentState()
        } else {
            stateMachine.getStateForUser(userId)
        }

        when {
            authState.isFederatedToIdentityPool() -> {
                val event = AuthenticationEvent(
                    AuthenticationEvent.EventType.ClearFederationToIdentityPool(userId = userId)
                )
                when (val result = signOut.completeSignOut(event = event, sendHubEvent = false, userId = userId)) {
                    is AWSCognitoAuthSignOutResult.FailedSignOut -> throw result.exception
                    else -> emitter.sendHubEvent(
                        AWSCognitoAuthChannelEventName.FEDERATION_TO_IDENTITY_POOL_CLEARED.toString()
                    )
                }
            }
            else -> throw InvalidStateException("Clearing of federation failed.")
        }
    }

    private fun AuthState.isFederatedToIdentityPool(): Boolean {
        val authNState = this.authNState
        val authZState = this.authZState

        return this is AuthState.Configured &&
            (
                authNState is AuthenticationState.FederatedToIdentityPool &&
                    authZState is AuthorizationState.SessionEstablished
                ) ||
            (
                authZState is AuthorizationState.Error &&
                    authZState.exception is SessionError &&
                    authZState.exception.amplifyCredential is AmplifyCredential.IdentityPoolFederated
                )
    }
}
