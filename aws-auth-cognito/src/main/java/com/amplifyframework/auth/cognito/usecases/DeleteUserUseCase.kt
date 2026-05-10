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
import com.amplifyframework.auth.cognito.helpers.collectWhile
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState
import com.amplifyframework.auth.cognito.toAuthException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import kotlinx.coroutines.flow.onSubscription

internal class DeleteUserUseCase(
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {

    suspend fun execute() = execute(userId = null)

    /**
     * Multi-user delete: deletes the user identified by [userId].
     *
     * When [userId] is null, behaves identically to the no-arg overload (active / single-user
     * legacy path).
     *
     * When [userId] is non-null, fetches the access token via the userId-aware fetchAuthSession
     * overload, dispatches a userId-bearing DeleteUser event, and routes through the state
     * machine's per-user send. AuthStateRepo's signOut→remove rule will purge the user's persisted
     * state once the deletion completes.
     *
     * Note: per the contract (skill §A.1), [AuthCategoryBehavior.deleteUser] does not expose a
     * userId parameter at the public API surface — this overload is only used internally when the
     * caller already has a userId in hand (e.g. from a multi-user-aware Plugin call site).
     */
    suspend fun execute(userId: String?) {
        stateMachine.requireSignedInState()

        // Preserve the no-arg call shape when userId is null so existing test mocks of
        // fetchAuthSession.execute() continue to match.
        val token = if (userId.isNullOrEmpty()) {
            fetchAuthSession.execute().requireAccessToken()
        } else {
            fetchAuthSession.execute(userId).requireAccessToken()
        }

        var deleteUserException: Exception? = null
        stateMachine.state
            .onSubscription {
                val event = DeleteUserEvent(
                    DeleteUserEvent.EventType.DeleteUser(accessToken = token, userId = userId)
                )
                if (userId.isNullOrEmpty()) stateMachine.send(event) else stateMachine.send(event, userId)
            }.collectWhile { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState

                when {
                    authZState is AuthorizationState.DeletingUser &&
                        authZState.deleteUserState is DeleteUserState.Error -> {
                        // Hold on to the exception until the state machine settles
                        deleteUserException = authZState.deleteUserState.exception
                        true
                    }
                    authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                        emitter.sendHubEvent(AuthChannelEventName.USER_DELETED.toString())
                        false // done
                    }
                    authZState is AuthorizationState.SessionEstablished && deleteUserException != null -> {
                        throw deleteUserException.toAuthException(
                            "Request to delete user may have failed. Please check exception stack"
                        )
                    }
                    else -> true // no-op
                }
            }
    }
}
