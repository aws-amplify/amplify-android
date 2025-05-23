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
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent

internal sealed class FetchAuthSessionState : State {
    data class NotStarted(val id: String = "") : FetchAuthSessionState()
    data class FetchingIdentity(val logins: LoginsMapProvider) : FetchAuthSessionState()
    data class FetchingAWSCredentials(val identityId: String, val logins: LoginsMapProvider) : FetchAuthSessionState()
    data class Fetched(val id: String = "") : FetchAuthSessionState()
    data class Error(val exception: Exception) : FetchAuthSessionState()

    class Resolver(
        private val fetchAuthSessionActions: FetchAuthSessionActions
    ) : StateMachineResolver<FetchAuthSessionState> {
        override val defaultState = NotStarted()
        private fun asFetchAuthSessionEvent(event: StateMachineEvent): FetchAuthSessionEvent.EventType? =
            (event as? FetchAuthSessionEvent)?.eventType

        override fun resolve(
            oldState: FetchAuthSessionState,
            event: StateMachineEvent
        ): StateResolution<FetchAuthSessionState> {
            val fetchAuthSessionEvent = asFetchAuthSessionEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (fetchAuthSessionEvent) {
                    is FetchAuthSessionEvent.EventType.FetchIdentity -> {
                        val action = fetchAuthSessionActions.fetchIdentityAction(fetchAuthSessionEvent.logins)
                        StateResolution(FetchingIdentity(fetchAuthSessionEvent.logins), listOf(action))
                    }
                    is FetchAuthSessionEvent.EventType.FetchAwsCredentials -> {
                        val action = fetchAuthSessionActions.fetchAWSCredentialsAction(
                            fetchAuthSessionEvent.identityId,
                            fetchAuthSessionEvent.logins
                        )
                        StateResolution(
                            FetchingAWSCredentials(fetchAuthSessionEvent.identityId, fetchAuthSessionEvent.logins),
                            listOf(action)
                        )
                    }
                    else -> defaultResolution
                }
                is FetchingIdentity -> when (fetchAuthSessionEvent) {
                    is FetchAuthSessionEvent.EventType.FetchAwsCredentials -> {
                        val action = fetchAuthSessionActions.fetchAWSCredentialsAction(
                            fetchAuthSessionEvent.identityId,
                            fetchAuthSessionEvent.logins
                        )
                        StateResolution(
                            FetchingAWSCredentials(fetchAuthSessionEvent.identityId, fetchAuthSessionEvent.logins),
                            listOf(action)
                        )
                    }
                    else -> defaultResolution
                }
                is FetchingAWSCredentials -> when (fetchAuthSessionEvent) {
                    is FetchAuthSessionEvent.EventType.Fetched -> {
                        val action = fetchAuthSessionActions.notifySessionEstablishedAction(
                            fetchAuthSessionEvent.identityId,
                            fetchAuthSessionEvent.awsCredentials
                        )
                        StateResolution(Fetched(), listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
