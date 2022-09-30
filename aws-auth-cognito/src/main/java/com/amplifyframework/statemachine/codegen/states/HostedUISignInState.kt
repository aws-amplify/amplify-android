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
import com.amplifyframework.statemachine.codegen.actions.HostedUIActions
import com.amplifyframework.statemachine.codegen.data.HostedUIOptions
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent

sealed class HostedUISignInState : State {
    data class NotStarted(val id: String = "") : HostedUISignInState()
    data class ShowingUI(val hostedUIOptions: HostedUIOptions) : HostedUISignInState()
    data class FetchingToken(val id: String = "") : HostedUISignInState()
    data class Done(val id: String = "") : HostedUISignInState()
    data class Error(val exception: Exception) : HostedUISignInState()

    class Resolver(private val hostedUIActions: HostedUIActions) : StateMachineResolver<HostedUISignInState> {
        override val defaultState = NotStarted()

        private fun asHostedUIEvent(event: StateMachineEvent): HostedUIEvent.EventType? {
            return (event as? HostedUIEvent)?.eventType
        }

        override fun resolve(
            oldState: HostedUISignInState,
            event: StateMachineEvent
        ): StateResolution<HostedUISignInState> {
            val hostedUIEvent = asHostedUIEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (hostedUIEvent) {
                    is HostedUIEvent.EventType.ShowHostedUI -> {
                        val action = hostedUIActions.showHostedUI(hostedUIEvent)
                        StateResolution(ShowingUI(hostedUIEvent.hostedUISignInData.hostedUIOptions), listOf(action))
                    }
                    else -> defaultResolution
                }
                is ShowingUI -> when (hostedUIEvent) {
                    is HostedUIEvent.EventType.FetchToken -> {
                        val action = hostedUIActions.fetchHostedUISignInToken(
                            hostedUIEvent,
                            oldState.hostedUIOptions.browserPackage
                        )
                        StateResolution(FetchingToken(), listOf(action))
                    }
                    is HostedUIEvent.EventType.ThrowError -> StateResolution((Error(hostedUIEvent.exception)))
                    else -> defaultResolution
                }
                is FetchingToken -> when (hostedUIEvent) {
                    is HostedUIEvent.EventType.TokenFetched -> StateResolution(Done())
                    is HostedUIEvent.EventType.ThrowError -> StateResolution((Error(hostedUIEvent.exception)))
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
