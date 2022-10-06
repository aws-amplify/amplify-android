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
import com.amplifyframework.statemachine.codegen.data.AmplifyCredentialType
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
                CredentialStoreEvent(
                    CredentialStoreEvent.EventType.LoadCredentialStore(
                        AmplifyCredentialType.DEVICE_METADATA,
                        null
                    )
                )
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun clearCredentialStoreAction(amplifyCredentialType: AmplifyCredentialType, username: String?) =
        Action<CredentialStoreEnvironment>("ClearCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                when (amplifyCredentialType) {
                    AmplifyCredentialType.AMPLIFY_CREDENTIAL -> {
                        credentialStore.deleteCredential()
                    }
                    AmplifyCredentialType.DEVICE_METADATA -> {
                        username?.let {
                            credentialStore.deleteDeviceKeyCredential(it)
                        }
                    }
                    AmplifyCredentialType.ASF_DEVICE_ID -> TODO()
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(AmplifyCredential.Empty))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun loadCredentialStoreAction(amplifyCredentialType: AmplifyCredentialType, username: String?) =
        Action<CredentialStoreEnvironment>("LoadCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials: AmplifyCredential = when (amplifyCredentialType) {
                    AmplifyCredentialType.AMPLIFY_CREDENTIAL -> {
                        credentialStore.retrieveCredential()
                    }
                    AmplifyCredentialType.DEVICE_METADATA -> {
                        AmplifyCredential.DeviceData(credentialStore.retrieveDeviceMetadata(username ?: ""))
                    }
                    AmplifyCredentialType.ASF_DEVICE_ID -> {
                        AmplifyCredential.Empty
                    }
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun storeCredentialsAction(
        amplifyCredentialType: AmplifyCredentialType,
        username: String?,
        credentials: AmplifyCredential
    ) =
        Action<CredentialStoreEnvironment>("StoreCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                when (amplifyCredentialType) {
                    AmplifyCredentialType.AMPLIFY_CREDENTIAL -> {
                        credentialStore.saveCredential(credentials)
                    }
                    AmplifyCredentialType.DEVICE_METADATA -> {
                        credentialStore.saveDeviceMetadata(
                            username as String,
                            (credentials as AmplifyCredential.DeviceMetaDataTypeCredential).deviceMetadata
                        )
                    }
                    AmplifyCredentialType.ASF_DEVICE_ID -> TODO()
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
