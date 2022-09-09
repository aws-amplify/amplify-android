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
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent

sealed class DeviceSRPSignInState : State {
    data class NotStarted(val id: String = "") : DeviceSRPSignInState()
    data class InitiatingDeviceSRP(val id: String = "") : DeviceSRPSignInState()
    data class RespondingDevicePasswordVerifier(val id: String = "") : DeviceSRPSignInState()
    data class SignedIn(val id: String = "") : DeviceSRPSignInState()
    data class Cancelling(val id: String = "") : DeviceSRPSignInState()
    data class Error(val exception: Exception) : DeviceSRPSignInState()

    class Resolver(private val deviceSRPSignInActions: DeviceSRPSignInActions) :
        StateMachineResolver<DeviceSRPSignInState> {
        override val defaultState = NotStarted()
        private fun asDeviceSRPSignInEvent(event: StateMachineEvent): DeviceSRPSignInEvent.EventType? {
            return (event as? DeviceSRPSignInEvent)?.eventType
        }

        override fun resolve(
            oldState: DeviceSRPSignInState,
            event: StateMachineEvent
        ): StateResolution<DeviceSRPSignInState> {
            val deviceSRPEvent = asDeviceSRPSignInEvent(event) ?: return StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> {
                    when (deviceSRPEvent) {
                        is DeviceSRPSignInEvent.EventType.RespondDeviceSRPChallenge -> {
                            val action = deviceSRPSignInActions.respondDeviceSRP(deviceSRPEvent)
                            StateResolution(InitiatingDeviceSRP(), listOf(action))
                        }
                        is DeviceSRPSignInEvent.EventType.ThrowAuthError -> {
                            StateResolution(Error(deviceSRPEvent.exception))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is InitiatingDeviceSRP -> {
                    when (deviceSRPEvent) {
                        is DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier -> {
                            val action = deviceSRPSignInActions.respondDevicePasswordVerifier(deviceSRPEvent)
                            StateResolution(RespondingDevicePasswordVerifier(), listOf(action))
                        }
                        is DeviceSRPSignInEvent.EventType.ThrowPasswordVerifiedError -> {
                            StateResolution(Error(deviceSRPEvent.exception))
                        }
                        is DeviceSRPSignInEvent.EventType.ThrowAuthError -> {
                            StateResolution(Error(deviceSRPEvent.exception))
                        }
                        is DeviceSRPSignInEvent.EventType.CancelSRPSignIn -> {
                            StateResolution(
                                Cancelling(),
                                listOf(deviceSRPSignInActions.cancellingSignIn(deviceSRPEvent))
                            )
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is RespondingDevicePasswordVerifier -> {
                    when (deviceSRPEvent) {
                        is DeviceSRPSignInEvent.EventType.CancelSRPSignIn -> {
                            StateResolution(
                                Cancelling(),
                                listOf(deviceSRPSignInActions.cancellingSignIn(deviceSRPEvent))
                            )
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is Cancelling -> {
                    when (deviceSRPEvent) {
                        is DeviceSRPSignInEvent.EventType.RestoreToNotInitialized -> {
                            StateResolution(NotStarted())
                        }
                        else -> StateResolution(oldState)
                    }
                }
                else -> StateResolution(oldState)
            }
        }
    }
}
