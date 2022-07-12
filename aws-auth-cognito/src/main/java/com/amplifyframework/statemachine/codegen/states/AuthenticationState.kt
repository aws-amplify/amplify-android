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
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent

sealed class AuthenticationState : State {
    data class NotConfigured(val id: String = "") : AuthenticationState()
    data class Configured(val id: String = "") : AuthenticationState()
    data class SigningIn(override var srpSignInState: SRPSignInState?) : AuthenticationState()
    data class SignedIn(val signedInData: SignedInData) : AuthenticationState()
    data class SigningOut(override var signOutState: SignOutState?) : AuthenticationState()
    data class SignedOut(val signedOutData: SignedOutData) : AuthenticationState()
    data class Error(val exception: Exception) : AuthenticationState()

    open var srpSignInState: SRPSignInState? = SRPSignInState.NotStarted()
    open var signOutState: SignOutState? = SignOutState.NotStarted()
    open var deleteUserState: DeleteUserState? = DeleteUserState.NotStarted()

    class Resolver(
        private val srpSignInResolver: StateMachineResolver<SRPSignInState>,
        private val signOutResolver: StateMachineResolver<SignOutState>,
        private val authenticationActions: AuthenticationActions
    ) :
        StateMachineResolver<AuthenticationState> {
        override val defaultState = NotConfigured()

        private fun asAuthenticationEvent(event: StateMachineEvent): AuthenticationEvent.EventType? {
            return (event as? AuthenticationEvent)?.eventType
        }

        private fun asDeleteUserEvent(event: StateMachineEvent): DeleteUserEvent.EventType? {
            return (event as? DeleteUserEvent)?.eventType
        }

        override fun resolve(
            oldState: AuthenticationState,
            event: StateMachineEvent
        ): StateResolution<AuthenticationState> {
            val resolution = resolveAuthNEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

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
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotConfigured -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.Configure -> {
                        val action = authenticationActions.configureAuthenticationAction(authenticationEvent)
                        StateResolution(Configured(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is Configured -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedIn -> StateResolution(
                        SignedIn(authenticationEvent.signedInData)
                    )
                    is AuthenticationEvent.EventType.InitializedSignedOut -> StateResolution(
                        SignedOut(authenticationEvent.signedOutData)
                    )
                    else -> defaultResolution
                }
                is SigningIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.InitializedSignedIn -> StateResolution(
                        SignedIn(authenticationEvent.signedInData)
                    )
                    is AuthenticationEvent.EventType.CancelSignIn -> StateResolution(SignedOut(SignedOutData()))
                    else -> defaultResolution
                }
                is SignedIn -> when (authenticationEvent) {
                    is AuthenticationEvent.EventType.SignOutRequested -> {
                        val action =
                            authenticationActions.initiateSignOutAction(authenticationEvent, oldState.signedInData)
                        StateResolution(SigningOut(oldState.signOutState), listOf(action))
                    }
                    else -> defaultResolution
                }
                is SigningOut -> when {
                    authenticationEvent is AuthenticationEvent.EventType.InitializedSignedOut -> {
                        StateResolution(
                            SignedOut(authenticationEvent.signedOutData)
                        )
                    }
                    else -> defaultResolution
                }
                is SignedOut -> when {
                    authenticationEvent is AuthenticationEvent.EventType.SignInRequested -> {
                        val action = authenticationActions.initiateSRPSignInAction(authenticationEvent)
                        StateResolution(SigningIn(oldState.srpSignInState), listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val authNState: AuthenticationState) :
        com.amplifyframework.statemachine.Builder<AuthenticationState> {
        var srpSignInState: SRPSignInState? = null
        var signOutState: SignOutState? = null

        override fun build(): AuthenticationState = when (authNState) {
            is SignedIn -> SignedIn(authNState.signedInData)
            is SignedOut -> SignedOut(authNState.signedOutData)
            is SigningIn -> SigningIn(srpSignInState)
            is SigningOut -> SigningOut(signOutState)
            else -> authNState
        }
    }
}
