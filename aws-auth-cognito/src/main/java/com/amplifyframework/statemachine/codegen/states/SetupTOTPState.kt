/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.statemachine.codegen.actions.SetupTOTPActions
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent

internal sealed class SetupTOTPState : State {
    data class NotStarted(val id: String = "") : SetupTOTPState()
    data class SetupTOTP(val signInTOTPSetupData: SignInTOTPSetupData) : SetupTOTPState()
    data class WaitingForAnswer(val signInTOTPSetupData: SignInTOTPSetupData) : SetupTOTPState()
    data class Verifying(val code: String, val username: String, val session: String?) : SetupTOTPState()
    data class RespondingToAuthChallenge(val username: String, val session: String?) : SetupTOTPState()
    data class Success(val id: String = "") : SetupTOTPState()
    data class Error(val exception: Exception) : SetupTOTPState()

    class Resolver(private val setupTOTPActions: SetupTOTPActions) : StateMachineResolver<SetupTOTPState> {
        override val defaultState = NotStarted("default")

        override fun resolve(oldState: SetupTOTPState, event: StateMachineEvent): StateResolution<SetupTOTPState> {
            val defaultResolution = StateResolution(oldState)
            val challengeEvent = (event as? SetupTOTPEvent)?.eventType
            return when (oldState) {
                is NotStarted -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.SetupTOTP -> {
                        StateResolution(
                            SetupTOTP(challengeEvent.totpSetupDetails),
                            listOf(setupTOTPActions.initiateTOTPSetup(challengeEvent))
                        )
                    }

                    is SetupTOTPEvent.EventType.ThrowAuthError -> StateResolution(
                        Error(challengeEvent.exception)
                    )

                    else -> defaultResolution
                }

                is SetupTOTP -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.WaitForAnswer -> {
                        StateResolution(WaitingForAnswer(challengeEvent.totpSetupDetails))
                    }

                    is SetupTOTPEvent.EventType.ThrowAuthError -> StateResolution(
                        Error(challengeEvent.exception)
                    )

                    else -> defaultResolution
                }

                is WaitingForAnswer -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.VerifyChallengeAnswer -> {
                        StateResolution(
                            Verifying(
                                challengeEvent.answer,
                                oldState.signInTOTPSetupData.username,
                                oldState.signInTOTPSetupData.session
                            ),
                            listOf(setupTOTPActions.verifyChallengeAnswer(challengeEvent))
                        )
                    }

                    is SetupTOTPEvent.EventType.ThrowAuthError -> StateResolution(
                        Error(challengeEvent.exception)
                    )

                    else -> defaultResolution
                }

                is Verifying -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.RespondToAuthChallenge -> {
                        StateResolution(
                            RespondingToAuthChallenge(oldState.username, oldState.session),
                            listOf(
                                setupTOTPActions.respondToAuthChallenge(
                                    challengeEvent
                                )
                            )
                        )
                    }

                    is SetupTOTPEvent.EventType.ThrowAuthError -> StateResolution(
                        Error(challengeEvent.exception)
                    )

                    else -> defaultResolution
                }

                is RespondingToAuthChallenge -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.Verified -> {
                        StateResolution(
                            Success()
                        )
                    }

                    is SetupTOTPEvent.EventType.ThrowAuthError -> StateResolution(
                        Error(challengeEvent.exception)
                    )

                    else -> defaultResolution
                }

                is Error -> when (challengeEvent) {
                    is SetupTOTPEvent.EventType.VerifyChallengeAnswer -> {
                        StateResolution(
                            // TODO: Fix this
                            Verifying(challengeEvent.answer, "", null),
                            listOf(setupTOTPActions.verifyChallengeAnswer(challengeEvent))
                        )
                    }

                    else -> defaultResolution
                }

                else -> defaultResolution
            }
        }
    }
}
