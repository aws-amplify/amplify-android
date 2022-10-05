package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AWSCognitoDeviceStore(
    val context: Context,
    keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory()
) {

    companion object {
        const val awsDeviceKeyValueStoreIdentifier = "com.amplify.device.credentialStore"
    }

    private var keyValue: KeyValueRepository =
        keyValueRepoFactory.create(
            context,
            awsDeviceKeyValueStoreIdentifier,
            true
        )

    fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata) =
        keyValue.put(username, serializeMetaData(deviceMetadata))

    fun retrieveDeviceMetadata(username: String?): DeviceMetadata =
        username?.let { deserializeMetadata(keyValue.get(username)) } ?: DeviceMetadata.Empty

    private fun deserializeMetadata(encodedDeviceMetadata: String?): DeviceMetadata {
        val deviceMetadata = encodedDeviceMetadata?.let { Json.decodeFromString(it) as DeviceMetadata }
        return deviceMetadata ?: DeviceMetadata.Empty
    }

    private fun serializeMetaData(deviceMetadata: DeviceMetadata): String {
        return Json.encodeToString(deviceMetadata)
    }
}
