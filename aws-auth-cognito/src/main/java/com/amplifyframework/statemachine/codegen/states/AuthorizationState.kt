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
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

internal sealed class AuthorizationState : State {
    data class NotConfigured(val id: String = "") : AuthorizationState()
    data class Configured(val id: String = "") : AuthorizationState()
    data class SigningIn(val id: String = "") : AuthorizationState()
    data class SigningOut(val amplifyCredential: AmplifyCredential) : AuthorizationState()
    data class FetchingAuthSession(
        val signedInData: SignedInData,
        val fetchAuthSessionState: FetchAuthSessionState
    ) : AuthorizationState()

    data class FetchingUnAuthSession(val fetchAuthSessionState: FetchAuthSessionState) : AuthorizationState()
    data class RefreshingSession(
        val existingCredential: AmplifyCredential,
        val refreshSessionState: RefreshSessionState
    ) : AuthorizationState()

    data class DeletingUser(
        val deleteUserState: DeleteUserState,
        val amplifyCredential: AmplifyCredential
    ) : AuthorizationState()
    data class StoringCredentials(val amplifyCredential: AmplifyCredential) : AuthorizationState()
    data class SessionEstablished(val amplifyCredential: AmplifyCredential) : AuthorizationState()
    data class FederatingToIdentityPool(
        val federatedToken: FederatedToken,
        val fetchAuthSessionState: FetchAuthSessionState,
        val existingCredential: AmplifyCredential?
    ) : AuthorizationState()

    data class Error(val exception: Exception) : AuthorizationState()

    override val type = this.toString()

