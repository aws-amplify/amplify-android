package com.amplifyframework.notifications.pushnotifications

import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer

interface PushNotificationsCategoryBehavior {

    fun onNewToken(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>)

    fun onForegroundNotificationReceived(listener: NotificationReceivedListener)

    fun onBackgroundNotificationReceived(listener: NotificationReceivedListener)

    fun onNotificationOpened(onSuccess: Action, onError: Consumer<PushNotificationsException>)

    fun registerForRemoteNotifications(
        details: PushNotificationsDetails,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    )

    fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>)

    fun getToken(): String?

    fun getBadgeCount(): Int

    fun setBadgeCount(count: Int)
}
