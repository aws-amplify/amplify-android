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

import com.amplifyframework.auth.cognito.actions.ClearCredentialStore
import com.amplifyframework.auth.cognito.actions.LoadCredentialStore
import com.amplifyframework.auth.cognito.actions.MigrateLegacyCredentialStore
import com.amplifyframework.auth.cognito.actions.StoreCredentials
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.CredentialStoreError
import com.amplifyframework.auth.cognito.events.CredentialStoreEvent
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution

sealed class CredentialStoreState : State {
    data class NotIntialized(val id: String = "") : CredentialStoreState()
    data class MigratingLegacyStore(val id: String = "") : CredentialStoreState()
    data class LoadingStoredCredentials(val id: String = "") : CredentialStoreState()
    data class StoringCredentials(val id: String = "") : CredentialStoreState()
    data class ClearingCredentials(val id: String = "") : CredentialStoreState()
    data class Idle(val storedCredentials: AmplifyCredential?) : CredentialStoreState()
    data class Error(val error: CredentialStoreError) : CredentialStoreState()

    override val type = this.toString()

    class Resolver : StateMachineResolver<CredentialStoreState> {
        override val defaultState = NotIntialized()

        private fun asCredentialStoreEvent(event: StateMachineEvent): CredentialStoreEvent.EventType? {
            return (event as? CredentialStoreEvent)?.eventType
        }

        override fun resolve(oldState: CredentialStoreState, event: StateMachineEvent): StateResolution<CredentialStoreState> {
            val storeEvent = asCredentialStoreEvent(event)
            return when (oldState) {
                is NotIntialized -> {
                    when (storeEvent) {
                        is CredentialStoreEvent.EventType.MigrateLegacyCredentialStore -> {
                            val action = MigrateLegacyCredentialStore()
                            StateResolution(MigratingLegacyStore(), listOf(action))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is MigratingLegacyStore -> {
                    when (storeEvent) {
                        is CredentialStoreEvent.EventType.LoadCredentialStore -> {
                            val action = LoadCredentialStore()
                            StateResolution(LoadingStoredCredentials(), listOf(action))
                        }
                        is CredentialStoreEvent.EventType.ThrowError -> {
                            StateResolution(Error(storeEvent.error))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is LoadingStoredCredentials, is StoringCredentials, is ClearingCredentials -> {
                    when (storeEvent) {
                        is CredentialStoreEvent.EventType.CompletedOperation -> {
                            StateResolution(Idle(storeEvent.storedCredentials))
                        }
                        is CredentialStoreEvent.EventType.ThrowError -> {
                            StateResolution(Error(storeEvent.error))
                        }
                        else -> StateResolution(oldState)
                    }
                }
                is Error, is Idle -> {
                    when (storeEvent) {
                        is CredentialStoreEvent.EventType.ClearCredentialStore -> {
                            val action = ClearCredentialStore()
                            StateResolution(ClearingCredentials(), listOf(action))
                        }
                        is CredentialStoreEvent.EventType.LoadCredentialStore -> {
                            val action = LoadCredentialStore()
                            StateResolution(LoadingStoredCredentials(), listOf(action))
                        }
                        is CredentialStoreEvent.EventType.StoreCredentials -> {
                            val action = StoreCredentials(storeEvent.credentials)
                            StateResolution(StoringCredentials(), listOf(action))
                        }
                        else -> StateResolution(oldState)
                    }
                }
            }
        }
    }
}