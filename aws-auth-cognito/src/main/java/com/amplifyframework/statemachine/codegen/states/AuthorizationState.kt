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
import com.amplifyframework.auth.cognito.isAuthenticationEvent
import com.amplifyframework.auth.cognito.isAuthorizationEvent
import com.amplifyframework.auth.cognito.isDeleteUserEvent
import com.amplifyframework.auth.cognito.isSignOutEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable @SerialName("AuthorizationState")
sealed class AuthorizationState : State {
    @Serializable @SerialName("AuthorizationState.NotConfigured")
    data class NotConfigured(val id: String = "") : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.Configured")
    data class Configured(val id: String = "") : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.SigningOut")
    data class SigningIn(val id: String = "") : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.SigningIn")
    data class SigningOut(val amplifyCredential: AmplifyCredential) : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.FetchingAuthSession")
    data class FetchingAuthSession(override var fetchAuthSessionState: FetchAuthSessionState?) : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.DeletingUser")
    data class DeletingUser(override var deleteUserState: DeleteUserState?) : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.WaitingToStore")
    data class WaitingToStore(var amplifyCredential: AmplifyCredential) : AuthorizationState()
    @Serializable @SerialName("AuthorizationState.SessionEstablished")
    data class SessionEstablished(val amplifyCredential: AmplifyCredential) : AuthorizationState()
    data class Error(val exception: Exception) : AuthorizationState()

    @Transient
    open var fetchAuthSessionState: FetchAuthSessionState? = FetchAuthSessionState.NotStarted()
    @Transient
    open var deleteUserState: DeleteUserState? = DeleteUserState.NotStarted()

    @Transient
    override val type = this.toString()

    class Resolver(
        private val fetchAuthSessionResolver: StateMachineResolver<FetchAuthSessionState>,
        private val deleteUserResolver: StateMachineResolver<DeleteUserState>,
        private val authorizationActions: AuthorizationActions
    ) : StateMachineResolver<AuthorizationState> {
        override val defaultState = NotConfigured("")

        override fun resolve(
            oldState: AuthorizationState,
            event: StateMachineEvent,
        ): StateResolution<AuthorizationState> {
            val resolution = resolveAuthorizationEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.fetchAuthSessionState?.let { fetchAuthSessionResolver.resolve(it, event) }?.let {
                builder.fetchAuthSessionState = it.newState
                actions += it.actions
            }

            oldState.deleteUserState?.let { deleteUserResolver.resolve(it, event) }?.let {
                builder.deleteUserState = it.newState
                actions += it.actions
            }
            return StateResolution(builder.build(), actions)
        }

        private fun resolveAuthorizationEvent(
            oldState: AuthorizationState,
            event: StateMachineEvent
        ): StateResolution<AuthorizationState> {
            val authEvent = event.isAuthEvent()
            val authenticationEvent = event.isAuthenticationEvent()
            val authorizationEvent = event.isAuthorizationEvent()
            val deleteUserEvent = event.isDeleteUserEvent()
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> {
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.Configure -> {
                            val action = authorizationActions.configureAuthorizationAction()
                            StateResolution(Configured(), listOf(action))
                        }
                        is AuthorizationEvent.EventType.CachedCredentialsAvailable -> {
                            val action = authorizationActions.configureAuthorizationAction()
                            StateResolution(SessionEstablished(authorizationEvent.amplifyCredential), listOf(action))
                        }
                        is AuthorizationEvent.EventType.ThrowError -> {
                            val action = authorizationActions.resetAuthorizationAction()
                            StateResolution(Error(authorizationEvent.exception), listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                is Configured ->
                    when {
                        authorizationEvent is AuthorizationEvent.EventType.FetchAuthSession -> {
                            val action =
                                authorizationActions.initializeFetchAuthSession(AmplifyCredential.Empty)
                            val newState = FetchingAuthSession(oldState.fetchAuthSessionState)
                            StateResolution(newState, listOf(action))
                        }
                        deleteUserEvent is DeleteUserEvent.EventType.DeleteUser -> {
                            StateResolution(DeletingUser(oldState.deleteUserState))
                        }
                        authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> StateResolution(
                            SigningIn()
                        )
                        else -> defaultResolution
                    }
                is WaitingToStore -> when (authEvent) {
                    is AuthEvent.EventType.ReceivedCachedCredentials -> {
                        if (oldState.amplifyCredential is AmplifyCredential.Empty) StateResolution(Configured())
                        else StateResolution(SessionEstablished(authEvent.storedCredentials))
                    }
                    is AuthEvent.EventType.CachedCredentialsFailed -> StateResolution(NotConfigured())
                    else -> defaultResolution
                }
                is SigningIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignInCompleted -> {
                        val action = authorizationActions.initializeFetchAuthSession(
                            AmplifyCredential.UserPool(authenticationEvent.signedInData.cognitoUserPoolTokens)
                        )
                        StateResolution(FetchingAuthSession(FetchAuthSessionState.NotStarted()), listOf(action))
                    }
                    is AuthenticationEvent.EventType.CancelSignIn -> {
                        val action = authorizationActions.initializeFetchAuthSession(AmplifyCredential.Empty)
                        StateResolution(FetchingAuthSession(FetchAuthSessionState.NotStarted()), listOf(action))
                    }
                    else -> defaultResolution
                }
                is SigningOut -> {
                    when {
                        event.isSignOutEvent() is SignOutEvent.EventType.SignOutLocally -> {
                            StateResolution(WaitingToStore(AmplifyCredential.Empty))
                        }
                        authenticationEvent is AuthenticationEvent.EventType.CancelSignOut -> {
                            StateResolution(SessionEstablished(oldState.amplifyCredential))
                        }
                        else -> defaultResolution
                    }
                }
                is FetchingAuthSession ->
                    when (authorizationEvent) {
                        is AuthorizationEvent.EventType.Fetched -> StateResolution(
                            WaitingToStore(authorizationEvent.amplifyCredential)
                        )
                        is AuthorizationEvent.EventType.ThrowError -> StateResolution(
                            Error(authorizationEvent.exception)
                        )
                        else -> defaultResolution
                    }
                is SessionEstablished, is Error -> when {
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> StateResolution(SigningIn())
                    authenticationEvent is AuthenticationEvent.EventType.SignOutRequested -> {
                        if (oldState is SessionEstablished) {
                            StateResolution(SigningOut(oldState.amplifyCredential))
                        } else {
                            defaultResolution
                        }
                    }
                    authorizationEvent is AuthorizationEvent.EventType.FetchAuthSession -> {
                        val action =
                            authorizationActions.initializeFetchAuthSession(AmplifyCredential.Empty)
                        val newState = FetchingAuthSession(oldState.fetchAuthSessionState)
                        StateResolution(newState, listOf(action))
                    }
                    authorizationEvent is AuthorizationEvent.EventType.RefreshAuthSession -> {
                        val action =
                            authorizationActions.refreshAuthSessionAction(authorizationEvent.amplifyCredential)
                        val newState = FetchingAuthSession(oldState.fetchAuthSessionState)
                        StateResolution(newState, listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val authZState: AuthorizationState) :
        com.amplifyframework.statemachine.Builder<AuthorizationState> {
        var fetchAuthSessionState: FetchAuthSessionState? = null
        var deleteUserState: DeleteUserState? = null
        override fun build(): AuthorizationState = when (authZState) {
            is FetchingAuthSession -> FetchingAuthSession(fetchAuthSessionState)
            is SessionEstablished -> SessionEstablished(authZState.amplifyCredential)
            is DeletingUser -> DeletingUser(deleteUserState)
            else -> authZState
        }
    }
}
