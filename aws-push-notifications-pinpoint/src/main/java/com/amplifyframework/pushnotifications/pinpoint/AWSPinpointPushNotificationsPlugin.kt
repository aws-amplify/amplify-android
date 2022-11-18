package com.amplifyframework.pushnotifications.pinpoint

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
import com.amplifyframework.notifications.pushnotifications.PushResultType
import com.amplifyframework.pushnotifications.pinpoint.utils.AWSPinpointPushNotificationUtils
import org.json.JSONObject

class AWSPinpointPushNotificationsPlugin : PushNotificationsPlugin<PinpointClient>() {

    companion object {
        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE = "amplify:aws-pinpoint-pushnotifications:%s"

        private const val AWS_PINPOINT_PUSHNOTIFICATIONS_PLUGIN_KEY = "awsPinpointPushNotifications"
    }

    private val logger =
        Amplify.Logging.forNamespace(AWS_PINPOINT_PUSHNOTIFICATIONS_LOG_NAMESPACE.format(this::class.java.simpleName))

    private lateinit var preferences: SharedPreferences

    private lateinit var context: Context

    private lateinit var pushNotificationUtils: AWSPinpointPushNotificationUtils

    private lateinit var foregroundNotificationListener: NotificationReceivedListener
    private lateinit var backgroundNotificationListener: NotificationReceivedListener

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        this.context = context
        pushNotificationUtils = AWSPinpointPushNotificationUtils(context)
        val preferencesKey = "appID" + "515d6767-01b7-49e5-8273-c8d11b0f331d"
        preferences = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
    }

    override fun identifyUser(userId: String) {
        try {
            Amplify.Analytics.identifyUser(userId, null)
        } catch (illegalStateException: IllegalStateException) {
            logger.warn("Failed to identify user, Analytics plugin not configured.")
            // TODO: update user profile endpoint
            println("Identity User: $userId")
        } catch (exception: Exception) {
            throw PushNotificationsException.default()
        }
    }

    override fun onNewToken(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) {
        // TODO: use credentials store instead of SharedPreferences
        putString("FCM_TOKEN", token)
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

    override fun registerForRemoteNotifications(
        details: PushNotificationsDetails,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) {
        try {
            val result = if (pushNotificationUtils.isAppInForeground()) {
                tryAnalyticsRecordEvent("foreground_event")
                foregroundNotificationListener.invoke(details)
                PushNotificationResult(PushResultType.AppInForeground())
            } else {
                pushNotificationUtils.showNotification(details)
                tryAnalyticsRecordEvent("background_event")
                backgroundNotificationListener.invoke(details)
                PushNotificationResult(PushResultType.NotificationPosted())
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
