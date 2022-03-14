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

import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.auth.cognito.events.FetchIdentityEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.FetchIdentityActions

sealed class FetchIdentityState : State {
    data class Configuring(val id: String = "") : FetchIdentityState()
    data class Fetching(val id: String = "") : FetchIdentityState()
    data class Fetched(val amplifyCredential: AmplifyCredential?) : FetchIdentityState()
    data class Error(val id: String = "") : FetchIdentityState()

    class Resolver(private val fetchIdentityActions: FetchIdentityActions) :
        StateMachineResolver<FetchIdentityState> {
        override val defaultState = Configuring()
        private fun asFetchIdentityEvent(event: StateMachineEvent): FetchIdentityEvent.EventType? {
            return (event as? FetchIdentityEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchIdentityState,
            event: StateMachineEvent
        ): StateResolution<FetchIdentityState> {
            val fetchIdentityEvent = asFetchIdentityEvent(event)
            return when (oldState) {
                is Configuring -> {
                    when (fetchIdentityEvent) {
                        is FetchIdentityEvent.EventType.Fetch -> {
                            val newState = Fetching()
                            val action =
                                fetchIdentityActions.initFetchIdentityAction(fetchIdentityEvent.amplifyCredential)
                            StateResolution(newState, listOf(action))
                        }
                        is FetchIdentityEvent.EventType.Fetched -> {
                            val newState = Fetched(fetchIdentityEvent.amplifyCredential)
                            StateResolution(newState)
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is Fetching -> {
                    when (fetchIdentityEvent) {
                        is FetchIdentityEvent.EventType.Fetched -> {
                            val newState = Fetched(fetchIdentityEvent.amplifyCredential)
                            StateResolution(newState)
                        }
                        is FetchIdentityEvent.EventType.ThrowError -> {
                            val newState = Error()
                            StateResolution(newState)
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is Fetched -> {
                    StateResolution(oldState)
                }

                is Error -> throw AuthenticationError("Fetch user identity error")
            }
        }
    }
}