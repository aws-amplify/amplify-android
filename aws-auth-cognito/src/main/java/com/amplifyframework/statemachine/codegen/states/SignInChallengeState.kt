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
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

sealed class SignInChallengeState : State {
    data class NotStarted(val id: String = "") : SignInChallengeState()
    data class WaitingForAnswer(val id: String = "") : SignInChallengeState()
    data class Verifying(val id: String = "") : SignInChallengeState()
    data class Verified(val id: String = "") : SignInChallengeState()
    data class Error(val error: Exception) : SignInChallengeState()

    override val type = this.toString()

    class Resolver(private val signinChallengeActions: SignInChallengeActions) : StateMachineResolver<SignInChallengeState> {
        override val defaultState = NotStarted()

        private fun asCustomSignInEvent(event: StateMachineEvent): SignInChallengeEvent.EventType? {
            return (event as? SignInChallengeEvent)?.eventType
        }

        override fun resolve(
            oldState: SignInChallengeState,
            event: StateMachineEvent
        ): StateResolution<SignInChallengeState> {
            val defaultResolution = StateResolution(oldState)
            val signInChallengeEvent = asCustomSignInEvent(event)
            return when (oldState) {
                is NotStarted -> {
                    when (signInChallengeEvent) {
                        is SignInChallengeEvent.EventType.WaitForAnswer -> {
                            signinChallengeActions.initiateChallengeAuthAction(signInChallengeEvent)
                            StateResolution(WaitingForAnswer())
                        }
                        else -> defaultResolution
                    }
                }
                is WaitingForAnswer -> {
                    when (signInChallengeEvent) {
                        is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> TODO()
                        is SignInChallengeEvent.EventType.ThrowError -> StateResolution(Error(signInChallengeEvent.exception))
                        else -> defaultResolution
                    }
                }
                is Verifying -> {
                    when (signInChallengeEvent) {
                        is SignInChallengeEvent.EventType.FinalizeSignIn -> TODO()
                        is SignInChallengeEvent.EventType.ThrowError -> StateResolution(Error(signInChallengeEvent.exception))
                        else -> defaultResolution
                    }
                }
                else -> defaultResolution
            }
        }
    }
}
