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
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

internal sealed class SignInChallengeState : State {
    data class NotStarted(val id: String = "") : SignInChallengeState()
    data class WaitingForAnswer(
        val challenge: AuthChallenge,
        val signInMethod: SignInMethod
    ) : SignInChallengeState()
    data class Verifying(
        val id: String = "",
        val signInMethod: SignInMethod
    ) : SignInChallengeState()
    data class Verified(val id: String = "") : SignInChallengeState()
    data class Error(
        val exception: Exception,
        val challenge: AuthChallenge,
        val signInMethod: SignInMethod
    ) : SignInChallengeState()

    class Resolver(private val challengeActions: SignInChallengeActions) : StateMachineResolver<SignInChallengeState> {
        override val defaultState: SignInChallengeState = NotStarted()

        private fun asSignInChallengeEvent(event: StateMachineEvent): SignInChallengeEvent.EventType? =
            (event as? SignInChallengeEvent)?.eventType

        override fun resolve(
            oldState: SignInChallengeState,
            event: StateMachineEvent
        ): StateResolution<SignInChallengeState> {
            val defaultResolution = StateResolution(oldState)
            val challengeEvent = asSignInChallengeEvent(event)
            return when (oldState) {
                is NotStarted -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.WaitForAnswer -> {
                        StateResolution(WaitingForAnswer(challengeEvent.challenge, challengeEvent.signInMethod))
                    }
                    else -> defaultResolution
                }
                is WaitingForAnswer -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.WaitForAnswer -> {
                        /**
                         * This sends out a second WaitingForAnswer because it requires an additional user-response
                         * before calling RespondToAuth. e.g. the first WaitingForAnswer asks the user to select
                         * a first-factor challenge and the user selects password. The user needs to supply the password
                         * so that the answer *and* the password can be sent in one RespondToAuth call.
                         **/
                        StateResolution(
                            WaitingForAnswer(
                                challenge = AuthChallenge(
                                    challengeName = challengeEvent.challenge.challengeName,
                                    username = oldState.challenge.username,
                                    session = oldState.challenge.session,
                                    parameters = oldState.challenge.parameters
                                ),
                                signInMethod = oldState.signInMethod
                            )
                        )
                    }
                    is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> {
                        val action = challengeActions.verifyChallengeAuthAction(
                            challengeEvent.answer,
                            challengeEvent.metadata,
                            challengeEvent.userAttributes,
                            oldState.challenge,
                            oldState.signInMethod
                        )
                        StateResolution(
                            Verifying(
                                oldState.challenge.challengeName,
                                oldState.signInMethod
                            ),
                            listOf(action)
                        )
                    }
                    else -> defaultResolution
                }
                is Verifying -> when (challengeEvent) {
                    is SignInChallengeEvent.EventType.Verified -> StateResolution(Verified())
                    is SignInChallengeEvent.EventType.ThrowError -> {
                        StateResolution(
                            Error(
                                challengeEvent.exception,
                                challengeEvent.challenge,
                                oldState.signInMethod
                            ),
                            listOf()
                        )
                    }
                    is SignInChallengeEvent.EventType.RetryVerifyChallengeAnswer -> {
                        val action = challengeActions.verifyChallengeAuthAction(
                            challengeEvent.answer,
                            challengeEvent.metadata,
                            challengeEvent.userAttributes,
                            challengeEvent.authChallenge,
                            oldState.signInMethod
                        )
                        StateResolution(
                            Verifying(
                                challengeEvent.authChallenge.challengeName,
                                oldState.signInMethod
                            ),
                            listOf(action)
                        )
                    }
                    is SignInChallengeEvent.EventType.WaitForAnswer -> {
                        StateResolution(
                            WaitingForAnswer(
                                challengeEvent.challenge,
                                oldState.signInMethod
                            ),
                            listOf()
                        )
                    }

                    else -> defaultResolution
                }
                is Error -> {
                    when (challengeEvent) {
                        is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> {
                            val action = challengeActions.verifyChallengeAuthAction(
                                challengeEvent.answer,
                                challengeEvent.metadata,
                                challengeEvent.userAttributes,
                                oldState.challenge,
                                oldState.signInMethod
                            )
                            StateResolution(
                                Verifying(
                                    oldState.challenge.challengeName,
                                    oldState.signInMethod
                                ),
                                listOf(action)
                            )
                        }
                        is SignInChallengeEvent.EventType.WaitForAnswer -> {
                            StateResolution(
                                WaitingForAnswer(
                                    challengeEvent.challenge,
                                    oldState.signInMethod
                                ),
                                listOf()
                            )
                        }
                        else -> defaultResolution
                    }
                }
                else -> defaultResolution
            }
        }
    }
}
