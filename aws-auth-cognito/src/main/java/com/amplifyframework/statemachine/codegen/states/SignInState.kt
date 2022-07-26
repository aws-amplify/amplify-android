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
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.events.SignInEvent

sealed class SignInState : State {
    data class NotStarted(val id: String = "") : SignInState()
    data class SigningInWithSRP(override var srpSignInState: SRPSignInState?) : SignInState()
    data class SigningInWithHostedUI(override var hostedUISignInState: HostedUISignInState?) : SignInState()
    data class SigningInWithCustom(val id: String = "") : SignInState()
    data class SigningInWithSRPCustom(val id: String = "") : SignInState()
    data class ResolvingSMSChallenge(val id: String = "") : SignInState()
    data class ResolvingCustomChallenge(val id: String = "") : SignInState()
    data class Done(val id: String = "") : SignInState()
    data class Error(val exception: Exception) : SignInState()

    open var srpSignInState: SRPSignInState? = SRPSignInState.NotStarted()
    open var hostedUISignInState: HostedUISignInState? = HostedUISignInState.NotStarted()

    class Resolver(
        private val srpSignInResolver: StateMachineResolver<SRPSignInState>,
        private val hostedUISignInResolver: StateMachineResolver<HostedUISignInState>,
        private val signInActions: SignInActions
    ) :
        StateMachineResolver<SignInState> {
        override val defaultState = NotStarted()

        private fun asSignInEvent(event: StateMachineEvent): SignInEvent.EventType? {
            return (event as? SignInEvent)?.eventType
        }

        override fun resolve(oldState: SignInState, event: StateMachineEvent): StateResolution<SignInState> {
            val resolution = resolveSignInEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.srpSignInState?.let { srpSignInResolver.resolve(it, event) }?.let {
                builder.srpSignInState = it.newState
                actions += it.actions
            }

            oldState.hostedUISignInState?.let { hostedUISignInResolver.resolve(it, event) }?.let {
                builder.hostedUISignInState = it.newState
                actions += it.actions
            }

            return StateResolution(builder.build(), actions)
        }

        private fun resolveSignInEvent(
            oldState: SignInState,
            event: StateMachineEvent
        ): StateResolution<SignInState> {
            val signInEvent = asSignInEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (signInEvent) {
                    is SignInEvent.EventType.InitiateSignInWithSRP -> StateResolution(
                        SigningInWithSRP(oldState.srpSignInState),
                        listOf(signInActions.startSRPAuthAction(signInEvent))
                    )
                    is SignInEvent.EventType.InitiateHostedUISignIn -> StateResolution(
                        SigningInWithHostedUI(HostedUISignInState.NotStarted()),
                        listOf(signInActions.startHostedUIAuthAction(signInEvent))
                    )
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is SigningInWithSRP -> when (signInEvent) {
                    is SignInEvent.EventType.ReceivedSMSChallenge -> StateResolution(ResolvingSMSChallenge())
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is SigningInWithHostedUI -> when (signInEvent) {
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is ResolvingSMSChallenge -> when (signInEvent) {
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is Error -> defaultResolution
                else -> defaultResolution
            }
        }
    }

    class Builder(private val signInState: SignInState) :
        com.amplifyframework.statemachine.Builder<SignInState> {
        var srpSignInState: SRPSignInState? = null
        var hostedUISignInState: HostedUISignInState? = null

        override fun build(): SignInState = when (signInState) {
            is SigningInWithSRP -> SigningInWithSRP(srpSignInState)
            is SigningInWithHostedUI -> SigningInWithHostedUI(hostedUISignInState)
            else -> signInState
        }
    }
}
