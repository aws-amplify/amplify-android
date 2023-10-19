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
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.events.SRPEvent

internal sealed class SRPSignInState : State {
    data class NotStarted(val id: String = "") : SRPSignInState()
    data class InitiatingSRPA(val id: String = "") : SRPSignInState()
    data class RespondingPasswordVerifier(val id: String = "") : SRPSignInState()
    data class SignedIn(val id: String = "") : SRPSignInState()
    data class Cancelling(val id: String = "") : SRPSignInState()
    data class Error(val exception: Exception) : SRPSignInState()

    class Resolver(private val srpActions: SRPActions) : StateMachineResolver<SRPSignInState> {
        override val defaultState = NotStarted("")

        private fun asSRPEvent(event: StateMachineEvent): SRPEvent.EventType? {
            return (event as? SRPEvent)?.eventType
        }

        override fun resolve(oldState: SRPSignInState, event: StateMachineEvent): StateResolution<SRPSignInState> {
            val srpEvent = asSRPEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (srpEvent) {
                    is SRPEvent.EventType.InitiateSRP -> {
                        val action = srpActions.initiateSRPAuthAction(srpEvent)
                        StateResolution(InitiatingSRPA(), listOf(action))
                    }
                    is SRPEvent.EventType.InitiateSRPWithCustom -> {
                        val action = srpActions.initiateSRPWithCustomAuthAction(srpEvent)
                        StateResolution(InitiatingSRPA(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is InitiatingSRPA -> when (srpEvent) {
                    is SRPEvent.EventType.RespondPasswordVerifier -> {
                        val action = srpActions.verifyPasswordSRPAction(
                            srpEvent.challengeParameters, srpEvent.metadata, srpEvent.session
                        )
                        StateResolution(RespondingPasswordVerifier(), listOf(action))
                    }
                    is SRPEvent.EventType.ThrowAuthError -> StateResolution(Error(srpEvent.exception))
                    is SRPEvent.EventType.CancelSRPSignIn -> StateResolution(Cancelling())
                    else -> defaultResolution
                }
                is RespondingPasswordVerifier -> when (srpEvent) {
                    is SRPEvent.EventType.RetryRespondPasswordVerifier -> {
                        val action = srpActions.verifyPasswordSRPAction(
                            srpEvent.challengeParameters, srpEvent.metadata, srpEvent.session
                        )
                        StateResolution(RespondingPasswordVerifier(), listOf(action))
                    }
                    is SRPEvent.EventType.ThrowPasswordVerifierError -> StateResolution(Error(srpEvent.exception))
                    is SRPEvent.EventType.CancelSRPSignIn -> StateResolution(Cancelling())
                    else -> defaultResolution
                }
                is Cancelling -> when (srpEvent) {
                    is SRPEvent.EventType.Reset -> StateResolution(NotStarted())
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
