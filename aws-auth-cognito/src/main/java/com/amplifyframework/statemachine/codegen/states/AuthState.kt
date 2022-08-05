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

import com.amplifyframework.auth.cognito.isAuthEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.AuthActions
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthEvent

sealed class AuthState : State {
    data class NotConfigured(val id: String = "") : AuthState()
    data class ConfiguringAuth(val id: String = "") : AuthState()

    data class WaitingForCachedCredentials(val config: AuthConfiguration) : AuthState()

    data class ValidatingCredentialsAndConfiguration(val id: String = "") : AuthState()

    data class ConfiguringAuthentication(
        override var authNState: AuthenticationState?
    ) : AuthState()

    data class ConfiguringAuthorization(
        override var authNState: AuthenticationState?,
        override var authZState: AuthorizationState?
    ) : AuthState()

    data class Configured(
        override var authNState: AuthenticationState?,
        override var authZState: AuthorizationState?
    ) : AuthState()

    data class Error(val exception: Exception) : AuthState()

    open var authNState: AuthenticationState? = AuthenticationState.NotConfigured()
    open var authZState: AuthorizationState? = AuthorizationState.NotConfigured()

    class Resolver(
        private val authNResolver: StateMachineResolver<AuthenticationState>,
        private val authZResolver: StateMachineResolver<AuthorizationState>,
        private val authActions: AuthActions
    ) :
        StateMachineResolver<AuthState> {
        override val defaultState = NotConfigured()

        override fun resolve(oldState: AuthState, event: StateMachineEvent): StateResolution<AuthState> {
            val resolution = resolveAuthEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.authNState?.let { authNResolver.resolve(it, event) }?.let {
                builder.authNState = it.newState
                actions += it.actions
            }

            oldState.authZState?.let { authZResolver.resolve(it, event) }?.let {
                builder.authZState = it.newState
                actions += it.actions
            }

            return StateResolution(builder.build(), actions)
        }

        private fun resolveAuthEvent(oldState: AuthState, event: StateMachineEvent): StateResolution<AuthState> {
            val authEvent = event.isAuthEvent()
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> when (authEvent) {
                    is AuthEvent.EventType.ConfigureAuth -> StateResolution(
                        ConfiguringAuth(),
                        listOf(authActions.initializeAuthConfigurationAction(authEvent)) // verify
                    )
                    else -> defaultResolution
                }
                is ConfiguringAuth -> when (authEvent) {
                    is AuthEvent.EventType.FetchCachedCredentials -> StateResolution(
                        WaitingForCachedCredentials(authEvent.configuration)
                    )
                    else -> defaultResolution
                }
                is WaitingForCachedCredentials -> when (authEvent) {
                    is AuthEvent.EventType.ReceivedCachedCredentials -> StateResolution(
                        ValidatingCredentialsAndConfiguration(),
                        listOf(authActions.validateCredentialsAndConfiguration(authEvent))
                    )
                    else -> defaultResolution
                }
                is ValidatingCredentialsAndConfiguration -> when (authEvent) {
                    is AuthEvent.EventType.ConfigureAuthentication -> StateResolution(
                        ConfiguringAuthentication(AuthenticationState.NotConfigured()),
                        listOf(authActions.initializeAuthenticationConfigurationAction(authEvent))
                    )
                    is AuthEvent.EventType.ConfigureAuthorization -> StateResolution(
                        ConfiguringAuthorization(
                            AuthenticationState.NotConfigured(),
                            AuthorizationState.NotConfigured()
                        ),
                        listOf(authActions.initializeAuthorizationConfigurationAction(authEvent))
                    )
                    else -> defaultResolution
                }
                is ConfiguringAuthentication -> when (authEvent) {
                    is AuthEvent.EventType.ConfiguredAuthentication -> StateResolution(
                        ConfiguringAuthorization(
                            oldState.authNState,
                            AuthorizationState.NotConfigured()
                        ),
                        listOf(authActions.initializeAuthorizationConfigurationAction(authEvent))
                    )
                    else -> defaultResolution
                }
                is ConfiguringAuthorization -> when (authEvent) {
                    is AuthEvent.EventType.ConfiguredAuthorization -> StateResolution(
                        Configured(oldState.authNState, oldState.authZState)
                    )
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val authState: AuthState) :
        com.amplifyframework.statemachine.Builder<AuthState> {
        var authNState: AuthenticationState? = null
        var authZState: AuthorizationState? = null

        override fun build() = when (authState) {
            is ConfiguringAuthentication -> ConfiguringAuthentication(authNState)
            is ConfiguringAuthorization -> ConfiguringAuthorization(authNState, authZState)
            is Configured -> Configured(authNState, authZState)
            else -> authState
        }
    }
}
