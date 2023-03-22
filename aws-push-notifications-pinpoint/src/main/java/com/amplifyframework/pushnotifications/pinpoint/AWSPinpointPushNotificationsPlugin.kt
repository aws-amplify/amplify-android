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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import com.amplifyframework.AmplifyException
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin
import com.amplifyframework.pinpoint.core.AnalyticsClient
import com.amplifyframework.pinpoint.core.TargetingClient
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.database.PinpointDatabase
import com.amplifyframework.pinpoint.core.util.getUniqueId
import com.google.firebase.messaging.FirebaseMessaging
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import org.json.JSONObject

class AWSPinpointPushNotificationsPlugin : PushNotificationsPlugin<PinpointClient>() {

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-push-notifications-pinpoint")
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY = "awsPinpointPushNotificationsPlugin"

        private const val DATABASE_NAME = "awspushnotifications.db"
        private const val DEFAULT_AUTO_FLUSH_INTERVAL = 30000L
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PREFERENCES_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_LEGACY_KEY = "AWSPINPOINT.GCMTOKEN"
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY = "FCMDeviceToken"
    }

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
        interceptors += object : HttpInterceptor {
            override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
                context.executionContext.customUserAgentMetadata.add("pushnotifications", BuildConfig.VERSION_NAME)
                return super.modifyBeforeSerialization(context)
            }
        }
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
            androidDeviceDetails,
            null // notifications does not need session client
        )
    }

    private fun fetchFCMDeviceToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            try {
                if (!task.isSuccessful) {
                    LOG.error("Fetching FCM registration token failed: ${task.exception}")
                }
                val token = task.result
                registerDevice(token, {
                    LOG.info("Registering push notifications token: $token")
                }, {
                    throw it
                })
            } catch (exception: IOException) {
                LOG.error(
                    "Fetching token failed, this is a known issue in emulators, " +
                        "rerun the app: https://github.com/firebase/firebase-android-sdk/issues/3040",
                    exception
                )
            }
        }
    }

    override fun identifyUser(
        userId: String,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = _identifyUser(userId, null, onSuccess, onError)

    override fun identifyUser(
        userId: String,
        profile: UserProfile,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = _identifyUser(userId, profile, onSuccess, onError)

    private fun _identifyUser(
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
        payload: NotificationPayload,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val pinpointPayload = PinpointNotificationPayload.fromNotificationPayload(payload)
                ?: throw Exception("message does not contain pinpoint push notification payload")

            val isAppInForeground = pushNotificationsUtils.isAppInForeground()
            val attributes = mapOf("isAppInForeground" to isAppInForeground.toString())
            val eventSourceType = EventSourceType.getEventSourceType(pinpointPayload)
            val eventName = eventSourceType.getEventTypeReceived(isAppInForeground)
            val eventSourceAttributes = eventSourceType.attributeParser.parseAttributes(pinpointPayload)
            tryUpdateEventSourceGlobally(eventSourceAttributes)
            tryAnalyticsRecordEvent(eventName, attributes)
            onSuccess.call()
        } catch (exception: Exception) {
            onError.accept(
                PushNotificationsException(
                    "Failed to record notification received event.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                    exception
                )
            )
        }
    }

    override fun recordNotificationOpened(
        payload: NotificationPayload,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val pinpointPayload = PinpointNotificationPayload.fromNotificationPayload(payload)
                ?: throw Exception("message does not contain pinpoint push notification payload")

            val eventSourceType = EventSourceType.getEventSourceType(pinpointPayload)
            val eventSourceAttributes = eventSourceType.attributeParser.parseAttributes(pinpointPayload)
            tryUpdateEventSourceGlobally(eventSourceAttributes)
            tryAnalyticsRecordEvent(eventSourceType.eventTypeOpened)
            onSuccess.call()
        } catch (exception: Exception) {
            onError.accept(
                PushNotificationsException(
                    "Failed to record notification opened event.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION,
                    exception
                )
            )
        }
    }

    override fun shouldHandleNotification(
        payload: NotificationPayload
    ) = PinpointNotificationPayload.isPinpointNotificationPayload(payload)

    override fun handleNotificationReceived(
        payload: NotificationPayload,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val pinpointPayload = PinpointNotificationPayload.fromNotificationPayload(payload)
                ?: throw Exception("message does not contain pinpoint push notification payload")

            val isAppInForeground = pushNotificationsUtils.isAppInForeground()
            val eventSourceType = EventSourceType.getEventSourceType(pinpointPayload)
            val eventSourceAttributes = eventSourceType.attributeParser.parseAttributes(pinpointPayload)
            tryUpdateEventSourceGlobally(eventSourceAttributes)

            val eventName = eventSourceType.getEventTypeReceived(isAppInForeground)
            val result = when {
                isAppInForeground -> PushNotificationResult.AppInForeground
                pinpointPayload.silentPush -> PushNotificationResult.Silent
                canShowNotification(pinpointPayload) -> {
                    val notificationId = getNotificationRequestId(eventSourceAttributes, eventSourceType)
                    pushNotificationsUtils.showNotification(
                        notificationId, pinpointPayload, AWSPinpointPushNotificationsActivity::class.java
                    )
                    PushNotificationResult.NotificationPosted
                }
                else -> PushNotificationResult.OptedOut
            }
            // adding isAppInForeground and isOptedOut attributes to events for backwards compatibility with aws-sdk-android
            val attributes = mapOf(
                "isAppInForeground" to isAppInForeground.toString(),
                "isOptedOut" to (result is PushNotificationResult.OptedOut).toString()
            )
            tryAnalyticsRecordEvent(eventName, attributes)
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
     * @return a unique notification request ID that is given to the
     * NotificationManager for the notification. A random identifier
     * is generated in order to uniquely identify the notification
     * within the application.
     */
    private fun getNotificationRequestId(attributes: Map<String, String>, eventSourceType: EventSourceType): Int {
        // Adding a random unique identifier for direct sends. For a campaign,
        // use the eventSourceId and the activityId in order to prevent displaying
        // duplicate notifications from a campaign activity.
        val eventSourceId = attributes[eventSourceType.eventSourceIdAttributeKey]
        val activityId = attributes[eventSourceType.eventSourceActivityAttributeKey]
        return if (PushNotificationsConstants.DIRECT_CAMPAIGN_SEND == eventSourceId && activityId.isNullOrBlank()) {
            Random.nextInt()
        } else {
            "$eventSourceId:$activityId".hashCode()
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

    private fun tryAnalyticsRecordEvent(eventName: String, attributes: Map<String, String> = mapOf()) {
        try {
            val event = analyticsClient.createEvent(eventName, attributes.toMutableMap())
            analyticsClient.recordEvent(event)
            analyticsClient.flushEvents()
        } catch (exception: Exception) {
            throw Exception("Failed to record push notifications event $eventName.")
        }
    }

    override fun getPluginKey() = AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY

    override fun getEscapeHatch() = pinpointClient

    override fun getVersion() = BuildConfig.VERSION_NAME

    private fun canShowNotification(payload: PinpointNotificationPayload): Boolean {
        val notificationsEnabled = pushNotificationsUtils.areNotificationsEnabled()
        val silentPush = payload.silentPush
        return notificationsEnabled && !silentPush && deviceRegistered
    }
}