    class Resolver(
        private val fetchAuthSessionResolver: StateMachineResolver<FetchAuthSessionState>,
        private val refreshSessionResolver: StateMachineResolver<RefreshSessionState>,
        private val deleteUserResolver: StateMachineResolver<DeleteUserState>,
        private val authorizationActions: AuthorizationActions
    ) : StateMachineResolver<AuthorizationState> {
        override val defaultState = NotConfigured("")

        override fun resolve(
            oldState: AuthorizationState,
            event: StateMachineEvent
        ): StateResolution<AuthorizationState> {
            val authEvent = event.isAuthEvent()
            val authenticationEvent = event.isAuthenticationEvent()
            val authorizationEvent = event.isAuthorizationEvent()
            val deleteUserEvent = event.isDeleteUserEvent()
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Configure -> {
                        val action = authorizationActions.configureAuthorizationAction()
                        StateResolution(Configured(), listOf(action))
                    }
                    is AuthorizationEvent.EventType.CachedCredentialsAvailable -> {
                        val action = authorizationActions.configureAuthorizationAction()
                        StateResolution(SessionEstablished(authorizationEvent.amplifyCredential), listOf(action))
                    }
                    is AuthorizationEvent.EventType.ThrowError -> StateResolution(NotConfigured())
                    else -> defaultResolution
                }
                is Configured -> when {
                    authorizationEvent is AuthorizationEvent.EventType.FetchUnAuthSession -> {
                        val action = authorizationActions.initializeFetchUnAuthSession()
                        val newState = FetchingUnAuthSession(FetchAuthSessionState.NotStarted())
                        StateResolution(newState, listOf(action))
                    }
                    authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                        val action =
                            authorizationActions.initializeFederationToIdentityPool(
                                authorizationEvent.token,
                                authorizationEvent.identityId
                            )
                        val newState = FederatingToIdentityPool(
                            authorizationEvent.token,
                            FetchAuthSessionState.NotStarted(),
                            authorizationEvent.existingCredential
                        )
                        StateResolution(newState, listOf(action))
                    }
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> StateResolution(SigningIn())
                    else -> defaultResolution
                }
                is StoringCredentials -> when (authEvent) {
                    is AuthEvent.EventType.ReceivedCachedCredentials -> {
                        if (oldState.amplifyCredential is AmplifyCredential.Empty) StateResolution(Configured())
                        else StateResolution(SessionEstablished(authEvent.storedCredentials))
                    }
                    is AuthEvent.EventType.CachedCredentialsFailed -> StateResolution(NotConfigured())
                    else -> defaultResolution
                }
                is SigningIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignInCompleted -> {
                        val action = authorizationActions.initializeFetchAuthSession(authenticationEvent.signedInData)
                        StateResolution(
                            FetchingAuthSession(authenticationEvent.signedInData, FetchAuthSessionState.NotStarted()),
                            listOf(action)
                        )
                    }
                    is AuthenticationEvent.EventType.CancelSignIn -> StateResolution(Configured())
                    else -> defaultResolution
                }
                is SigningOut -> when {
                    event.isSignOutEvent() is SignOutEvent.EventType.SignOutLocally -> {
                        val action = authorizationActions.persistCredentials(AmplifyCredential.Empty)
                        StateResolution(StoringCredentials(AmplifyCredential.Empty), listOf(action))
                    }
                    authenticationEvent is AuthenticationEvent.EventType.CancelSignOut -> {
                        StateResolution(SessionEstablished(oldState.amplifyCredential))
                    }
                    else -> defaultResolution
                }
                is FetchingUnAuthSession -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Fetched -> {
                        val amplifyCredential = AmplifyCredential.IdentityPool(
                            authorizationEvent.identityId,
                            authorizationEvent.awsCredentials
                        )
                        val action = authorizationActions.persistCredentials(amplifyCredential)
                        StateResolution(StoringCredentials(amplifyCredential), listOf(action))
                    }
                    is AuthorizationEvent.EventType.ThrowError -> StateResolution(
                        Error(SessionError(authorizationEvent.exception, AmplifyCredential.Empty))
                    )
                    else -> {
                        val resolution = fetchAuthSessionResolver.resolve(oldState.fetchAuthSessionState, event)
                        StateResolution(FetchingUnAuthSession(resolution.newState), resolution.actions)
                    }
                }
                is FetchingAuthSession -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Fetched -> {
                        val amplifyCredential = AmplifyCredential.UserAndIdentityPool(
                            oldState.signedInData,
                            authorizationEvent.identityId,
                            authorizationEvent.awsCredentials
                        )
                        val action = authorizationActions.persistCredentials(amplifyCredential)
                        StateResolution(StoringCredentials(amplifyCredential), listOf(action))
                    }
                    is AuthorizationEvent.EventType.ThrowError -> {
                        val amplifyCredential = AmplifyCredential.UserPool(oldState.signedInData)
                        val action = authorizationActions.persistCredentials(amplifyCredential)
                        StateResolution(StoringCredentials(amplifyCredential), listOf(action))
                    }
                    else -> {
                        val resolution = fetchAuthSessionResolver.resolve(oldState.fetchAuthSessionState, event)
                        StateResolution(
                            FetchingAuthSession(oldState.signedInData, resolution.newState),
                            resolution.actions
                        )
                    }
                }
                is FederatingToIdentityPool -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Fetched -> {
                        val amplifyCredential = AmplifyCredential.IdentityPoolFederated(
                            oldState.federatedToken,
                            authorizationEvent.identityId,
                            authorizationEvent.awsCredentials
                        )
                        val action = authorizationActions.persistCredentials(amplifyCredential)
                        StateResolution(StoringCredentials(amplifyCredential), listOf(action))
                    }
                    is AuthorizationEvent.EventType.ThrowError -> StateResolution(
                        Error(
                            SessionError(
                                authorizationEvent.exception,
                                oldState.existingCredential ?: AmplifyCredential.Empty
                            )
                        )
                    )
                    else -> {
                        val resolution = fetchAuthSessionResolver.resolve(oldState.fetchAuthSessionState, event)
                        StateResolution(
                            FederatingToIdentityPool(
                                oldState.federatedToken,
                                resolution.newState,
                                oldState.existingCredential
                            ),
                            resolution.actions
                        )
                    }
                }
                is RefreshingSession -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.Refreshed -> {
                        val action = authorizationActions.persistCredentials(authorizationEvent.amplifyCredential)
                        StateResolution(StoringCredentials(authorizationEvent.amplifyCredential), listOf(action))
                    }
                    is AuthorizationEvent.EventType.ThrowError -> StateResolution(
                        Error(SessionError(authorizationEvent.exception, oldState.existingCredential))
                    )
                    else -> {
                        val resolution = refreshSessionResolver.resolve(oldState.refreshSessionState, event)
                        StateResolution(
                            RefreshingSession(oldState.existingCredential, resolution.newState),
                            resolution.actions
                        )
                    }
                }
                is DeletingUser -> when (authorizationEvent) {
                    is AuthorizationEvent.EventType.UserDeleted -> {
                        StateResolution(
                            SigningOut(oldState.amplifyCredential),
                            listOf()
                        )
                    }
                    is AuthorizationEvent.EventType.ThrowError -> {
                        StateResolution(SessionEstablished(oldState.amplifyCredential))
                    }
                    else -> {
                        val resolution = deleteUserResolver.resolve(oldState.deleteUserState, event)
                        StateResolution(
                            DeletingUser(resolution.newState, oldState.amplifyCredential), resolution.actions
                        )
                    }
                }
                is SessionEstablished -> when {
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> StateResolution(SigningIn())
                    authenticationEvent is AuthenticationEvent.EventType.SignOutRequested ||
                        authenticationEvent is AuthenticationEvent.EventType.ClearFederationToIdentityPool -> {
                        StateResolution(SigningOut(oldState.amplifyCredential))
                    }
                    deleteUserEvent is DeleteUserEvent.EventType.DeleteUser -> StateResolution(
                        DeletingUser(DeleteUserState.NotStarted(), oldState.amplifyCredential),
                        listOf(authorizationActions.initiateDeleteUser(deleteUserEvent))
                    )
                    authorizationEvent is AuthorizationEvent.EventType.RefreshSession -> {
                        val action = authorizationActions.initiateRefreshSessionAction(
                            authorizationEvent.amplifyCredential
                        )
                        val newState = RefreshingSession(
                            authorizationEvent.amplifyCredential,
                            RefreshSessionState.NotStarted()
                        )
                        StateResolution(newState, listOf(action))
                    }
                    authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                        val action =
                            authorizationActions.initializeFederationToIdentityPool(
                                authorizationEvent.token,
                                authorizationEvent.identityId
                            )
                        val newState = FederatingToIdentityPool(
                            authorizationEvent.token,
                            FetchAuthSessionState.NotStarted(),
                            authorizationEvent.existingCredential
                        )
                        StateResolution(newState, listOf(action))
                    }
                    else -> defaultResolution
                }
                is Error -> when {
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> StateResolution(SigningIn())
                    authenticationEvent is AuthenticationEvent.EventType.SignOutRequested -> StateResolution(
                        SigningOut(AmplifyCredential.Empty)
                    )
                    authorizationEvent is AuthorizationEvent.EventType.FetchUnAuthSession -> {
                        val action = authorizationActions.initializeFetchUnAuthSession()
                        val newState = FetchingUnAuthSession(FetchAuthSessionState.NotStarted())
                        StateResolution(newState, listOf(action))
                    }
                    authorizationEvent is AuthorizationEvent.EventType.RefreshSession -> {
                        val action = authorizationActions.initiateRefreshSessionAction(
                            authorizationEvent.amplifyCredential
                        )
                        val newState = RefreshingSession(
                            authorizationEvent.amplifyCredential,
                            RefreshSessionState.NotStarted()
                        )
                        StateResolution(newState, listOf(action))
                    }
                    authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                        val action =
                            authorizationActions.initializeFederationToIdentityPool(
                                authorizationEvent.token,
                                authorizationEvent.identityId
                            )
                        val newState = FederatingToIdentityPool(
                            authorizationEvent.token,
                            FetchAuthSessionState.NotStarted(),
                            authorizationEvent.existingCredential
                        )
                        StateResolution(newState, listOf(action))
                    }
                    deleteUserEvent is DeleteUserEvent.EventType.DeleteUser -> {
                        StateResolution(
                            DeletingUser(DeleteUserState.NotStarted(), AmplifyCredential.Empty),
                            listOf(authorizationActions.initiateDeleteUser(deleteUserEvent))
                        )
                    }
                    else -> defaultResolution
                }
            }
        }
    }
}
