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
import com.amplifyframework.statemachine.codegen.actions.StoreActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent

sealed class CredentialStoreState : State {
    data class NotConfigured(val id: String = "") : CredentialStoreState()
    data class MigratingLegacyStore(val id: String = "") : CredentialStoreState()
    data class LoadingStoredCredentials(val id: String = "") : CredentialStoreState()
    data class StoringCredentials(val id: String = "") : CredentialStoreState()
    data class ClearingCredentials(val id: String = "") : CredentialStoreState()
    data class Idle(val id: String = "") : CredentialStoreState()
    data class Success(val storedCredentials: AmplifyCredential) : CredentialStoreState()
    data class Error(val error: CredentialStoreError) : CredentialStoreState()

    override val type = this.toString()

    class Resolver(private val storeActions: StoreActions) : StateMachineResolver<CredentialStoreState> {
        override val defaultState = NotConfigured()

        private fun asCredentialStoreEvent(event: StateMachineEvent): CredentialStoreEvent.EventType? {
            return (event as? CredentialStoreEvent)?.eventType
        }

        override fun resolve(
            oldState: CredentialStoreState,
            event: StateMachineEvent
        ): StateResolution<CredentialStoreState> {
            val defaultResolution = StateResolution(oldState)
            val storeEvent = asCredentialStoreEvent(event)
            return when (oldState) {
                is NotConfigured -> when (storeEvent) {
                    is CredentialStoreEvent.EventType.MigrateLegacyCredentialStore,
                    is CredentialStoreEvent.EventType.LoadCredentialStore -> {
                        val action = storeActions.migrateLegacyCredentialStoreAction()
                        StateResolution(MigratingLegacyStore(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is MigratingLegacyStore -> when (storeEvent) {
                    is CredentialStoreEvent.EventType.LoadCredentialStore -> {
                        val action = storeActions.loadCredentialStoreAction()
                        StateResolution(LoadingStoredCredentials(), listOf(action))
                    }
                    is CredentialStoreEvent.EventType.ThrowError -> StateResolution(Error(storeEvent.error))
                    else -> defaultResolution
                }
                is LoadingStoredCredentials, is StoringCredentials, is ClearingCredentials -> when (storeEvent) {
                    is CredentialStoreEvent.EventType.CompletedOperation -> {
                        val action = storeActions.moveToIdleStateAction()
                        val newState = Success(storeEvent.storedCredentials)
                        StateResolution(newState, listOf(action))
                    }
                    is CredentialStoreEvent.EventType.ThrowError -> StateResolution(Error(storeEvent.error))
                    else -> defaultResolution
                }
                is Idle -> when (storeEvent) {
                    is CredentialStoreEvent.EventType.ClearCredentialStore -> {
                        val action = storeActions.clearCredentialStoreAction()
                        StateResolution(ClearingCredentials(), listOf(action))
                    }
                    is CredentialStoreEvent.EventType.LoadCredentialStore -> {
                        val action = storeActions.loadCredentialStoreAction()
                        StateResolution(LoadingStoredCredentials(), listOf(action))
                    }
                    is CredentialStoreEvent.EventType.StoreCredentials -> {
                        val action = storeActions.storeCredentialsAction(storeEvent.credentials)
                        StateResolution(StoringCredentials(), listOf(action))
                    }
                    else -> StateResolution(oldState)
                }
                is Success, is Error -> when (storeEvent) {
                    is CredentialStoreEvent.EventType.MoveToIdleState -> {
                        val action = storeActions.moveToIdleStateAction()
                        StateResolution(Idle(), listOf(action))
                    }
                    else -> StateResolution(oldState)
                }
            }
        }
    }
}
