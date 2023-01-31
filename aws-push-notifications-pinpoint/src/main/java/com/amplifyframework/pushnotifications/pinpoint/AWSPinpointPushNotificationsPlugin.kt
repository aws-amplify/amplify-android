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
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidDeviceDetails
import com.amplifyframework.auth.cognito.BuildConfig
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin
import com.amplifyframework.pushnotifications.pinpoint.credentials.CognitoCredentialsProvider
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsService
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsUtils
import com.amplifyframework.pushnotifications.pinpoint.utils.toNotificationsPayload
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.jvm.Throws
import org.json.JSONObject

class AWSPinpointPushNotificationsPlugin : PushNotificationsPlugin<PinpointClient>() {

    companion object {
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE = "amplify:aws-pinpoint-pushnotifications:%s"

        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY = "awsPinpointPushNotificationsPlugin"
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    open class ServiceExtension : PushNotificationsService()

    private val logger =
        Amplify.Logging.forNamespace(AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE.format(this::class.java.simpleName))

    private lateinit var preferences: SharedPreferences

    private lateinit var configuration: AWSPinpointPushNotificationsConfiguration

    private lateinit var context: Context

    private lateinit var pushNotificationsUtils: PushNotificationsUtils

    private lateinit var pinpointClient: PinpointClient

    private lateinit var targetingClient: TargetingClient

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        try {
            this.context = context
            configuration = AWSPinpointPushNotificationsConfiguration.fromJson(pluginConfiguration)
            pushNotificationsUtils = PushNotificationsUtils(context)

            preferences = context.getSharedPreferences(
                configuration.appId + "515d6767-01b7-49e5-8273-c8d11b0f331d",
                Context.MODE_PRIVATE
            )

            pinpointClient = PinpointClient {
                region = configuration.region
                credentialsProvider = CognitoCredentialsProvider()
            }

            val androidAppDetails = AndroidAppDetails(context, configuration.appId)
            val androidDeviceDetails = AndroidDeviceDetails(context)
            targetingClient = TargetingClient(
                context,
                pinpointClient,
                preferences,
                androidAppDetails,
                androidDeviceDetails
            )

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    logger.info("Fetching FCM registration token failed: ${task.exception}")
                }
                val token = task.result
                registerDevice(token, { }, { })
                logger.info("Registering push notifications token: $token")
            }
        } catch (exception: Exception) {
            throw ConfigurationException(
                "Failed to configure AWSPinpointPushNotificationsPlugin.",
                "Make sure your amplifyconfiguration.json is valid.",
                exception
            )
        }
    }

    override fun identifyUser(userId: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        try {
            Amplify.Analytics.identifyUser(userId, null)
        } catch (illegalStateException: IllegalStateException) {
            logger.warn("Failed to identify user, Analytics plugin not configured.")
            // TODO: update user profile endpoint
        } catch (exception: Exception) {
            throw PushNotificationsException.default()
        }
    }

    override fun registerDevice(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        // TODO: use credentials store instead of SharedPreferences
        putString("FCM_TOKEN", token)
        // TODO: update pinpoint endpoint
    }

    override fun recordNotificationReceived(
        data: Map<String, String>,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        if (pushNotificationsUtils.isAppInForeground()) {
            tryAnalyticsRecordEvent("foreground_event")
            PushNotificationResult.AppInForeground()
        } else {
            tryAnalyticsRecordEvent("background_event")
            PushNotificationResult.NotificationPosted()
        }
        onSuccess.call()
    }

    override fun recordNotificationOpened(
        data: Map<String, String>,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        tryAnalyticsRecordEvent("notification_opened")
        PushNotificationResult.NotificationOpened()
        onSuccess.call()
    }

    override fun handleNotificationReceived(
        details: Bundle,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val payload = details.toNotificationsPayload()
            val result = if (pushNotificationsUtils.isAppInForeground()) {
                tryAnalyticsRecordEvent("foreground_event")
                PushNotificationResult.AppInForeground()
            } else {
                if (canShowNotification(details)) {
                    pushNotificationsUtils.showNotification(payload, AWSPinpointPushNotificationsActivity::class.java)
                }
                tryAnalyticsRecordEvent("background_event")
                PushNotificationResult.NotificationPosted()
            }
            onSuccess.accept(result)
        } catch (exception: Exception) {
            onError.accept(PushNotificationsException.default())
        }
    }

    private fun tryAnalyticsRecordEvent(eventName: String) {
        try {
            Amplify.Analytics.recordEvent(eventName)
        } catch (illegalStateException: IllegalStateException) {
            logger.warn("Failed to record $eventName with Analytics plugin, plugin not configured.")
        }
    }

    override fun getPluginKey() = AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY

    override fun getEscapeHatch() = pinpointClient

    override fun getVersion() = BuildConfig.VERSION_NAME

    private fun getString(key: String?, optValue: String?): String? {
        return preferences.getString(key, optValue)
    }

    private fun putString(key: String?, value: String?) {
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun canShowNotification(details: Bundle): Boolean {
        val silentPush = details.toNotificationsPayload().silentPush
        val optOut = targetingClient.currentEndpoint().optOut == "ALL"
        return !(!pushNotificationsUtils.areNotificationsEnabled() || silentPush || optOut)
    }
}
