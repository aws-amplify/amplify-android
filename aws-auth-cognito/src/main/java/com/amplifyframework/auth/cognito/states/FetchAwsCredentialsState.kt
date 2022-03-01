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

import com.amplifyframework.auth.cognito.actions.InitFetchAWSCredentialsAction
import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.auth.cognito.events.FetchAwsCredentialsEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution

sealed class FetchAwsCredentialsState : State {
    data class Configuring(val id: String = "") : FetchAwsCredentialsState()
    data class Fetching(val id: String = "") : FetchAwsCredentialsState()
    data class Fetched(val id: String = "") : FetchAwsCredentialsState()
    data class Error(val id: String = "") : FetchAwsCredentialsState()

    class Resolver : StateMachineResolver<FetchAwsCredentialsState> {
        override val defaultState = Configuring()
        private fun asFetchIdentityEvent(event: StateMachineEvent): FetchAwsCredentialsEvent.EventType? {
            return (event as? FetchAwsCredentialsEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchAwsCredentialsState,
            event: StateMachineEvent
        ): StateResolution<FetchAwsCredentialsState> {
            val fetchIdentityEvent = asFetchIdentityEvent(event)
            return when (oldState) {
                is Configuring -> {
                    when (fetchIdentityEvent) {
                        is FetchAwsCredentialsEvent.EventType.Fetch -> onFetchAWSCredentials()
                        else -> StateResolution(oldState)
                    }
                }
                is Fetching -> {
                    when (fetchIdentityEvent) {
                        is FetchAwsCredentialsEvent.EventType.Fetched -> onFetchAWSCredentialsSuccess()
                        is FetchAwsCredentialsEvent.EventType.ThrowError -> onFetchAWSCredentialsFailure()
                        else -> StateResolution(oldState)
                    }
                }
                is Fetched -> {
                    StateResolution(oldState)
                }

                is Error -> throw AuthenticationError("Fetch user AWS Credentials error")
            }
        }

        private fun onFetchAWSCredentialsSuccess(): StateResolution<FetchAwsCredentialsState> {
            val newState = Fetched()
            return StateResolution(newState)
        }

        private fun onFetchAWSCredentialsFailure(): StateResolution<FetchAwsCredentialsState> {
            val newState = Error()
            return StateResolution(newState)
        }

        private fun onFetchAWSCredentials(): StateResolution<FetchAwsCredentialsState> {
            val newState = Fetching()
            val action = InitFetchAWSCredentialsAction()
            return StateResolution(newState, listOf(action))
        }
    }
}