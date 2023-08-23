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
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent

internal sealed class RefreshSessionState : State {
    data class NotStarted(val id: String = "") : RefreshSessionState()
    data class RefreshingUserPoolTokens(val signedInData: SignedInData) : RefreshSessionState()
    data class RefreshingAuthSession(
        val signedInData: SignedInData,
        override val fetchAuthSessionState: FetchAuthSessionState?
    ) : RefreshSessionState()

    data class RefreshingUnAuthSession(
        override val fetchAuthSessionState: FetchAuthSessionState?
    ) : RefreshSessionState()

    data class Refreshed(val id: String = "") : RefreshSessionState()

    open val fetchAuthSessionState: FetchAuthSessionState? = FetchAuthSessionState.NotStarted()

    class Resolver(
        private val fetchAuthSessionResolver: StateMachineResolver<FetchAuthSessionState>,
        private val fetchAuthSessionActions: FetchAuthSessionActions
    ) : StateMachineResolver<RefreshSessionState> {
        override val defaultState = NotStarted()

        private fun asFetchAuthSessionEvent(event: StateMachineEvent): FetchAuthSessionEvent.EventType? {
            return (event as? FetchAuthSessionEvent)?.eventType
        }

        private fun asRefreshSessionEvent(event: StateMachineEvent): RefreshSessionEvent.EventType? {
            return (event as? RefreshSessionEvent)?.eventType
        }

        override fun resolve(
            oldState: RefreshSessionState,
            event: StateMachineEvent
        ): StateResolution<RefreshSessionState> {
            val resolution = resolveRefreshSessionEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.fetchAuthSessionState?.let { fetchAuthSessionResolver.resolve(it, event) }?.let {
                builder.fetchAuthSessionState = it.newState
                actions += it.actions
            }

            return StateResolution(builder.build(), actions)
        }

        private fun resolveRefreshSessionEvent(
            oldState: RefreshSessionState,
            event: StateMachineEvent
        ): StateResolution<RefreshSessionState> {
            val refreshSessionEvent = asRefreshSessionEvent(event)
            val fetchAuthSessionEvent = asFetchAuthSessionEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (refreshSessionEvent) {
                    is RefreshSessionEvent.EventType.RefreshUserPoolTokens -> {
                        val action = fetchAuthSessionActions.refreshUserPoolTokensAction(
                            refreshSessionEvent.signedInData
                        )
                        StateResolution(RefreshingUserPoolTokens(refreshSessionEvent.signedInData), listOf(action))
                    }
                    is RefreshSessionEvent.EventType.RefreshUnAuthSession -> {
                        val action = fetchAuthSessionActions.refreshAuthSessionAction(refreshSessionEvent.logins)
                        StateResolution(RefreshingUnAuthSession(FetchAuthSessionState.NotStarted()), listOf(action))
                    }
                    else -> defaultResolution
                }
                is RefreshingUnAuthSession -> when (fetchAuthSessionEvent) {
                    is FetchAuthSessionEvent.EventType.Fetched -> {
                        val amplifyCredential = AmplifyCredential.IdentityPool(
                            fetchAuthSessionEvent.identityId,
                            fetchAuthSessionEvent.awsCredentials
                        )
                        val action = fetchAuthSessionActions.notifySessionRefreshedAction(amplifyCredential)
                        StateResolution(Refreshed(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is RefreshingUserPoolTokens -> when (refreshSessionEvent) {
                    is RefreshSessionEvent.EventType.Refreshed -> {
                        val amplifyCredential = AmplifyCredential.UserPool(refreshSessionEvent.signedInData)
                        val action = fetchAuthSessionActions.notifySessionRefreshedAction(amplifyCredential)
                        StateResolution(Refreshed(), listOf(action))
                    }
                    is RefreshSessionEvent.EventType.RefreshAuthSession -> {
                        val action = fetchAuthSessionActions.refreshAuthSessionAction(refreshSessionEvent.logins)
                        StateResolution(
                            RefreshingAuthSession(refreshSessionEvent.signedInData, FetchAuthSessionState.NotStarted()),
                            listOf(action)
                        )
                    }
                    else -> defaultResolution
                }
                is RefreshingAuthSession -> when (fetchAuthSessionEvent) {
                    is FetchAuthSessionEvent.EventType.Fetched -> {
                        val amplifyCredential = AmplifyCredential.UserAndIdentityPool(
                            oldState.signedInData,
                            fetchAuthSessionEvent.identityId,
                            fetchAuthSessionEvent.awsCredentials
                        )
                        val action = fetchAuthSessionActions.notifySessionRefreshedAction(amplifyCredential)
                        StateResolution(Refreshed(), listOf(action))
                    }
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val refreshSessionState: RefreshSessionState) :
        com.amplifyframework.statemachine.Builder<RefreshSessionState> {
        var fetchAuthSessionState: FetchAuthSessionState? = null
        override fun build(): RefreshSessionState = when (refreshSessionState) {
            is RefreshingUnAuthSession -> RefreshingUnAuthSession(fetchAuthSessionState)
            is RefreshingAuthSession -> RefreshingAuthSession(refreshSessionState.signedInData, fetchAuthSessionState)
            else -> refreshSessionState
        }
    }
}
