/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

import androidx.annotation.VisibleForTesting
import com.amplifyframework.connect.internal.ConnectService
import com.amplifyframework.connect.internal.DeviceIdStore
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Client for the Amazon Connect Customer Profiles identify endpoint.
 *
 * All routes are SigV4-signed (`execute-api`). The backend Lambda derives
 * the caller's identity (Cognito sub or guest identityId) from the signer.
 *
 * Public API:
 * - [identifyUser] — sends profile attributes
 * - [registerDevice] — registers the device for push notifications
 * - [removeDevice] — removes the device from the profile
 *
 * ## Deferred (Phase 2)
 *
 * Network calls are synchronous and surface [ConnectNetworkException] when
 * offline. The offline queue, connectivity drain, and retry/backoff are not
 * implemented here.
 *
 * @param configuration Endpoint and region from amplify_outputs
 * @param credentialsProvider Resolves AWS credentials for SigV4 signing
 * @param deviceIdStore Persistent device id store (shared key with enrichment)
 * @param platform Client OS platform (e.g. "Android")
 * @param appVersion Application version string
 * @param channelType Push channel type for this device
 */
class AmplifyConnectClient(
    configuration: ConnectClientConfiguration,
    private val credentialsProvider: ConnectCredentialsProvider,
    private val deviceIdStore: DeviceIdStore,
    private val platform: String? = null,
    private val appVersion: String? = null,
    private val channelType: ChannelType = ChannelType.GCM
) {
    @VisibleForTesting
    internal var service: ConnectService = ConnectService(
        endpoint = configuration.endpoint,
        region = configuration.region
    )

    @VisibleForTesting
    internal constructor(
        configuration: ConnectClientConfiguration,
        credentialsProvider: ConnectCredentialsProvider,
        deviceIdStore: DeviceIdStore,
        platform: String?,
        appVersion: String?,
        channelType: ChannelType,
        service: ConnectService
    ) : this(configuration, credentialsProvider, deviceIdStore, platform, appVersion, channelType) {
        this.service = service
    }

    private val logger: Logger = AmplifyLogging.logger<AmplifyConnectClient>()

    /**
     * Sends user profile attributes to the Customer Profiles endpoint.
     *
     * POST /identify-user with body `{ userProfile }`.
     *
     * @param userProfile User profile attributes to send
     * @throws ConnectNotSignedInException if credentials cannot be resolved
     * @throws ConnectNetworkException on transport failure
     * @throws AmplifyConnectException for endpoint errors
     */
    suspend fun identifyUser(userProfile: UserProfile) {
        val credentials = credentialsProvider.resolve()
        val body = buildJsonObject {
            putJsonObject("userProfile") {
                userProfile.email?.let { put("email", it) }
                userProfile.name?.let { put("name", it) }
                userProfile.phone?.let { put("phone", it) }
                userProfile.customAttributes?.takeIf { it.isNotEmpty() }?.let { attrs ->
                    putJsonObject("customAttributes") {
                        attrs.forEach { (k, v) -> put(k, v) }
                    }
                }
                userProfile.location?.let { loc ->
                    putJsonObject("location") {
                        loc.city?.let { put("city", it) }
                        loc.country?.let { put("country", it) }
                        loc.postalCode?.let { put("postalCode", it) }
                        loc.region?.let { put("region", it) }
                    }
                }
            }
        }
        service.identifyUser(credentials, body.toString())
        logger.info { "identifyUser sent" }
    }

    /**
     * Registers the current device for push notifications.
     *
     * POST /register-device with body `{ device: { token, deviceId, platform?, appVersion?, channelType } }`.
     *
     * The [deviceId] is resolved from the shared device-id store
     * (`com.amplifyframework.device_id`). It is the server-side upsert key —
     * stable across launches and token refreshes so a device always maps to
     * one profile object.
     *
     * @param token The platform push token (FCM registration token or APNs device token)
     * @throws ConnectNotSignedInException if credentials cannot be resolved
     * @throws ConnectNetworkException on transport failure
     * @throws AmplifyConnectException for endpoint errors
     */
    suspend fun registerDevice(token: String) {
        val credentials = credentialsProvider.resolve()
        val deviceId = deviceIdStore.getOrCreate()
        val body = buildJsonObject {
            putJsonObject("device") {
                put("token", token)
                put("deviceId", deviceId)
                platform?.let { put("platform", it) }
                appVersion?.let { put("appVersion", it) }
                put("channelType", channelType.value)
            }
        }
        service.registerDevice(credentials, body.toString())
        logger.info { "registerDevice sent for channel ${channelType.value}" }
    }

    /**
     * Removes the current device from the caller's profile.
     *
     * POST /remove-device with body `{ deviceId }`.
     *
     * The [deviceId] is resolved from the shared device-id store.
     *
     * @throws ConnectNotSignedInException if credentials cannot be resolved
     * @throws ConnectNetworkException on transport failure
     * @throws AmplifyConnectException for endpoint errors
     */
    suspend fun removeDevice() {
        val credentials = credentialsProvider.resolve()
        val deviceId = deviceIdStore.getOrCreate()
        val body = buildJsonObject {
            put("deviceId", deviceId)
        }
        service.removeDevice(credentials, body.toString())
        logger.info { "removeDevice sent" }
    }
}
