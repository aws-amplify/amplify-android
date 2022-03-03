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

import com.amplifyframework.auth.cognito.events.SignUpEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignUpActions

sealed class SignUpState : State {
    data class NotStarted(val id: String = "") : SignUpState()
    data class InitiatingSigningUp(val id: String = "") : SignUpState()
    data class SigningUpInitiated(val id: String = "") : SignUpState()
    data class ConfirmingSignUp(val id: String = "") : SignUpState()
    data class SignedUp(val id: String = "") : SignUpState()
    data class Error(val exception: Exception) : SignUpState()

    class Resolver(private val signUpActions: SignUpActions) : StateMachineResolver<SignUpState> {
        override val defaultState = NotStarted("")

        private fun asSignUpEvent(event: StateMachineEvent): SignUpEvent.EventType? {
            return (event as? SignUpEvent)?.eventType
        }

        override fun resolve(
            oldState: SignUpState,
            event: StateMachineEvent
        ): StateResolution<SignUpState> {
            val defaultResolution = StateResolution(oldState)
            val signUpEvent = asSignUpEvent(event)
            return when (oldState) {
                is NotStarted, is SigningUpInitiated -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUp -> {
                        val action = signUpActions.startSignUpAction(signUpEvent)
                        val newState = InitiatingSigningUp()
                        return StateResolution(newState, listOf(action))
                    }
                    is SignUpEvent.EventType.ConfirmSignUp -> {
                        val action = signUpActions.confirmSignUpAction(signUpEvent)
                        val newState = ConfirmingSignUp()
                        return StateResolution(newState, listOf(action))
                    }
                    else -> defaultResolution
                }
                is InitiatingSigningUp -> when (signUpEvent) {
                    is SignUpEvent.EventType.InitiateSignUpSuccess -> StateResolution(
                        SigningUpInitiated()
                    )
                    is SignUpEvent.EventType.InitiateSignUpFailure -> {
                        val action = signUpActions.cancelSignUpAction()
                        StateResolution(Error(signUpEvent.exception), listOf(action))
                    }
                    else -> defaultResolution
                }
                is ConfirmingSignUp -> when (signUpEvent) {
                    is SignUpEvent.EventType.ConfirmSignUpSuccess -> StateResolution(SignedUp())
                    is SignUpEvent.EventType.ConfirmSignUpFailure -> {
                        val action = signUpActions.cancelSignUpAction()
                        StateResolution(Error(signUpEvent.exception), listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}