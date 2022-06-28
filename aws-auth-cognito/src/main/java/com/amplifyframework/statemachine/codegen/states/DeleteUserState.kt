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

package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.DeleteUserActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import java.lang.Exception

sealed class DeleteUserState : State {
    data class NotStarted(val id: String = "") : DeleteUserState()
    data class DeletingUser(val id: String = "") : DeleteUserState()
    data class SigningOut(val id: String = "") : DeleteUserState()
    data class UserDeleted(val id: String = "") : DeleteUserState()
    data class Error(val exception: Exception) : DeleteUserState()

    class Resolver(private val deleteUserActions: DeleteUserActions) :
        StateMachineResolver<DeleteUserState> {
        override val defaultState = NotStarted()
        private fun asDeleteUserEvent(event: StateMachineEvent): DeleteUserEvent.EventType? {
            return (event as? DeleteUserEvent)?.eventType
        }

        private fun asAuthenticationEvent(event: StateMachineEvent): AuthenticationEvent.EventType? {
            return (event as? AuthenticationEvent)?.eventType
        }

        override fun resolve(
            oldState: DeleteUserState,
            event: StateMachineEvent
        ): StateResolution<DeleteUserState> {
            val deleteUserEvent = asDeleteUserEvent(event)
            val authenticationEvent = asAuthenticationEvent(event)
            return when (oldState) {
                is NotStarted -> {
                    when (deleteUserEvent) {
                        is DeleteUserEvent.EventType.DeleteUser -> {
                            val action = deleteUserActions.initDeleteUserAction(deleteUserEvent.accessToken)
                            StateResolution(DeletingUser(), listOf(action))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is DeletingUser -> {
                    when {
                        authenticationEvent is AuthenticationEvent.EventType.SignOutRequested -> StateResolution(SigningOut())
                        deleteUserEvent is DeleteUserEvent.EventType.ThrowError -> {
                            StateResolution(Error(deleteUserEvent.exception))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is SigningOut -> {
                    when {
                        authenticationEvent is AuthenticationEvent.EventType.InitializedSignedOut -> StateResolution((UserDeleted()))
                        else -> StateResolution(oldState)
                    }
                }
                else -> StateResolution(oldState)
            }
        }
    }
}
