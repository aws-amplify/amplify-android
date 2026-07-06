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

import android.content.Context
import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.customerprofiles.CustomerProfilesClient
import aws.sdk.kotlin.services.customerprofiles.model.DeleteProfileObjectRequest
import aws.sdk.kotlin.services.customerprofiles.model.PutProfileObjectRequest
import com.amplifyframework.connect.internal.DeviceIdStore
import com.amplifyframework.connect.internal.IdentityResolver
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toSmithyProvider
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Client for registering devices and associating user identity with Amazon Connect
 * Customer Profiles.
 *
 * Provides methods to identify the current user, register/remove the device for
 * push notifications, and reset local state on sign-out.
 *
 * Example usage:
 * ```kotlin
 * val connectClient = AmplifyConnectClient(
 *     context = applicationContext,
 *     configuration = ConnectClientConfiguration(
 *         domainName = "my-domain",
 *         region = "us-east-1"
 *     ),
 *     credentialsProvider = myCredentialsProvider,
 *     identityIdProvider = { fetchCognitoSub() }
 * )
 *
 * connectClient.identifyUser(userId = "user-123")
 * connectClient.registerDevice(deviceToken = fcmToken, channelType = ChannelType.GCM)
 * ```
 *
 * @param context Android application context
 * @param configuration Domain name and region for Customer Profiles
 * @param credentialsProvider AWS credentials for authenticating API calls
 * @param identityIdProvider Suspending function that returns the Cognito sub (identity ID)
 */
class AmplifyConnectClient(
    context: Context,
    private val configuration: ConnectClientConfiguration,
    private val credentialsProvider: AwsCredentialsProvider<AwsCredentials>,
    private val identityIdProvider: suspend () -> String
) {
    private val logger: Logger = AmplifyLogging.logger<AmplifyConnectClient>()
    private val mutex = Mutex()

    @VisibleForTesting
    internal val customerProfilesClient: CustomerProfilesClient = CustomerProfilesClient {
        region = configuration.region
        this.credentialsProvider = this@AmplifyConnectClient.credentialsProvider.toSmithyProvider()
    }

    @VisibleForTesting
    internal val identityResolver = IdentityResolver(customerProfilesClient, configuration.domainName)

    @VisibleForTesting
    internal val deviceIdStore = DeviceIdStore(context.applicationContext)

    @Volatile
    private var profileId: String? = null

    @Volatile
    private var cognitoSub: String? = null

    /**
     * Identifies the current user by resolving or creating a Customer Profiles record
     * linked to their Cognito identity.
     *
     * This must be called before [registerDevice] or [removeDevice].
     *
     * @param userId Application-level user identifier (informational; the Cognito sub is the authoritative key)
     * @param userProfile Optional profile attributes to persist
     * @throws ConnectNotSignedInException if credentials cannot be resolved
     * @throws ConnectObjectTypeNotConfiguredException if the AmplifyDevice type is not provisioned
     * @throws AmplifyConnectException for other service errors
     */
    suspend fun identifyUser(userId: String, userProfile: UserProfile? = null) {
        mutex.withLock {
            try {
                logger.debug { "identifyUser: resolving identity for userId=$userId" }

                val sub = resolveIdentity()
                identityResolver.validateObjectType()
                val resolvedId = identityResolver.resolveProfile(sub, userProfile)

                profileId = resolvedId
                cognitoSub = sub
                logger.debug { "identifyUser: profile resolved, profileId=$resolvedId" }
            } catch (e: AmplifyConnectException) {
                throw e
            } catch (e: Exception) {
                throw AmplifyConnectException.from(e)
            }
        }
    }

    /**
     * Registers the device for push notifications by creating a device profile object
     * in Customer Profiles.
     *
     * Requires a prior [identifyUser] call. If the device was previously registered,
     * calling this again with a new token updates the registration in place
     * (keyed by stable device ID).
     *
     * @param deviceToken The platform push token (FCM registration token or APNs device token)
     * @param channelType The notification channel type
     * @throws ConnectNotSignedInException if identifyUser has not been called
     * @throws AmplifyConnectException for service errors
     */
    suspend fun registerDevice(deviceToken: String, channelType: ChannelType) {
        mutex.withLock {
            try {
                val currentProfileId = profileId ?: throw ConnectNotSignedInException(
                    message = "identifyUser must be called before registerDevice."
                )
                val currentSub = cognitoSub ?: throw ConnectNotSignedInException(
                    message = "Cognito identity not resolved. Call identifyUser first."
                )

                val deviceId = deviceIdStore.getOrCreate()
                logger.debug { "registerDevice: deviceId=$deviceId, channelType=$channelType" }

                val deviceObject = buildDeviceObject(
                    deviceId = deviceId,
                    deviceToken = deviceToken,
                    channelType = channelType,
                    cognitoUserId = currentSub
                )

                val request = PutProfileObjectRequest {
                    this.domainName = configuration.domainName
                    this.objectTypeName = IdentityResolver.OBJECT_TYPE_NAME
                    this.`object` = deviceObject
                }
                customerProfilesClient.putProfileObject(request)

                logger.debug { "registerDevice: device registered successfully" }
            } catch (e: AmplifyConnectException) {
                throw e
            } catch (e: Exception) {
                throw AmplifyConnectException.from(e)
            }
        }
    }

    /**
     * Removes the device registration from Customer Profiles and clears the local device ID.
     *
     * @throws ConnectDeviceNotRegisteredException if no device is registered
     * @throws ConnectNotSignedInException if identifyUser has not been called
     * @throws AmplifyConnectException for service errors
     */
    suspend fun removeDevice() {
        mutex.withLock {
            try {
                val currentProfileId = profileId ?: throw ConnectNotSignedInException(
                    message = "identifyUser must be called before removeDevice."
                )
                val deviceId = deviceIdStore.get() ?: throw ConnectDeviceNotRegisteredException()

                logger.debug { "removeDevice: removing deviceId=$deviceId" }

                val request = DeleteProfileObjectRequest {
                    this.domainName = configuration.domainName
                    this.objectTypeName = IdentityResolver.OBJECT_TYPE_NAME
                    this.profileId = currentProfileId
                    this.profileObjectUniqueKey = deviceId
                }
                customerProfilesClient.deleteProfileObject(request)

                deviceIdStore.clear()
                logger.debug { "removeDevice: device removed successfully" }
            } catch (e: AmplifyConnectException) {
                throw e
            } catch (e: Exception) {
                throw AmplifyConnectException.from(e)
            }
        }
    }

    /**
     * Resets all local state. Call this on user sign-out.
     *
     * Clears the cached profile ID, Cognito sub, and device ID.
     * Does not make any network calls or remove data from Customer Profiles.
     */
    suspend fun reset() {
        mutex.withLock {
            logger.debug { "reset: clearing local state" }
            profileId = null
            cognitoSub = null
            deviceIdStore.clear()
        }
    }

    private suspend fun resolveIdentity(): String = try {
        identityIdProvider()
    } catch (e: Exception) {
        throw ConnectNotSignedInException(
            message = "Failed to resolve Cognito identity: ${e.message}",
            cause = e
        )
    }

    private fun buildDeviceObject(
        deviceId: String,
        deviceToken: String,
        channelType: ChannelType,
        cognitoUserId: String
    ): String {
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceToken", deviceToken)
            put("channelType", channelType.name)
            put("cognitoUserId", cognitoUserId)
            put("platform", "Android")
            put("registeredAt", Instant.now().toString())
        }
        return json.toString()
    }
}
