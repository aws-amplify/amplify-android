/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.auth.cognito.isSignUpEvent
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignUpActions
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.SignUpEvent

internal sealed class SignUpState : State {
    data class NotStarted(val id: String = "") : SignUpState()
    data class InitiatingSignUp(val signUpData: SignUpData) : SignUpState()
    data class AwaitingUserConfirmation(val signUpData: SignUpData, val signUpResult: AuthSignUpResult) : SignUpState()
    data class ConfirmingSignUp(val signUpData: SignUpData) : SignUpState()
    data class SignedUp(val signUpData: SignUpData, val signUpResult: AuthSignUpResult) : SignUpState()
    data class Error(
        val signUpData: SignUpData,
        val exception: Exception,
        var hasNewResponse: Boolean = true
    ) : SignUpState()

    class Resolver(private val signUpActions: SignUpActions) :
        StateMachineResolver<SignUpState> {
        override val defaultState = NotStarted("")

        override fun resolve(oldState: SignUpState, event: StateMachineEvent): StateResolution<SignUpState> {
            val defaultResolution = StateResolution(oldState)
            val signUpEvent = event.isSignUpEvent()

            return when (oldState) {
                is NotStarted, is SignedUp -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        StateResolution(
                            InitiatingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.initiateSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        StateResolution(
                            ConfirmingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.confirmSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ThrowError -> {
                        StateResolution(Error(signUpEvent.signUpData, signUpEvent.exception))
                    }
                    else -> defaultResolution
                }
                is InitiatingSignUp -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        StateResolution(
                            InitiatingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.initiateSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.InitiateSignUpComplete -> {
                        StateResolution(AwaitingUserConfirmation(signUpEvent.signUpData, signUpEvent.signUpResult))
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        StateResolution(
                            ConfirmingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.confirmSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.SignedUp -> {
                        StateResolution(
                            SignedUp(signUpEvent.signUpData, signUpEvent.signUpResult)
                        )
                    }
                    is SignUpEvent.EventType.ThrowError -> {
                        StateResolution(Error(signUpEvent.signUpData, signUpEvent.exception))
                    }
                    else -> defaultResolution
                }
                is AwaitingUserConfirmation -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        StateResolution(
                            InitiatingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.initiateSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        StateResolution(
                            ConfirmingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.confirmSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ThrowError -> {
                        StateResolution(Error(signUpEvent.signUpData, signUpEvent.exception))
                    }
                    else -> defaultResolution
                }
                is ConfirmingSignUp -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        StateResolution(
                            InitiatingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.initiateSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        StateResolution(
                            ConfirmingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.confirmSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.SignedUp -> {
                        StateResolution(SignedUp(signUpEvent.signUpData, signUpEvent.signUpResult))
                    }
                    is SignUpEvent.EventType.ThrowError -> {
                        StateResolution(Error(signUpEvent.signUpData, signUpEvent.exception))
                    }
                    else -> defaultResolution
                }
                is Error -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        StateResolution(
                            InitiatingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.initiateSignUpAction(signUpEvent))
                        )
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        StateResolution(
                            ConfirmingSignUp(signUpEvent.signUpData),
                            listOf(signUpActions.confirmSignUpAction(signUpEvent))
                        )
                    }
                    else -> defaultResolution
                }
            }
        }
    }
}

internal fun SignUpState.getSignUpData(): SignUpData? = when (this) {
    is SignUpState.AwaitingUserConfirmation -> this.signUpData
    is SignUpState.ConfirmingSignUp -> this.signUpData
    is SignUpState.Error -> this.signUpData
    is SignUpState.InitiatingSignUp -> this.signUpData
    is SignUpState.NotStarted -> null
    is SignUpState.SignedUp -> this.signUpData
}
