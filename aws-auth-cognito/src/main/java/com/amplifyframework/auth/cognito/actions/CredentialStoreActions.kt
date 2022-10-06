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
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.StoreActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent

object CredentialStoreActions : StoreActions {
    override fun migrateLegacyCredentialStoreAction() =
        Action<CredentialStoreEnvironment>("MigrateLegacyCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials = legacyCredentialStore.retrieveCredential()
                if (credentials != AmplifyCredential.Empty) {
                    credentialStore.saveCredential(credentials)
                    legacyCredentialStore.deleteCredential()
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore(null))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun clearCredentialStoreAction(username: String?) =
        Action<CredentialStoreEnvironment>("ClearCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                username?.let{
                    credentialStore.deleteDeviceKeyCredential(it)
                }
                credentialStore.deleteCredential()
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(AmplifyCredential.Empty))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun loadCredentialStoreAction(username: String?) =
        Action<CredentialStoreEnvironment>("LoadCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials: AmplifyCredential = if (username != null) {
                    AmplifyCredential.DeviceData(credentialStore.retrieveDeviceMetadata(username))
                } else {
                    credentialStore.retrieveCredential()
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun storeCredentialsAction(username: String?, credentials: AmplifyCredential) =
        Action<CredentialStoreEnvironment>("StoreCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                if (username != null && credentials is AmplifyCredential.DeviceMetaDataTypeCredential) {
                    credentialStore.saveDeviceMetadata(
                        username,
                        credentials.deviceMetadata
                    )
                } else {
                    credentialStore.saveCredential(credentials)
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun moveToIdleStateAction() =
        Action<CredentialStoreEnvironment>("MoveToIdleState") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = CredentialStoreEvent(CredentialStoreEvent.EventType.MoveToIdleState())
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
