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

package com.amplifyframework.auth.cognito

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import com.amplifyframework.auth.cognito.asf.UserContextDataProvider
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import java.util.Date
import java.util.UUID

internal class AuthEnvironment internal constructor(
    val context: Context,
    val configuration: AuthConfiguration,
    val cognitoAuthService: AWSCognitoAuthService,
    val credentialStoreClient: StoreClientBehavior,
    private val userContextDataProvider: UserContextDataProvider? = null,
    val hostedUIClient: HostedUIClient?,
    val logger: Logger
) : Environment {

    companion object {
        /*
        Auth plugin needs to read from Pinpoint shared preferences, but we don't currently have an architecture
        that allows the plugins to pass data between each other. We are duplicating this suffix constant because it
        is internal to the Pinpoint class, which analytics does not pull in. If the Pinpoint suffix is updated, this
        needs updated as well.
         */
        const val PINPOINT_SHARED_PREFS_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
        const val PINPOINT_UNIQUE_ID_KEY = "UniqueId"
    }

    internal lateinit var srpHelper: SRPHelper
    private var cachedPinpointEndpointId: String? = null

        /*
        Auth plugin needs to read from Pinpoint shared preferences, but we don't currently have an architecture
        that allows the plugins to pass data between each other. We are retrieving the pinpointEndpointId by reading
        Pinpoint preferences constructed from pinpointAppId + a shared prefs suffix. If the storage of UniqueId changes
        in Pinpoint, we need to update here as well.
         */
    @SuppressLint("ApplySharedPref")
    @Synchronized
    fun getPinpointEndpointId(): String? {
        if (configuration.userPool?.pinpointAppId == null) return null
        if (cachedPinpointEndpointId != null) return cachedPinpointEndpointId

        val pinpointPrefs = context.getSharedPreferences(
            "${configuration.userPool.pinpointAppId}$PINPOINT_SHARED_PREFS_SUFFIX",
            Context.MODE_PRIVATE
        )

        val uniqueIdFromPrefs = pinpointPrefs.getString(PINPOINT_UNIQUE_ID_KEY, null)
        val uniqueId = if (uniqueIdFromPrefs == null) {
            val newUniqueId = UUID.randomUUID().toString()
            pinpointPrefs.edit(commit = true) { putString(PINPOINT_UNIQUE_ID_KEY, uniqueIdFromPrefs) }
            newUniqueId
        } else {
            uniqueIdFromPrefs
        }
        this.cachedPinpointEndpointId = uniqueId
        return uniqueId
    }

    suspend fun getUserContextData(username: String): String? {
        val asfDevice = credentialStoreClient.loadCredentials(CredentialType.ASF) as? AmplifyCredential.ASFDevice
        if (asfDevice == null) {
            logger.warn("loadCredentials returned unexpected AmplifyCredential Type.")
        }
        val deviceId = if (asfDevice?.id == null) {
            val newDeviceId = "${UUID.randomUUID()}:${Date().time}"
            val newASFDevice = AmplifyCredential.ASFDevice(newDeviceId)
            credentialStoreClient.storeCredentials(CredentialType.ASF, newASFDevice)
            newDeviceId
        } else {
            asfDevice.id
        }

        return userContextDataProvider?.getEncodedContextData(username, deviceId)
    }

    /**
     * Loads device metadata for [username].
     *
     * SDK versions <= 2.30.2 keyed device metadata by the value the user typed at sign-in (an alias
     * such as an email) rather than by the Cognito username. On those installs the metadata is
     * unreachable under the current key, so the DeviceKey is omitted from refresh and Cognito
     * rejects it with "Invalid Refresh Token.". [legacyUsernames] are alias values taken from the
     * user's own tokens; the first match is migrated to [username] so this happens at most once.
     */
    suspend fun getDeviceMetadata(
        username: String,
        legacyUsernames: List<String> = emptyList()
    ): DeviceMetadata.Metadata? {
        loadDeviceMetadata(username)?.let { return it }

        for (legacyUsername in legacyUsernames.filter { it != username }) {
            val legacyMetadata = loadDeviceMetadata(legacyUsername) ?: continue
            logger.info("Migrating device metadata stored by an earlier SDK version.")
            credentialStoreClient.storeCredentials(
                CredentialType.Device(username),
                AmplifyCredential.DeviceData(legacyMetadata)
            )
            credentialStoreClient.clearCredentials(CredentialType.Device(legacyUsername))
            return legacyMetadata
        }
        return null
    }

    private suspend fun loadDeviceMetadata(username: String): DeviceMetadata.Metadata? {
        val deviceCredentials =
            credentialStoreClient.loadCredentials(CredentialType.Device(username)) as? AmplifyCredential.DeviceData
        if (deviceCredentials == null) {
            logger.warn("loadCredentials returned unexpected AmplifyCredential Type.")
            return null
        }
        return deviceCredentials.deviceMetadata as? DeviceMetadata.Metadata
    }
}

internal fun AuthEnvironment.requireIdentityProviderClient() = cognitoAuthService.cognitoIdentityProviderClient
    ?: throw InvalidStateException("No Cognito identity provider client available")

internal fun StateMachineEvent.isAuthEvent(): AuthEvent.EventType? = (this as? AuthEvent)?.eventType

internal fun StateMachineEvent.isAuthenticationEvent(): AuthenticationEvent.EventType? =
    (this as? AuthenticationEvent)?.eventType

internal fun StateMachineEvent.isAuthorizationEvent(): AuthorizationEvent.EventType? =
    (this as? AuthorizationEvent)?.eventType

internal fun StateMachineEvent.isSignOutEvent(): SignOutEvent.EventType? = (this as? SignOutEvent)?.eventType

internal fun StateMachineEvent.isDeleteUserEvent(): DeleteUserEvent.EventType? = (this as? DeleteUserEvent)?.eventType

internal fun StateMachineEvent.isSignUpEvent(): SignUpEvent.EventType? = (this as? SignUpEvent)?.eventType
