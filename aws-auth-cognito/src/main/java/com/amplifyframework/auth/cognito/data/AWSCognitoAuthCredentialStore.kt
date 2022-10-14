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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AWSCognitoAuthCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
    isPersistenceEnabled: Boolean = true,
    keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory(),
) : AuthCredentialStore {

    companion object {
        const val awsKeyValueStoreIdentifier = "com.amplify.credentialStore"
    }

    private val key = generateKey()
    private var keyValue: KeyValueRepository =
        keyValueRepoFactory.create(context, awsKeyValueStoreIdentifier, isPersistenceEnabled)

    override fun saveCredential(credential: AmplifyCredential) = keyValue.put(key, serializeCredential(credential))

    override fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata) = keyValue.put(
        username,
        serializeMetaData(deviceMetadata)
    )

    override fun retrieveDeviceMetadata(username: String): DeviceMetadata = deserializeMetadata(keyValue.get(username))

    override fun retrieveCredential(): AmplifyCredential = deserializeCredential(keyValue.get(key))

    override fun deleteCredential() = keyValue.remove(key)

    override fun deleteDeviceKeyCredential(username: String) = keyValue.remove(username)

    private fun generateKey(): String {
        var prefix = "amplify"
        val sessionKeySuffix = "session"

        authConfiguration.userPool?.let {
            prefix += ".${it.poolId}"
        }
        authConfiguration.identityPool?.let {
            prefix += ".${it.poolId}"
        }

        return prefix.plus(".$sessionKeySuffix")
    }

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

    private fun serializeCredential(credential: AmplifyCredential): String {
        return Json.encodeToString(credential)
    }

    private fun serializeMetaData(deviceMetadata: DeviceMetadata): String {
        return Json.encodeToString(deviceMetadata)
    }
}
