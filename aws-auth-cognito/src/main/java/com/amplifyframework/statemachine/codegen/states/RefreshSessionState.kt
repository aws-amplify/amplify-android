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

import com.amplifyframework.statemachine.Builder
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.SignedInData

sealed class RefreshSessionState : State {
    data class NotStarted(val id: String = "") : RefreshSessionState()
    data class RefreshingUserPoolTokens(val signedInData: SignedInData) : RefreshSessionState()
    data class RefreshingAWSSession(override val fetchAuthSessionState: FetchAuthSessionState?) : RefreshSessionState()
    data class Refreshed(val amplifyCredential: AmplifyCredential) : RefreshSessionState()

    open val fetchAuthSessionState: FetchAuthSessionState? = FetchAuthSessionState.NotStarted()

    class Resolver(
        private val fetchAuthSessionResolver: StateMachineResolver<FetchAuthSessionState>
    ) : StateMachineResolver<RefreshSessionState> {
        override val defaultState=NotStarted()

        override fun resolve(oldState: RefreshSessionState, event: StateMachineEvent): StateResolution<RefreshSessionState> {
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
        ):StateResolution<RefreshSessionState> {
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                else -> defaultResolution
            }
        }
    }

    class Builder(private val refreshSessionState: RefreshSessionState): com.amplifyframework.statemachine.Builder<RefreshSessionState> {
        var fetchAuthSessionState: FetchAuthSessionState? = null
        override fun build(): RefreshSessionState = when (refreshSessionState) {
            is RefreshingAWSSession -> RefreshingAWSSession(fetchAuthSessionState)
            else -> refreshSessionState
        }
    }
}