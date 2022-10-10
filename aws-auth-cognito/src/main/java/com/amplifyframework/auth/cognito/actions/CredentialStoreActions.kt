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
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent

object CredentialStoreActions : StoreActions {
    override fun migrateLegacyCredentialStoreAction() =
        Action<CredentialStoreEnvironment>("MigrateLegacyCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials = legacyCredentialStore.retrieveCredential()
                if (credentials != AmplifyCredential.Empty) {
                    val username = (credentials as AmplifyCredential.UserPoolTypeCredential).signedInData.username
                    val deviceMetaData = legacyCredentialStore.retrieveDeviceMetadata(username)
                    credentialStore.saveCredential(credentials)
                    credentialStore.saveDeviceMetadata(username, deviceMetaData)
                    legacyCredentialStore.deleteCredential()
                    legacyCredentialStore.deleteDeviceKeyCredential(username)
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore(CredentialType.Amplify))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun clearCredentialStoreAction(credentialType: CredentialType) =
        Action<CredentialStoreEnvironment>("ClearCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                when (credentialType) {
                    CredentialType.Amplify -> {
                        credentialStore.deleteCredential()
                    }
                    is CredentialType.Device -> {
                        credentialStore.deleteDeviceKeyCredential(credentialType.username ?: "")
                    }
                    CredentialType.ASF -> TODO()
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(AmplifyCredential.Empty))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun loadCredentialStoreAction(credentialType: CredentialType) =
        Action<CredentialStoreEnvironment>("LoadCredentialStore") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials: AmplifyCredential = when (credentialType) {
                    CredentialType.Amplify -> {
                        credentialStore.retrieveCredential()
                    }
                    is CredentialType.Device -> {
                        AmplifyCredential.DeviceData(
                            credentialStore.retrieveDeviceMetadata(credentialType.username ?: "")
                        )
                    }
                    CredentialType.ASF -> {
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
        credentialType: CredentialType,
        credentials: AmplifyCredential
    ) =
        Action<CredentialStoreEnvironment>("StoreCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                when (credentialType) {
                    CredentialType.Amplify -> {
                        credentialStore.saveCredential(credentials)
                    }
                    is CredentialType.Device -> {
                        credentialStore.saveDeviceMetadata(
                            credentialType.username ?: "",
                            (credentials as AmplifyCredential.DeviceMetaDataTypeCredential).deviceMetadata
                        )
                    }
                    CredentialType.ASF -> TODO()
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
