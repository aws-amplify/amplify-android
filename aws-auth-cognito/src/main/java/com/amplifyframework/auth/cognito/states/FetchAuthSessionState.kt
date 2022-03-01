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

import com.amplifyframework.auth.cognito.actions.ConfigureAWSCredentialsAction
import com.amplifyframework.auth.cognito.actions.ConfigureIdentityAction
import com.amplifyframework.auth.cognito.actions.ConfigureUserPoolTokensAction
import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.auth.cognito.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution

sealed class FetchAuthSessionState : State {
    data class InitializingFetchAuthSession(val id: String = "") : FetchAuthSessionState()
    data class FetchingUserPoolTokens(val id: String = "") : FetchAuthSessionState()
    data class FetchingIdentity(val id: String = "") : FetchAuthSessionState()
    data class FetchingAWSCredentials(val id: String = "") : FetchAuthSessionState()
    data class SessionEstablished(val id: String = "") : FetchAuthSessionState()
    data class Error(val id: String = "") : FetchAuthSessionState()

    class Resolver : StateMachineResolver<FetchAuthSessionState> {
        override val defaultState = InitializingFetchAuthSession()
        private fun asFetchAuthSessionEvent(event: StateMachineEvent): FetchAuthSessionEvent.EventType? {
            return (event as? FetchAuthSessionEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchAuthSessionState,
            event: StateMachineEvent
        ): StateResolution<FetchAuthSessionState> {
            val fetchAuthSessionEvent = asFetchAuthSessionEvent(event)
            return when (oldState) {
                is InitializingFetchAuthSession -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchUserPoolTokens -> onFetchUserPoolTokens()
                        is FetchAuthSessionEvent.EventType.FetchIdentity -> onFetchIdentity()
                        is FetchAuthSessionEvent.EventType.ThrowError -> throw AuthenticationError("Fetch user auth session error")
                        else -> StateResolution(oldState)
                    }
                }
                is FetchingUserPoolTokens -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchIdentity ->  onFetchIdentity()
                        is FetchAuthSessionEvent.EventType.ThrowError -> throw AuthenticationError("Fetch user auth session error")
                        else -> StateResolution(oldState)
                    }
                }
                is FetchingIdentity -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchAwsCredentials -> onFetchAWSCredentials()
                        is FetchAuthSessionEvent.EventType.ThrowError -> throw AuthenticationError("Fetch user auth session error")
                        else -> StateResolution(oldState)
                    }
                }
                is FetchingAWSCredentials -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchedAuthSession -> onFetchedAuthSession()
                        is FetchAuthSessionEvent.EventType.ThrowError -> throw AuthenticationError("Fetch user auth session error")
                        else -> StateResolution(oldState)
                    }
                }
                is SessionEstablished -> {
                    StateResolution(oldState)
                }

                is Error -> throw AuthenticationError("Fetch user auth session error")
            }
        }

        private fun onFetchUserPoolTokens(): StateResolution<FetchAuthSessionState> {
            val newState = FetchingUserPoolTokens()
            val action = ConfigureUserPoolTokensAction()
            return StateResolution(newState, listOf(action))
        }

        private fun onFetchIdentity(): StateResolution<FetchAuthSessionState> {
            val newState = FetchingIdentity()
            val action = ConfigureIdentityAction()
            return StateResolution(newState, listOf(action))
        }

        private fun onFetchAWSCredentials(): StateResolution<FetchAuthSessionState> {
            val newState = FetchingAWSCredentials()
            val action = ConfigureAWSCredentialsAction()
            return StateResolution(newState, listOf(action))
        }

        private fun onFetchedAuthSession(): StateResolution<FetchAuthSessionState> {
            val newState = SessionEstablished()
            return StateResolution(newState)
        }
    }


}