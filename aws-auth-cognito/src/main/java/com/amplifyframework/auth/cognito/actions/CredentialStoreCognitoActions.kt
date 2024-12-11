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
import com.amplifyframework.statemachine.codegen.actions.CredentialStoreActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent

internal object CredentialStoreCognitoActions : CredentialStoreActions {
    override fun migrateLegacyCredentialStoreAction() =
        Action<CredentialStoreEnvironment>("MigrateLegacyCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val credentials = legacyCredentialStore.retrieveCredential()
                if (credentials != AmplifyCredential.Empty) {
                    // migrate credentials
                    credentialStore.saveCredential(credentials)
                    legacyCredentialStore.deleteCredential()
                }

                /*
                Migrate Device Metadata
                1. We first need to get the list of userIds that contain device metadata on the device.
                2. For each userId, we check to see if the current credential store has device metadata for that user.
                3. If the current user does not have device metadata in the current store, migrate from legacy.
                This is a possibility because of a bug where we were previously attempting to migrate using an aliased
                userId lookup. This situation would happen if a user migrated, signed out, then signed back in. Upon
                signing back in, they would be granted new device metadata. Since that new metadata is what is
                associated with the refresh token, we do not want to overwrite it.
                4. If the current user has device metadata in the current credential store, do not migrate from legacy.
                5. Upon migration completion, we delete the legacy device metadata.
                 */
                legacyCredentialStore.retrieveDeviceMetadataUserIdList().forEach { userId ->
                    val deviceMetaData = legacyCredentialStore.retrieveDeviceMetadata(userId)
                    if (deviceMetaData != DeviceMetadata.Empty) {
                        credentialStore.retrieveDeviceMetadata(userId)
                        if (credentialStore.retrieveDeviceMetadata(userId) == DeviceMetadata.Empty) {
                            credentialStore.saveDeviceMetadata(userId, deviceMetaData)
                        }
                        legacyCredentialStore.deleteDeviceKeyCredential(userId)
                    }
                }

                // migrate ASF device
                val asfDevice = legacyCredentialStore.retrieveASFDevice()
                asfDevice.id?.let {
                    credentialStore.saveASFDevice(asfDevice)
                    legacyCredentialStore.deleteASFDevice()
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
                    CredentialType.Amplify -> credentialStore.deleteCredential()
                    is CredentialType.Device -> credentialStore.deleteDeviceKeyCredential(credentialType.username)
                    CredentialType.ASF -> credentialStore.deleteASFDevice()
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
                    CredentialType.Amplify -> credentialStore.retrieveCredential()
                    is CredentialType.Device -> {
                        AmplifyCredential.DeviceData(credentialStore.retrieveDeviceMetadata(credentialType.username))
                    }
                    CredentialType.ASF -> credentialStore.retrieveASFDevice()
                }
                CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
            } catch (error: CredentialStoreError) {
                CredentialStoreEvent(CredentialStoreEvent.EventType.ThrowError(error))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun storeCredentialsAction(credentialType: CredentialType, credentials: AmplifyCredential) =
        Action<CredentialStoreEnvironment>("StoreCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                when (credentialType) {
                    CredentialType.Amplify -> credentialStore.saveCredential(credentials)
                    is CredentialType.Device -> {
                        val deviceData = credentials as? AmplifyCredential.DeviceMetaDataTypeCredential
                        deviceData?.let {
                            credentialStore.saveDeviceMetadata(credentialType.username, it.deviceMetadata)
                        }
                    }
                    CredentialType.ASF -> {
                        val asfDevice = credentials as? AmplifyCredential.ASFDevice
                        asfDevice?.id?.let { credentialStore.saveASFDevice(asfDevice) }
                    }
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
