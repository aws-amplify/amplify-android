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

package com.amplifyframework.pushnotifications.pinpoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.auth.cognito.BuildConfig
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.notifications.pushnotifications.NotificationReceivedListener
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsDetails
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsService
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsUtils
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class AWSPinpointPushNotificationsPlugin : PushNotificationsPlugin<PinpointClient>() {

    companion object {
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE = "amplify:aws-pinpoint-pushnotifications:%s"

        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY = "awsPinpointPushNotifications"
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    class ServiceExtension : PushNotificationsService()

    private val logger =
        Amplify.Logging.forNamespace(AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE.format(this::class.java.simpleName))

    private lateinit var preferences: SharedPreferences

    private lateinit var context: Context

    private lateinit var pushNotificationsUtils: PushNotificationsUtils

    private lateinit var foregroundNotificationListener: NotificationReceivedListener
    private lateinit var backgroundNotificationListener: NotificationReceivedListener

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        this.context = context
        pushNotificationsUtils = PushNotificationsUtils(context)
        val preferencesKey = "appID" + "515d6767-01b7-49e5-8273-c8d11b0f331d"
        preferences = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                logger.info("Fetching FCM registration token failed: ${task.exception}")
            }
            val token = task.result
            registerDevice(token, { }, { })
            logger.info("Registering push notifications token: $token")
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

    override fun onForegroundNotificationReceived(listener: NotificationReceivedListener) {
        foregroundNotificationListener = listener
    }

    override fun onBackgroundNotificationReceived(listener: NotificationReceivedListener) {
        backgroundNotificationListener = listener
    }

    override fun onNotificationOpened(onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        TODO("Not yet implemented")
    }

    override fun handleNotificationReceived(
        details: PushNotificationsDetails,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val result = if (pushNotificationsUtils.isAppInForeground()) {
                tryAnalyticsRecordEvent("foreground_event")
                foregroundNotificationListener.invoke(details)
                PushNotificationResult.AppInForeground()
            } else {
                pushNotificationsUtils.showNotification(details)
                tryAnalyticsRecordEvent("background_event")
                backgroundNotificationListener.invoke(details)
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

    override fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        TODO("Not yet implemented")
    }

    override fun getToken() = getString("FCM_TOKEN", null)

    override fun getBadgeCount(): Int {
        TODO("Not yet implemented")
    }

    override fun setBadgeCount(count: Int) {
        TODO("Not yet implemented")
    }

    override fun getPluginKey() = AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY

    override fun getEscapeHatch(): PinpointClient? {
        TODO("Not yet implemented")
    }

    override fun getVersion() = BuildConfig.VERSION_NAME

    private fun getString(key: String?, optValue: String?): String? {
        return preferences.getString(key, optValue)
    }

    private fun putString(key: String?, value: String?) {
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply()
    }
}
