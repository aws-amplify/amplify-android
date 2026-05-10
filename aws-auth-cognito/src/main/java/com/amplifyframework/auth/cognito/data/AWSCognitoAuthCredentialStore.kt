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
    keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory()
) : AuthCredentialStore {

    companion object {
        const val AWS_KEY_VALUE_STORE_IDENTIFIER = "com.amplify.credentialStore"
        private const val KEY_SESSION = "session"
        private const val KEY_DEVICE_METADATA = "deviceMetadata"
        private const val KEY_ASF_DEVICE = "asfDevice"
    }

    private var keyValue: KeyValueRepository =
        keyValueRepoFactory.create(context, AWS_KEY_VALUE_STORE_IDENTIFIER)

    //region Save Credentials

    /**
     * Saves [credential] to the default session key (single-user / upstream-compat). When the
     * credential carries a userId via its [SignedInData] (i.e. [AmplifyCredential.UserPoolTypeCredential]),
     * the same payload is *additionally* written to a userId-prefixed key so per-user reads via
     * [retrieveCredential] (with userId) can locate it. Dual-write keeps single-user callers
     * (no-arg [retrieveCredential]) working unchanged while enabling multi-user routing.
     */
    override fun saveCredential(credential: AmplifyCredential) {
        val serialized = serializeCredential(credential)
        // Always write to the default key so single-user reads continue to work.
        keyValue.put(generateKey(KEY_SESSION), serialized)
        // Additionally write to a per-user key when the credential identifies a user.
        val userId = (credential as? AmplifyCredential.UserPoolTypeCredential)?.signedInData?.userId
        if (!userId.isNullOrEmpty()) {
            keyValue.put(generateUserScopedKey(userId, KEY_SESSION), serialized)
        }
    }

    override fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata) = keyValue.put(
        generateKey("$username.$KEY_DEVICE_METADATA"),
        serializeMetaData(deviceMetadata)
    )

    override fun saveASFDevice(device: AmplifyCredential.ASFDevice) = keyValue.put(
        generateKey(KEY_ASF_DEVICE),
        serializeASFDevice(device)
    )
    //endregion

    //region Retrieve Credentials

    /**
     * Returns the credential for [userId]. When [userId] is null/empty (single-user / upstream
     * path) reads the default session key. When [userId] is non-empty, prefers the userId-prefixed
     * key and falls back to the default key when the per-user entry is missing — which preserves
     * the upgrade path for installations created before multi-user.
     */
    override fun retrieveCredential(userId: String?): AmplifyCredential {
        if (userId.isNullOrEmpty()) {
            return deserializeCredential(keyValue.get(generateKey(KEY_SESSION)))
        }
        val perUser = deserializeCredential(keyValue.get(generateUserScopedKey(userId, KEY_SESSION)))
        return if (perUser !is AmplifyCredential.Empty) {
            perUser
        } else {
            deserializeCredential(keyValue.get(generateKey(KEY_SESSION)))
        }
    }

    override fun retrieveDeviceMetadata(username: String): DeviceMetadata = deserializeMetadata(
        keyValue.get(generateKey("$username.$KEY_DEVICE_METADATA"))
    )

    override fun retrieveASFDevice(): AmplifyCredential.ASFDevice = deserializeASFDevice(
        keyValue.get(generateKey(KEY_ASF_DEVICE))
    )
    //endregion

    //region Delete Credentials

    /**
     * Deletes the credential for [userId]. When [userId] is null/empty (single-user) removes the
     * default session key. When [userId] is non-empty, removes only the userId-prefixed key — the
     * default key is left intact since it is shared across users and the next [saveCredential]
     * will overwrite it.
     */
    override fun deleteCredential(userId: String?) {
        if (userId.isNullOrEmpty()) {
            keyValue.remove(generateKey(KEY_SESSION))
        } else {
            keyValue.remove(generateUserScopedKey(userId, KEY_SESSION))
        }
    }

    override fun deleteDeviceKeyCredential(username: String) = keyValue.remove(
        generateKey("$username.$KEY_DEVICE_METADATA")
    )

    override fun deleteASFDevice() = keyValue.remove(generateKey(KEY_ASF_DEVICE))
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

    private fun generateUserScopedKey(userId: String, keySuffix: String): String = "${userId}_${generateKey(keySuffix)}"

    //region Deserialization
    private fun deserializeCredential(encodedCredential: String?): AmplifyCredential = try {
        val credentials = encodedCredential?.let { Json.decodeFromString(it) as AmplifyCredential }
        credentials ?: AmplifyCredential.Empty
    } catch (e: Exception) {
        AmplifyCredential.Empty
    }

    private fun deserializeMetadata(encodedDeviceMetadata: String?): DeviceMetadata = try {
        val deviceMetadata = encodedDeviceMetadata?.let { Json.decodeFromString(it) as DeviceMetadata }
        deviceMetadata ?: DeviceMetadata.Empty
    } catch (e: Exception) {
        DeviceMetadata.Empty
    }

    private fun deserializeASFDevice(encodedASFDevice: String?): AmplifyCredential.ASFDevice = try {
        val asfDevice = encodedASFDevice?.let { Json.decodeFromString(it) as AmplifyCredential.ASFDevice }
        asfDevice ?: AmplifyCredential.ASFDevice(null)
    } catch (e: Exception) {
        AmplifyCredential.ASFDevice(null)
    }
    //endregion

    //region Serialization
    private fun serializeCredential(credential: AmplifyCredential): String = Json.encodeToString(credential)

    private fun serializeMetaData(deviceMetadata: DeviceMetadata): String = Json.encodeToString(deviceMetadata)

    private fun serializeASFDevice(device: AmplifyCredential.ASFDevice): String = Json.encodeToString(device)
    //endregion
}
