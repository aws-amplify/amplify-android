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
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent

sealed class AuthorizationState : State {
    data class NotConfigured(val id: String = "") : AuthorizationState()
    data class Configured(val id: String = "") : AuthorizationState()
    data class FetchingAuthSession(override var fetchAuthSessionState: FetchAuthSessionState?) :
        AuthorizationState()

    data class SessionEstablished(val amplifyCredential: AmplifyCredential?) :
        AuthorizationState()

    data class Error(val exception: Exception) : AuthorizationState()

    open var fetchAuthSessionState: FetchAuthSessionState? =
        FetchAuthSessionState.InitializingFetchAuthSession()

    override val type = this.toString()

    class Resolver(
        private val fetchAuthSessionResolver: StateMachineResolver<FetchAuthSessionState>,
        private val authorizationActions: AuthorizationActions
    ) : StateMachineResolver<AuthorizationState> {
        override val defaultState = NotConfigured("")

        private fun asAuthorizationEvent(event: StateMachineEvent): AuthorizationEvent.EventType? {
            return (event as? AuthorizationEvent)?.eventType
        }

        override fun resolve(
            oldState: AuthorizationState,
            event: StateMachineEvent,
        ): StateResolution<AuthorizationState> {
            val resolution = resolveAuthorizationEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.fetchAuthSessionState?.let {
                fetchAuthSessionResolver.resolve(it, event)
            }?.let {
                builder.fetchAuthSessionState = it.newState
                actions += it.actions
            }
            return StateResolution(builder.build(), actions)
        }

        private fun resolveAuthorizationEvent(
            oldState: AuthorizationState,
            event: StateMachineEvent
        ): StateResolution<AuthorizationState> {
            val authorizationEvent = asAuthorizationEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> {
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.Configure -> {
                            val action = authorizationActions.configureAuthorizationAction()
                            StateResolution(Configured(), listOf(action))
                        }
                        is AuthorizationEvent.EventType.ThrowError -> {
                            val action = authorizationActions.resetAuthorizationAction()
                            StateResolution(Error(authorizationEvent.exception), listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                is Configured ->
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.FetchAuthSession -> {
                            val action =
                                authorizationActions.initializeFetchAuthSession(authorizationEvent.amplifyCredential)
                            val newState = FetchingAuthSession(oldState.fetchAuthSessionState)
                            StateResolution(newState, listOf(action))
                        }
                        else -> defaultResolution
                    }
                is FetchingAuthSession ->
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.FetchedAuthSession -> {
                            val action = authorizationActions.resetAuthorizationAction()
                            val newState = SessionEstablished(authorizationEvent.amplifyCredential)
                            StateResolution(newState, listOf(action))
                        }
                        else -> defaultResolution
                    }
                is SessionEstablished, is Error -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Configure -> StateResolution(Configured())
                    else -> defaultResolution
                }
            }
        }
    }

    class Builder(private val authZState: AuthorizationState) :
        com.amplifyframework.statemachine.Builder<AuthorizationState> {
        var fetchAuthSessionState: FetchAuthSessionState? = null

        override fun build(): AuthorizationState = when (authZState) {
            is FetchingAuthSession -> FetchingAuthSession(fetchAuthSessionState)
            is SessionEstablished -> SessionEstablished(authZState.amplifyCredential)
            else -> authZState
        }
    }
}
