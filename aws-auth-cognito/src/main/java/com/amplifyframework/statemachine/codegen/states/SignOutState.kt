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
import com.amplifyframework.auth.cognito.isSignOutEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignOutActions
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

sealed class SignOutState : State {
    data class NotStarted(val id: String = "") : SignOutState()
    data class SigningOutLocally(val signedInData: SignedInData) : SignOutState()
    data class SigningOutGlobally(val id: String = "") : SignOutState()
    data class RevokingToken(val id: String = "") : SignOutState()
    data class SignedOut(val signedOutData: SignedOutData) : SignOutState()
    data class Error(val exception: Exception) : SignOutState()

    class Resolver(private val signOutActions: SignOutActions) :
        StateMachineResolver<SignOutState> {
        override val defaultState = NotStarted("")

        override fun resolve(oldState: SignOutState, event: StateMachineEvent): StateResolution<SignOutState> {
            val defaultResolution = StateResolution(oldState)
            val signOutEvent = event.isSignOutEvent()
            return when (oldState) {
                is NotStarted -> when (signOutEvent) {
                    is SignOutEvent.EventType.SignOutLocally -> {
                        val action = signOutActions.localSignOutAction(signOutEvent)
                        StateResolution(SigningOutLocally(signOutEvent.signedInData), listOf(action))
                    }
                    is SignOutEvent.EventType.SignOutGlobally -> {
                        val action = signOutActions.globalSignOutAction(signOutEvent)
                        StateResolution(SigningOutGlobally(), listOf(action))
                    }
                    is SignOutEvent.EventType.RevokeToken -> {
                        val action = signOutActions.revokeTokenAction(signOutEvent)
                        StateResolution(RevokingToken(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is SigningOutLocally -> when (event.isAuthEvent()) {
                    is AuthEvent.EventType.ReceivedCachedCredentials -> {
                        val newState = SignedOut(SignedOutData(oldState.signedInData.username))
                        StateResolution(newState)
                    }
                    is AuthEvent.EventType.CachedCredentialsFailed -> StateResolution(
                        Error(Exception("Failed clearing store"))
                    )
                    else -> defaultResolution
                }
                is SigningOutGlobally -> when (signOutEvent) {
                    is SignOutEvent.EventType.RevokeToken -> {
                        val action = signOutActions.revokeTokenAction(signOutEvent)
                        StateResolution(RevokingToken(), listOf(action))
                    }
                    is SignOutEvent.EventType.SignOutLocally -> {
                        val action = signOutActions.localSignOutAction(signOutEvent)
                        StateResolution(SigningOutLocally(signOutEvent.signedInData), listOf(action))
                    }
                    else -> defaultResolution
                }
                is RevokingToken -> when (signOutEvent) {
                    is SignOutEvent.EventType.SignOutLocally -> {
                        val action = signOutActions.localSignOutAction(signOutEvent)
                        StateResolution(SigningOutLocally(signOutEvent.signedInData), listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
