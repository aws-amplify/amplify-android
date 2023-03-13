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

import com.amplifyframework.auth.cognito.isAuthenticationEvent
import com.amplifyframework.auth.cognito.isAuthorizationEvent
import com.amplifyframework.auth.cognito.isSignOutEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

internal sealed class AuthenticationState : State {
    data class NotConfigured(val id: String = "") : AuthenticationState()
    data class Configured(val id: String = "") : AuthenticationState()
    data class SigningIn(var signInState: SignInState = SignInState.NotStarted()) : AuthenticationState()
    data class SignedIn(val signedInData: SignedInData, val deviceMetadata: DeviceMetadata) : AuthenticationState()
    data class SigningOut(var signOutState: SignOutState = SignOutState.NotStarted()) : AuthenticationState()
    data class SignedOut(val signedOutData: SignedOutData) : AuthenticationState()
    data class FederatingToIdentityPool(val id: String = "") : AuthenticationState()
    data class FederatedToIdentityPool(val id: String = "") : AuthenticationState()
    data class Error(val exception: Exception) : AuthenticationState()

    class Resolver(
        private val signInResolver: StateMachineResolver<SignInState>,
        private val signOutResolver: StateMachineResolver<SignOutState>,
        private val authenticationActions: AuthenticationActions
    ) :
        StateMachineResolver<AuthenticationState> {
        override val defaultState = NotConfigured()

        override fun resolve(
            oldState: AuthenticationState,
            event: StateMachineEvent
        ): StateResolution<AuthenticationState> {
            val authenticationEvent = event.isAuthenticationEvent()
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> {
                    val authorizationEvent = event.isAuthorizationEvent()
                    when {
                        authenticationEvent is AuthenticationEvent.EventType.Configure -> {
                            val action = authenticationActions.configureAuthenticationAction(authenticationEvent)
                            StateResolution(Configured(), listOf(action))
                        }
                        authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                            StateResolution(FederatingToIdentityPool())
                        }
                        else -> defaultResolution
                    }
                }
                is Configured -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedIn -> StateResolution(
                        SignedIn(authenticationEvent.signedInData, authenticationEvent.deviceMetadata)
                    )
                    is AuthenticationEvent.EventType.InitializedFederated -> StateResolution(
                        FederatedToIdentityPool()
                    )
                    is AuthenticationEvent.EventType.InitializedSignedOut -> StateResolution(
                        SignedOut(authenticationEvent.signedOutData)
                    )
                    else -> defaultResolution
                }
                is SigningIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignInCompleted -> StateResolution(
                        SignedIn(authenticationEvent.signedInData, authenticationEvent.deviceMetadata)
                    )
                    is AuthenticationEvent.EventType.CancelSignIn -> {
                        if (authenticationEvent.error != null) {
                            StateResolution(Error(authenticationEvent.error))
                        }
                        StateResolution(SignedOut(SignedOutData()))
                    }
                    else -> {
                        val resolution = signInResolver.resolve(oldState.signInState, event)
                        StateResolution(SigningIn(resolution.newState), resolution.actions)
                    }
                }
                is SignedIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignOutRequested -> {
                        val action =
                            authenticationActions.initiateSignOutAction(authenticationEvent, oldState.signedInData)
                        StateResolution(SigningOut(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is SigningOut -> {
                    val signOutEvent = event.isSignOutEvent()
                    when {
                        signOutEvent is SignOutEvent.EventType.SignedOutSuccess -> {
                            StateResolution(SignedOut(signOutEvent.signedOutData))
                        }
                        authenticationEvent is AuthenticationEvent.EventType.CancelSignOut -> {
                            StateResolution(
                                SignedIn(authenticationEvent.signedInData, authenticationEvent.deviceMetadata)
                            )
                        }
                        else -> {
                            val resolution = signOutResolver.resolve(oldState.signOutState, event)
                            StateResolution(SigningOut(resolution.newState), resolution.actions)
                        }
                    }
                }
                is SignedOut -> {
                    val authorizationEvent = event.isAuthorizationEvent()
                    when {
                        authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> {
                            val action = authenticationActions.initiateSignInAction(authenticationEvent)
                            StateResolution(SigningIn(), listOf(action))
                        }
                        authenticationEvent is AuthenticationEvent.EventType.SignOutRequested -> {
                            val action = authenticationActions
                                .initiateSignOutAction(authenticationEvent, null)
                            StateResolution(SigningOut(), listOf(action))
                        }
                        authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                            StateResolution(FederatingToIdentityPool())
                        }
                        else -> defaultResolution
                    }
                }
                is FederatingToIdentityPool -> {
                    when (val authorizationEvent = event.isAuthorizationEvent()) {
                        is AuthorizationEvent.EventType.Fetched -> {
                            StateResolution(FederatedToIdentityPool())
                        }
                        is AuthorizationEvent.EventType.ThrowError -> {
                            StateResolution(Error(authorizationEvent.exception))
                        }
                        else -> defaultResolution
                    }
                }
                is FederatedToIdentityPool -> {
                    val authorizationEvent = event.isAuthorizationEvent()
                    when {
                        authenticationEvent is AuthenticationEvent.EventType.ClearFederationToIdentityPool -> {
                            val action = authenticationActions.initiateSignOutAction(
                                AuthenticationEvent.EventType.SignOutRequested(SignOutData()),
                                null
                            )
                            StateResolution(SigningOut(), listOf(action))
                        }
                        authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                            StateResolution(FederatingToIdentityPool())
                        }
                        else -> defaultResolution
                    }
                }
                is Error -> {
                    val authorizationEvent = event.isAuthorizationEvent()
                    when {
                        authorizationEvent is AuthorizationEvent.EventType.StartFederationToIdentityPool -> {
                            StateResolution(FederatingToIdentityPool())
                        }
                        authenticationEvent is AuthenticationEvent.EventType.ClearFederationToIdentityPool -> {
                            val action = authenticationActions.initiateSignOutAction(
                                AuthenticationEvent.EventType.SignOutRequested(SignOutData()),
                                null
                            )
                            StateResolution(SigningOut(), listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
            }
        }
    }
}
