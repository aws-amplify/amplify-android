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

package com.amplifyframework.pushnotifications.pinpoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import com.amplifyframework.AmplifyException
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.analytics.pinpoint.targeting.AnalyticsClient
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.targeting.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.targeting.util.getUniqueId
import com.amplifyframework.auth.cognito.BuildConfig
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin
import com.amplifyframework.pushnotifications.pinpoint.credentials.CognitoCredentialsProvider
import com.amplifyframework.pushnotifications.pinpoint.utils.NotificationPayload
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsService
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsUtils
import com.amplifyframework.pushnotifications.pinpoint.utils.toNotificationsPayload
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

class AWSPinpointPushNotificationsPlugin : PushNotificationsPlugin<PinpointClient>() {

    companion object {
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE = "amplify:aws-pinpoint-pushnotifications:%s"
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY = "awsPinpointPushNotificationsPlugin"

        private const val DATABASE_NAME = "awspushnotifications.db"
        private const val DEFAULT_AUTO_FLUSH_INTERVAL = 30000L
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PREFERENCES_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_LEGACY_KEY = "AWSPINPOINT.GCMTOKEN"
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY = "FCMDeviceToken"
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    open class ServiceExtension : PushNotificationsService()

    private val logger =
        Amplify.Logging.forNamespace(AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE.format(this::class.java.simpleName))

    private lateinit var preferences: SharedPreferences

    private lateinit var store: KeyValueRepository

    private lateinit var configuration: AWSPinpointPushNotificationsConfiguration

    private lateinit var context: Context

    private lateinit var pushNotificationsUtils: PushNotificationsUtils

    private lateinit var pinpointClient: PinpointClient

    private lateinit var targetingClient: TargetingClient

    private lateinit var analyticsClient: AnalyticsClient

    private var eventSourceAttributes = ConcurrentHashMap<String, String>()

    private var deviceRegistered = false

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        try {
            this.context = context
            configuration = AWSPinpointPushNotificationsConfiguration.fromJson(pluginConfiguration)
            pushNotificationsUtils = PushNotificationsUtils(context)

            val androidAppDetails = AndroidAppDetails(context, configuration.appId)
            val androidDeviceDetails = AndroidDeviceDetails(context)

            createAndMigrateStore()
            pinpointClient = createPinpointClient()
            targetingClient = createTargetingClient(androidAppDetails, androidDeviceDetails)
            analyticsClient = createAnalyticsClient(androidAppDetails, androidDeviceDetails)
            fetchFCMDeviceToken()
        } catch (exception: Exception) {
            throw PushNotificationsException(
                "Failed to configure AWSPinpointPushNotificationsPlugin.",
                "Make sure your amplifyconfiguration.json is valid.",
                exception
            )
        }
    }

    private fun createAndMigrateStore() {
        preferences = context.getSharedPreferences(
            configuration.appId + AWS_PINPOINT_PUSHNOTIFICATIONS_PREFERENCES_SUFFIX,
            Context.MODE_PRIVATE
        )
        store = EncryptedKeyValueRepository(
            context,
            configuration.appId + AWS_PINPOINT_PUSHNOTIFICATIONS_PREFERENCES_SUFFIX
        )

        val deviceToken = preferences.getString(AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_LEGACY_KEY, null)
        deviceToken?.let {
            store.put(AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY, it)
            preferences.edit { remove(AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_LEGACY_KEY).apply() }
        }
    }

    private fun createPinpointClient() = PinpointClient {
        region = configuration.region
        credentialsProvider = CognitoCredentialsProvider()
    }

    private fun createTargetingClient(
        androidAppDetails: AndroidAppDetails,
        androidDeviceDetails: AndroidDeviceDetails
    ): TargetingClient {
        return TargetingClient(context, pinpointClient, preferences, androidAppDetails, androidDeviceDetails)
    }

    private fun createAnalyticsClient(
        androidAppDetails: AndroidAppDetails,
        androidDeviceDetails: AndroidDeviceDetails
    ): AnalyticsClient {
        val pinpointDatabase = PinpointDatabase(context, DATABASE_NAME)
        return AnalyticsClient(
            context,
            DEFAULT_AUTO_FLUSH_INTERVAL,
            pinpointClient,
            targetingClient,
            pinpointDatabase,
            preferences.getUniqueId(),
            androidAppDetails,
            androidDeviceDetails
        )
    }

