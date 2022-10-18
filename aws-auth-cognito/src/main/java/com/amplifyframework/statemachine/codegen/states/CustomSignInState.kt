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
import com.amplifyframework.statemachine.codegen.actions.CustomSignInActions
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent

sealed class CustomSignInState : State {
    data class NotStarted(val id: String = "") : CustomSignInState()
    data class Initiating(val id: String = "") : CustomSignInState()
    data class SignedIn(val id: String = "") : CustomSignInState()
    data class Error(val error: Exception) : CustomSignInState()

    override val type = this.toString()

    class Resolver(private val signInCustomActions: CustomSignInActions) : StateMachineResolver<CustomSignInState> {
        override val defaultState = NotStarted()

        private fun asCustomSignInEvent(event: StateMachineEvent): CustomSignInEvent.EventType? {
            return (event as? CustomSignInEvent)?.eventType
        }

        override fun resolve(
            oldState: CustomSignInState,
            event: StateMachineEvent
        ): StateResolution<CustomSignInState> {
            val defaultResolution = StateResolution(oldState)
            val customSignInEvent = asCustomSignInEvent(event)
            return when (oldState) {
                is NotStarted -> {
                    when (customSignInEvent) {
                        is CustomSignInEvent.EventType.InitiateCustomSignIn -> {
                            StateResolution(
                                Initiating(),
                                listOf(signInCustomActions.initiateCustomSignInAuthAction(customSignInEvent))
                            )
                        }
                        is CustomSignInEvent.EventType.ThrowAuthError -> StateResolution(
                            Error(customSignInEvent.exception)
                        )
                        else -> defaultResolution
                    }
                }
                is Initiating -> {
                    when (customSignInEvent) {
                        is CustomSignInEvent.EventType.FinalizeSignIn -> StateResolution(SignedIn())
                        is CustomSignInEvent.EventType.ThrowAuthError -> StateResolution(
                            Error(customSignInEvent.exception)
                        )
                        else -> defaultResolution
                    }
                }
                else -> defaultResolution
            }
        }
    }
}
