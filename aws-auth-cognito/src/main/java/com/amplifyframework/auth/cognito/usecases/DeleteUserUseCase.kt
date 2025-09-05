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
import com.amplifyframework.auth.cognito.helpers.collectWhile
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState
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

    suspend fun execute() {
        stateMachine.requireSignedInState()

        val token = fetchAuthSession.execute().requireAccessToken()

        var deleteUserException: Exception? = null
        stateMachine.state
            .onSubscription {
                val event = DeleteUserEvent(DeleteUserEvent.EventType.DeleteUser(accessToken = token))
                stateMachine.send(event)
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
                        throw CognitoAuthExceptionConverter.lookup(
                            deleteUserException!!,
                            "Request to delete user may have failed. Please check exception stack"
                        )
                    }
                    else -> true // no-op
                }
            }
    }
}
