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

package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.auth.cognito.CredentialStoreEnvironment
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.CredentialStoreError
import com.amplifyframework.auth.cognito.events.CredentialStoreEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface CredentialStoreAction: Action

class MigrateLegacyCredentialStore: CredentialStoreAction {

    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as CredentialStoreEnvironment)
        val credentialStore = env.credentialStore
        val legacyCredentialStore = env.legacyCredentialStore

        try {
            val credentials = legacyCredentialStore.retrieveCredential()
            credentialStore.saveCredential(credentials)
            val event = CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore())
            dispatcher.send(event)
        } catch (error: CredentialStoreError) {
            val event = CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            dispatcher.send(event)
        }
    }
}

class ClearCredentialStore: CredentialStoreAction {

    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as CredentialStoreEnvironment)
        val store = env.credentialStore

        try {
            store.deleteCredential()
            val storeEvent = CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(null))
            dispatcher.send(storeEvent)
        } catch (error: CredentialStoreError) {
            val event = CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            dispatcher.send(event)
        }
    }
}

class LoadCredentialStore: CredentialStoreAction {

    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as CredentialStoreEnvironment)
        val store = env.credentialStore

        try {
            val credentials = store.retrieveCredential()
            val storeEvent = CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            dispatcher.send(storeEvent)
        } catch (error: CredentialStoreError) {
            val event = CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            dispatcher.send(event)
        }
    }
}

class StoreCredentials(val credentials: AmplifyCredential): CredentialStoreAction {

    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as CredentialStoreEnvironment)
        val store = env.credentialStore

        try {
            store.saveCredential(credentials)
            val storeEvent = CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            dispatcher.send(storeEvent)
        } catch (error: CredentialStoreError) {
            val event = CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            dispatcher.send(event)
        }
    }
}
