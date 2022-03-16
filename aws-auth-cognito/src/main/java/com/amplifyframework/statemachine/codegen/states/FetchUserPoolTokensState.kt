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

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.AuthenticationError
import com.amplifyframework.statemachine.codegen.events.FetchUserPoolTokensEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.FetchUserPoolTokensActions

sealed class FetchUserPoolTokensState : State {
    data class Configuring(val id: String = "") : FetchUserPoolTokensState()
    data class Refreshing(val id: String = "") : FetchUserPoolTokensState()
    data class Fetched(val amplifyCredential: AmplifyCredential?) : FetchUserPoolTokensState()
    data class Error(val id: String = "") : FetchUserPoolTokensState()

    class Resolver(private val fetchUserPoolTokensActions: FetchUserPoolTokensActions) :
        StateMachineResolver<FetchUserPoolTokensState> {
        override val defaultState = Configuring()
        private fun asFetchUserPoolTokensEvent(event: StateMachineEvent): FetchUserPoolTokensEvent.EventType? {
            return (event as? FetchUserPoolTokensEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchUserPoolTokensState,
            event: StateMachineEvent
        ): StateResolution<FetchUserPoolTokensState> {
            val fetchUserPoolTokensEvent = asFetchUserPoolTokensEvent(event)
            return when (oldState) {
                is Configuring -> {
                    when (fetchUserPoolTokensEvent) {
                        is FetchUserPoolTokensEvent.EventType.Fetched -> {
                            val newState = Fetched(fetchUserPoolTokensEvent.amplifyCredential)
                             StateResolution(newState)
                        }
                        is FetchUserPoolTokensEvent.EventType.Refresh -> {
                            val newState = Refreshing()
                            val action =
                                fetchUserPoolTokensActions.refreshFetchUserPoolTokensAction(fetchUserPoolTokensEvent.amplifyCredential)
                            StateResolution(newState, listOf(action))
                        }
                        is FetchUserPoolTokensEvent.EventType.ThrowError -> onFetchUserPoolTokensFailure()
                        else -> StateResolution(oldState)
                    }
                }
                is Refreshing -> {
                    when (fetchUserPoolTokensEvent) {
                        is FetchUserPoolTokensEvent.EventType.Fetched -> {
                            val newState = Fetched(fetchUserPoolTokensEvent.amplifyCredential)
                            StateResolution(newState)
                        }
                        is FetchUserPoolTokensEvent.EventType.ThrowError -> onFetchUserPoolTokensFailure()
                        else -> StateResolution(oldState)
                    }
                }
                is Fetched -> {
                    StateResolution(oldState)
                }

                is Error -> throw AuthenticationError("Fetch user pool tokens error")
            }
        }

        private fun onFetchUserPoolTokensFailure(): StateResolution<FetchUserPoolTokensState> {
            val newState = Error()
            return StateResolution(newState)
        }
    }
}