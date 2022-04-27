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
import com.amplifyframework.statemachine.codegen.actions.FetchAWSCredentialsActions
import com.amplifyframework.statemachine.codegen.events.FetchAwsCredentialsEvent
import java.lang.Exception

sealed class FetchAwsCredentialsState : State {
    data class Configuring(val id: String = "") : FetchAwsCredentialsState()
    data class Fetching(val id: String = "") : FetchAwsCredentialsState()
    data class Fetched(val id: String = "") : FetchAwsCredentialsState()
    data class Error(val exception: Exception) : FetchAwsCredentialsState()

    class Resolver(private val fetchAWSCredentialsActions: FetchAWSCredentialsActions) :
        StateMachineResolver<FetchAwsCredentialsState> {
        override val defaultState = Configuring()
        private fun asFetchAwsCredentialsEvent(event: StateMachineEvent): FetchAwsCredentialsEvent.EventType? {
            return (event as? FetchAwsCredentialsEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchAwsCredentialsState,
            event: StateMachineEvent
        ): StateResolution<FetchAwsCredentialsState> {
            val fetchAwsCredentialsEvent = asFetchAwsCredentialsEvent(event)
            return when (oldState) {
                is Configuring -> {
                    when (fetchAwsCredentialsEvent) {
                        is FetchAwsCredentialsEvent.EventType.Fetch -> {
                            val action = fetchAWSCredentialsActions.initFetchAWSCredentialsAction(
                                fetchAwsCredentialsEvent.amplifyCredential
                            )
                            StateResolution(Fetching(), listOf(action))
                        }
                        is FetchAwsCredentialsEvent.EventType.Fetched -> StateResolution(Fetched())
                        else -> StateResolution(oldState)
                    }
                }
                is Fetching -> {
                    when (fetchAwsCredentialsEvent) {
                        is FetchAwsCredentialsEvent.EventType.Fetched -> StateResolution(Fetched())
                        is FetchAwsCredentialsEvent.EventType.ThrowError -> {
                            StateResolution(Error(fetchAwsCredentialsEvent.exception))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                else -> StateResolution(oldState)
            }
        }
    }
}
