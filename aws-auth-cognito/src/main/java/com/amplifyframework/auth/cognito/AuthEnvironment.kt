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

    suspend fun getDeviceMetadata(username: String): DeviceMetadata.Metadata? {
        var deviceCredentials =
            credentialStoreClient.loadCredentials(CredentialType.Device(username)) as? AmplifyCredential.DeviceData
        if (deviceCredentials == null) {
            logger.warn("loadCredentials returned unexpected AmplifyCredential Type.")
            deviceCredentials = AmplifyCredential.DeviceData(DeviceMetadata.Empty)
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
