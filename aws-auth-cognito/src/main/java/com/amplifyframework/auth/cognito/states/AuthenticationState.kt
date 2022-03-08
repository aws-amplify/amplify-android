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

import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.statemachine.*
import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.auth.cognito.data.SignedOutData
import com.amplifyframework.auth.cognito.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions

sealed class AuthenticationState : State {
    data class NotConfigured(val id: String = "") : AuthenticationState()
    data class Configured(override var store: CredentialStoreState?) :
        AuthenticationState()

    data class SigningIn(override var srpSignInState: SRPSignInState?) : AuthenticationState()
    data class SignedIn(val signedInData: SignedInData) : AuthenticationState()
    data class SigningOut(override var signOutState: SignOutState?) : AuthenticationState()
    data class SignedOut(val signedOutData: SignedOutData) : AuthenticationState()
    data class SigningUp(override var signUpState: SignUpState?) : AuthenticationState()
    data class Error(val exception: Exception) : AuthenticationState()

    open var store: CredentialStoreState? = CredentialStoreState.NotConfigured()
    open var srpSignInState: SRPSignInState? = SRPSignInState.NotStarted()
    open var signUpState: SignUpState? = SignUpState.NotStarted()
    open var signOutState: SignOutState? = SignOutState.NotStarted()

    class Resolver(
        private val storeResolver: StateMachineResolver<CredentialStoreState>,
        private val signUpResolver: StateMachineResolver<SignUpState>,
        private val srpSignInResolver: StateMachineResolver<SRPSignInState>,
        private val signOutResolver: StateMachineResolver<SignOutState>,
        private val authenticationActions: AuthenticationActions
    ) :
        StateMachineResolver<AuthenticationState> {
        override val defaultState = NotConfigured()

        private fun asAuthenticationEvent(event: StateMachineEvent): AuthenticationEvent.EventType? {
            return (event as? AuthenticationEvent)?.eventType
        }

        override fun resolve(
            oldState: AuthenticationState,
            event: StateMachineEvent
        ): StateResolution<AuthenticationState> {
            val resolution = resolveAuthNEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.store?.let { storeResolver.resolve(it, event) }?.let {
                builder.storeState = it.newState
                actions += it.actions
            }

            oldState.signUpState?.let { signUpResolver.resolve(it, event) }?.let {
                builder.signUpState = it.newState
                actions += it.actions
            }

            oldState.srpSignInState?.let { srpSignInResolver.resolve(it, event) }?.let {
                builder.srpSignInState = it.newState
                actions += it.actions
            }

            oldState.signOutState?.let { signOutResolver.resolve(it, event) }?.let {
                builder.signOutState = it.newState
                actions += it.actions
            }

            return StateResolution(builder.build(), actions)
        }

        private fun resolveAuthNEvent(
            oldState: AuthenticationState,
            event: StateMachineEvent
        ): StateResolution<AuthenticationState> {
            val authenticationEvent = asAuthenticationEvent(event)
            val signUpEvent = (event as? SignUpEvent)?.eventType
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.Configure -> {
                        val action = authenticationActions.configureAuthenticationAction(
                            authenticationEvent
                        )
                        val newState = Configured(oldState.store)
                        StateResolution(newState, listOf(action))
                    }
                    else -> defaultResolution
                }
                is Configured -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedIn -> StateResolution(
                        SignedIn(
                            authenticationEvent.signedInData
                        )
                    )
                    is AuthenticationEvent.EventType.InitializedSignedOut -> StateResolution(
                        SignedOut(authenticationEvent.signedOutData)
                    )
                    else -> defaultResolution
                }
                is SigningIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedIn -> StateResolution(
                        SignedIn(
                            authenticationEvent.signedInData
                        )
                    )
                    is AuthenticationEvent.EventType.CancelSignIn -> StateResolution(
                        SignedOut(
                            SignedOutData()
                        )
                    )
                    else -> defaultResolution
                }
                is SigningUp -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.resetSignUp -> StateResolution(
                        SignedOut(
                            SignedOutData()
                        )
                    )
                    else -> defaultResolution
                }
                is SignedIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignOutRequested -> {
                        val action =
                            authenticationActions.initiateSignOutAction(
                                authenticationEvent,
                                oldState.signedInData
                            )
                        val newState = SigningOut(oldState.signOutState)
                        StateResolution(newState, listOf(action))
                    }
                    else -> defaultResolution
                }
                is SigningOut -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedOut -> StateResolution(
                        SignedOut(authenticationEvent.signedOutData)
                    )
                    else -> defaultResolution
                }
                is SignedOut -> when {
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> {
                        val action =
                            authenticationActions.initiateSRPSignInAction(authenticationEvent)
                        val newState = SigningIn(oldState.srpSignInState)
                        StateResolution(newState, listOf(action))
                    }
                    signUpEvent is SignUpEvent.EventType.InitiateSignUp || signUpEvent is SignUpEvent.EventType.ConfirmSignUp -> StateResolution(
                        SigningUp(oldState.signUpState)
                    )
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val authNState: AuthenticationState) :
        com.amplifyframework.statemachine.Builder<AuthenticationState> {
        var storeState: CredentialStoreState? = null
        var srpSignInState: SRPSignInState? = null
        var signUpState: SignUpState? = null
        var signOutState: SignOutState? = null

        override fun build(): AuthenticationState = when (authNState) {
            is NotConfigured -> NotConfigured()
            is Configured -> Configured(storeState)
            is SignedIn -> SignedIn(authNState.signedInData)
            is SignedOut -> SignedOut(authNState.signedOutData)
            is SigningIn -> SigningIn(srpSignInState)
            is SigningOut -> SigningOut(signOutState)
            is SigningUp -> SigningUp(signUpState)
            is Error -> authNState
        }
    }
}