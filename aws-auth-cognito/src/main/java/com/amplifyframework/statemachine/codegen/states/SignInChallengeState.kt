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
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

internal sealed class SignInChallengeState : State {
    data class NotStarted(val id: String = "") : SignInChallengeState()
    data class WaitingForAnswer(
        val challenge: AuthChallenge,
        var hasNewResponse: Boolean = false
    ) : SignInChallengeState()
    data class Verifying(val id: String = "") : SignInChallengeState()
    data class Verified(val id: String = "") : SignInChallengeState()
    data class Error(
        val exception: Exception,
        val challenge: AuthChallenge,
        var hasNewResponse: Boolean = false
    ) : SignInChallengeState()

    class Resolver(private val challengeActions: SignInChallengeActions) : StateMachineResolver<SignInChallengeState> {
        override val defaultState: SignInChallengeState = NotStarted()

        private fun asSignInChallengeEvent(event: StateMachineEvent): SignInChallengeEvent.EventType? {
            return (event as? SignInChallengeEvent)?.eventType
        }

        override fun resolve(
            oldState: SignInChallengeState,
            event: StateMachineEvent
        ): StateResolution<SignInChallengeState> {
            val defaultResolution = StateResolution(oldState)
            val challengeEvent = asSignInChallengeEvent(event)
            return when (oldState) {
                is NotStarted -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.WaitForAnswer -> {
                        StateResolution(WaitingForAnswer(challengeEvent.challenge))
                    }
                    else -> defaultResolution
                }
                is WaitingForAnswer -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> {
                        val action = challengeActions.verifyChallengeAuthAction(
                            challengeEvent.answer, challengeEvent.metadata, oldState.challenge
                        )
                        StateResolution(Verifying(oldState.challenge.challengeName), listOf(action))
                    }
                    else -> defaultResolution
                }
                is Verifying -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.Verified -> StateResolution(Verified())
                    is SignInChallengeEvent.EventType.ThrowError -> {
                        StateResolution(
                            Error(
                                challengeEvent.exception, challengeEvent.challenge, true
                            ),
                            listOf()
                        )
                    }
                    is SignInChallengeEvent.EventType.RetryVerifyChallengeAnswer -> {
                        val action = challengeActions.verifyChallengeAuthAction(
                            challengeEvent.answer, challengeEvent.metadata, challengeEvent.authChallenge
                        )
                        StateResolution(Verifying(challengeEvent.authChallenge.challengeName), listOf(action))
                    }
                    is SignInChallengeEvent.EventType.WaitForAnswer -> {
                        StateResolution(WaitingForAnswer(challengeEvent.challenge, true), listOf())
                    }

                    else -> defaultResolution
                }
                is Error -> {
                    when (challengeEvent) {
                        is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> {
                            val action = challengeActions.verifyChallengeAuthAction(
                                challengeEvent.answer, challengeEvent.metadata, oldState.challenge
                            )
                            StateResolution(Verifying(oldState.challenge.challengeName), listOf(action))
                        }
                        is SignInChallengeEvent.EventType.WaitForAnswer -> {
                            StateResolution(WaitingForAnswer(challengeEvent.challenge), listOf())
                        }
                        else -> defaultResolution
                    }
                }
                else -> defaultResolution
            }
        }
    }
}
