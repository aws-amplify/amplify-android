/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class AWSCognitoAuthCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
    private val keyValueRepository: KeyValueRepository
) : AuthCredentialStore {

    companion object {
        private const val Key_Session = "session"
        private const val Key_DeviceMetadata = "deviceMetadata"
        private const val Key_ASFDevice = "asfDevice"
    }

    //region Save Credentials
    override fun saveCredential(credential: AmplifyCredential) = keyValueRepository.put(
        generateKey(Key_Session),
        serializeCredential(credential)
    )

    override fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata) = keyValueRepository.put(
        generateKey("$username.$Key_DeviceMetadata"),
        serializeMetaData(deviceMetadata)
    )

    override fun saveASFDevice(device: AmplifyCredential.ASFDevice) = keyValueRepository.put(
        generateKey(Key_ASFDevice),
        serializeASFDevice(device)
    )
    //endregion

    //region Retrieve Credentials
    override fun retrieveCredential(): AmplifyCredential = deserializeCredential(keyValueRepository.get(generateKey(Key_Session)))

    override fun retrieveDeviceMetadata(username: String): DeviceMetadata = deserializeMetadata(
        keyValueRepository.get(generateKey("$username.$Key_DeviceMetadata"))
    )

    override fun retrieveASFDevice(): AmplifyCredential.ASFDevice = deserializeASFDevice(
        keyValueRepository.get(generateKey(Key_ASFDevice))
    )
    //endregion

    //region Delete Credentials
    override fun deleteCredential() = keyValueRepository.remove(generateKey(Key_Session))

    override fun deleteDeviceKeyCredential(username: String) = keyValueRepository.remove(
        generateKey("$username.$Key_DeviceMetadata")
    )

    override fun deleteASFDevice() = keyValueRepository.remove(generateKey(Key_ASFDevice))
    //endregion

    private fun generateKey(keySuffix: String): String {
        var prefix = "amplify"

        authConfiguration.userPool?.let {
            prefix += ".${it.poolId}"
        }
        authConfiguration.identityPool?.let {
            prefix += ".${it.poolId}"
        }

        return prefix.plus(".$keySuffix")
    }

    //region Deserialization
    private fun deserializeCredential(encodedCredential: String?): AmplifyCredential {
        return try {
            val credentials = encodedCredential?.let { Json.decodeFromString(it) as AmplifyCredential }
            credentials ?: AmplifyCredential.Empty
        } catch (e: Exception) {
            AmplifyCredential.Empty
        }
    }

    private fun deserializeMetadata(encodedDeviceMetadata: String?): DeviceMetadata {
        return try {
            val deviceMetadata = encodedDeviceMetadata?.let { Json.decodeFromString(it) as DeviceMetadata }
            deviceMetadata ?: DeviceMetadata.Empty
        } catch (e: Exception) {
            DeviceMetadata.Empty
        }
    }

    private fun deserializeASFDevice(encodedASFDevice: String?): AmplifyCredential.ASFDevice {
        return try {
            val asfDevice = encodedASFDevice?.let { Json.decodeFromString(it) as AmplifyCredential.ASFDevice }
            asfDevice ?: AmplifyCredential.ASFDevice(null)
        } catch (e: Exception) {
            AmplifyCredential.ASFDevice(null)
        }
    }
    //endregion

    //region Serialization
    private fun serializeCredential(credential: AmplifyCredential): String {
        return Json.encodeToString(credential)
    }

    private fun serializeMetaData(deviceMetadata: DeviceMetadata): String {
        return Json.encodeToString(deviceMetadata)
    }

    private fun serializeASFDevice(device: AmplifyCredential.ASFDevice): String {
        return Json.encodeToString(device)
    }
    //endregion
}
