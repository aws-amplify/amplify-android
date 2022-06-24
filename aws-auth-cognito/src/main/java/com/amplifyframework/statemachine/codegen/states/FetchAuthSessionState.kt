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
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent

sealed class FetchAuthSessionState : State {
    data class NotStarted(val id: String = "") : FetchAuthSessionState()
    data class FetchingUserPoolTokens(
        override var fetchUserPoolTokensState: FetchUserPoolTokensState?
    ) : FetchAuthSessionState()

    data class FetchingIdentity(override var fetchIdentityState: FetchIdentityState?) : FetchAuthSessionState()

    data class FetchingAWSCredentials(
        override var fetchAwsCredentialsState: FetchAwsCredentialsState?
    ) : FetchAuthSessionState()

    data class Fetched(val id: String = "") : FetchAuthSessionState()

    open var fetchAwsCredentialsState: FetchAwsCredentialsState? = FetchAwsCredentialsState.Configuring()
    open var fetchUserPoolTokensState: FetchUserPoolTokensState? = FetchUserPoolTokensState.Configuring()
    open var fetchIdentityState: FetchIdentityState? = FetchIdentityState.Configuring()

    class Resolver(
        private val fetchAWSCredentialsResolver: StateMachineResolver<FetchAwsCredentialsState>,
        private val fetchIdentityResolver: StateMachineResolver<FetchIdentityState>,
        private val fetchUserPoolTokensResolver: StateMachineResolver<FetchUserPoolTokensState>,
        private val fetchAuthSessionActions: FetchAuthSessionActions
    ) : StateMachineResolver<FetchAuthSessionState> {
        override val defaultState = NotStarted()
        private fun asFetchAuthSessionEvent(event: StateMachineEvent): FetchAuthSessionEvent.EventType? {
            return (event as? FetchAuthSessionEvent)?.eventType
        }

        override fun resolve(
            oldState: FetchAuthSessionState,
            event: StateMachineEvent
        ): StateResolution<FetchAuthSessionState> {
            val resolution = resolveFetchAuthSessionEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.fetchAwsCredentialsState?.let { fetchAWSCredentialsResolver.resolve(it, event) }?.let {
                builder.fetchAwsCredentialsState = it.newState
                actions += it.actions
            }

            oldState.fetchIdentityState?.let { fetchIdentityResolver.resolve(it, event) }?.let {
                builder.fetchIdentityState = it.newState
                actions += it.actions
            }

            oldState.fetchUserPoolTokensState?.let { fetchUserPoolTokensResolver.resolve(it, event) }?.let {
                builder.fetchUserPoolTokensState = it.newState
                actions += it.actions
            }
            return StateResolution(builder.build(), actions)
        }

        private fun resolveFetchAuthSessionEvent(
            oldState: FetchAuthSessionState,
            event: StateMachineEvent
        ): StateResolution<FetchAuthSessionState> {
            val fetchAuthSessionEvent = asFetchAuthSessionEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchUserPoolTokens -> {
                            val newState = FetchingUserPoolTokens(oldState.fetchUserPoolTokensState)
                            val action = fetchAuthSessionActions.configureUserPoolTokensAction(
                                fetchAuthSessionEvent.amplifyCredential
                            )
                            StateResolution(newState, listOf(action))
                        }
                        is FetchAuthSessionEvent.EventType.FetchIdentity -> {
                            val newState = FetchingIdentity(oldState.fetchIdentityState)
                            val action = fetchAuthSessionActions.configureIdentityAction(
                                fetchAuthSessionEvent.amplifyCredential
                            )
                            StateResolution(newState, listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                is FetchingUserPoolTokens -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchIdentity -> {
                            val newState = FetchingIdentity(oldState.fetchIdentityState)
                            val action = fetchAuthSessionActions.configureIdentityAction(
                                fetchAuthSessionEvent.amplifyCredential
                            )
                            StateResolution(newState, listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                is FetchingIdentity -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchAwsCredentials -> {
                            val newState = FetchingAWSCredentials(oldState.fetchAwsCredentialsState)
                            val action = fetchAuthSessionActions.configureAWSCredentialsAction(
                                fetchAuthSessionEvent.amplifyCredential
                            )
                            StateResolution(newState, listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                is FetchingAWSCredentials -> {
                    when (fetchAuthSessionEvent) {
                        is FetchAuthSessionEvent.EventType.FetchedAuthSession -> {
                            val action = fetchAuthSessionActions.authorizationSessionEstablished(
                                fetchAuthSessionEvent.amplifyCredential
                            )
                            StateResolution(Fetched(), listOf(action))
                        }
                        else -> defaultResolution
                    }
                }
                else -> defaultResolution
            }
        }
    }

    class Builder(private val authSessionState: FetchAuthSessionState) :
        com.amplifyframework.statemachine.Builder<FetchAuthSessionState> {
        var fetchAwsCredentialsState: FetchAwsCredentialsState? = null
        var fetchUserPoolTokensState: FetchUserPoolTokensState? = null
        var fetchIdentityState: FetchIdentityState? = null

        override fun build(): FetchAuthSessionState = when (authSessionState) {
            is FetchingAWSCredentials -> FetchingAWSCredentials(fetchAwsCredentialsState)
            is FetchingIdentity -> FetchingIdentity(fetchIdentityState)
            is FetchingUserPoolTokens -> FetchingUserPoolTokens(fetchUserPoolTokensState)
            else -> authSessionState
        }
    }
}
