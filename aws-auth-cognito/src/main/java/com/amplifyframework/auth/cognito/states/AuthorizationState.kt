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

package com.amplifyframework.auth.cognito.states

import com.amplifyframework.auth.cognito.events.AuthorizationEvent
import com.amplifyframework.statemachine.*
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions

sealed class AuthorizationState : State {
    data class NotConfigured(val id: String = "") : AuthorizationState()
    data class Configured(val id: String = "") : AuthorizationState()
    data class FetchingAuthSession(val id: String = "") : AuthorizationState()
    data class SessionEstablished(val id: String = "") : AuthorizationState()
    data class Error(val exception: Exception) : AuthorizationState()

    override val type = this.toString()

    class Resolver(private val authorizationActions: AuthorizationActions) : StateMachineResolver<AuthorizationState> {
        override val defaultState = NotConfigured("")

        private fun asAuthorizationEvent(event: StateMachineEvent): AuthorizationEvent.EventType? {
            return (event as? AuthorizationEvent)?.eventType
        }

        override fun resolve(
            oldState: AuthorizationState,
            event: StateMachineEvent,
        ): StateResolution<AuthorizationState> {
            val authorizationEvent = asAuthorizationEvent(event)
            return when (oldState) {
                is NotConfigured -> {
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.Configure -> onConfigure()
                        is AuthorizationEvent.EventType.ThrowError -> StateResolution(Error(authorizationEvent.exception))
                        else -> StateResolution.from(oldState)
                    }
                }
                is Configured, is SessionEstablished ->
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.FetchAuthSession -> onFetchAuthSession(authorizationEvent.credentials)
                        else -> StateResolution.from(oldState)
                    }
                is FetchingAuthSession ->
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.FetchedAuthSession -> onFetchedAuthSession(authorizationEvent.session)
                        else -> StateResolution.from(oldState)
                    }
                else -> StateResolution(oldState)
            }
        }

        private fun onFetchedAuthSession(session: String): StateResolution<AuthorizationState> {
            TODO("Implement FetchAuthSession, and FetchingAuthSession etc to call sub state machine" +
                    "Then change state to SessionEstablished")
        }

        private fun onFetchAuthSession(credential: Any?): StateResolution<AuthorizationState> {
            /**
             * TODO Implement below
            val action = InitializeFetchAuthSession(credential)
            val newState = FetchAuthSessionState.InitializingFetchAuthSession
            return StateResolution(newState, listOf(action))
             */
            TODO("NOT Implemented yet")
        }

        private fun onConfigure(): StateResolution<AuthorizationState> {
            val action = authorizationActions.configureAuthorizationAction()
            val newState = Configured()
            return StateResolution(newState, listOf(action))
        }
    }
}