    private fun fetchFCMDeviceToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                logger.info("Fetching FCM registration token failed: ${task.exception}")
            }
            val token = task.result
            registerDevice(token, {
                logger.info("Registering push notifications token: $token")
            }, {
                throw it
            })
        }
    }

    override fun identifyUser(
        userId: String,
        profile: UserProfile?,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            targetingClient.identifyUser(userId, profile)
            onSuccess.call()
        } catch (exception: Exception) {
            onError.accept(
                PushNotificationsException(
                    "Failed to identify user with the service.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                    exception
                )
            )
        }
    }

    override fun registerDevice(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        try {
            store.put(AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY, token)
            // targetingClient needs to send the address, optOut etc. to Pinpoint so we can receive campaigns/journeys
            val endpointProfile = targetingClient.currentEndpoint().apply {
                channelType = ChannelType.Gcm
                address = token
            }

            targetingClient.updateEndpointProfile(endpointProfile)
            deviceRegistered = true
            onSuccess.call()
        } catch (exception: Exception) {
            onError.accept(
                PushNotificationsException(
                    "Failed to register FCM device token with the service.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                    exception
                )
            )
        }
    }

    override fun recordNotificationReceived(
        data: Bundle,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val payload = data.toNotificationsPayload()
            val eventSourceType = EventSourceType.getEventSourceType(payload)
            if (pushNotificationsUtils.isAppInForeground()) {
                tryAnalyticsRecordEvent(eventSourceType.getEventTypeReceivedForeground())
            } else {
                tryAnalyticsRecordEvent(eventSourceType.getEventTypeReceivedBackground())
            }
            onSuccess.call()
        } catch (exception: PushNotificationsException) {
            onError.accept(exception)
        }
    }

    override fun recordNotificationOpened(
        data: Bundle,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val payload = data.toNotificationsPayload()
            val eventSourceType = EventSourceType.getEventSourceType(payload)
            tryAnalyticsRecordEvent(eventSourceType.getEventTypeOpened())
            onSuccess.call()
        } catch (exception: PushNotificationsException) {
            onError.accept(exception)
        }
    }

    override fun handleNotificationReceived(
        details: Bundle,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val payload = details.toNotificationsPayload()
            val eventSourceType = EventSourceType.getEventSourceType(payload)

            val eventSourceAttributes = eventSourceType.getAttributeParser().parseAttributes(payload)
            tryUpdateEventSourceGlobally(eventSourceAttributes)

            val result = if (pushNotificationsUtils.isAppInForeground()) {
                tryAnalyticsRecordEvent(eventSourceType.getEventTypeReceivedForeground())
                PushNotificationResult.AppInForeground()
            } else {
                if (canShowNotification(payload)) {
                    pushNotificationsUtils.showNotification(payload, AWSPinpointPushNotificationsActivity::class.java)
                } // TODO: else add isOptedOut to event
                tryAnalyticsRecordEvent(eventSourceType.getEventTypeReceivedBackground())
                PushNotificationResult.NotificationPosted()
            }
            onSuccess.accept(result)
        } catch (exception: Exception) {
            onError.accept(
                PushNotificationsException(
                    "Failed to handle push notification message.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                    exception
                )
            )
        }
    }

    /**
     * Removes the attributes currently stored in eventSourceAttributes
     * from the globalAttributes collection. This is called
     * when we want to switch which event source (campaign, journey, or other future
     * pinpoint construct) pinpoint events are attributed to.
     */
    private fun clearEventSourceAttributes() {
        for (key in eventSourceAttributes.keys()) {
            analyticsClient.removeGlobalAttribute(key)
        }
    }

    /**
     * Replaces the current event source attributes (if any)
     * with a new set of event source attributes
     * @param attributes map of attribute values
     */
    private fun tryUpdateEventSourceGlobally(attributes: Map<String, String>) {
        // Remove attributes from previous update call
        clearEventSourceAttributes()
        for ((key, value) in attributes) {
            analyticsClient.addGlobalAttribute(key, value)

            // Hold on to attributes so we can remove them later
            eventSourceAttributes[key] = value
        }
    }

    private fun tryAnalyticsRecordEvent(eventName: String) {
        try {
            val event = analyticsClient.createEvent(eventName)
            //TODO: globals and foreground key
            event.attributes.plus("isAppInForeground" to pushNotificationsUtils.isAppInForeground().toString())
            analyticsClient.recordEvent(event)
            analyticsClient.flushEvents()
        } catch (exception: Exception) {
            throw PushNotificationsException(
                "Failed to record push notifications event $eventName.",
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                exception
            )
        }
    }

    override fun getPluginKey() = AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY

    override fun getEscapeHatch() = pinpointClient

    override fun getVersion() = BuildConfig.VERSION_NAME

    private fun canShowNotification(payload: NotificationPayload): Boolean {
        val notificationsEnabled = pushNotificationsUtils.areNotificationsEnabled()
        val silentPush = payload.silentPush
        return notificationsEnabled && !silentPush && deviceRegistered
    }
}
