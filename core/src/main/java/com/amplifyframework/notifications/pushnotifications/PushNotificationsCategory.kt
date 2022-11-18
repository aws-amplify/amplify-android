package com.amplifyframework.notifications.pushnotifications

import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.Category
import com.amplifyframework.notifications.NotificationsCategoryBehavior

class PushNotificationsCategory :
    Category<PushNotificationsPlugin<*>>(),
    PushNotificationsCategoryBehavior,
    NotificationsCategoryBehavior {

    override fun getCategoryType() = selectedPlugin.categoryType

    override fun identifyUser(userId: String) = selectedPlugin.identifyUser(userId)

    override fun onNewToken(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.onNewToken(token, onSuccess, onError)

    override fun onForegroundNotificationReceived(listener: NotificationReceivedListener) =
        selectedPlugin.onForegroundNotificationReceived(listener)

    override fun onBackgroundNotificationReceived(listener: NotificationReceivedListener) =
        selectedPlugin.onBackgroundNotificationReceived(listener)

    override fun onNotificationOpened(onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.onNotificationOpened(onSuccess, onError)

    override fun registerForRemoteNotifications(
        details: PushNotificationsDetails,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.registerForRemoteNotifications(details, onSuccess, onError)

    override fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.getInitialNotification(onSuccess, onError)

    override fun getToken() = selectedPlugin.getToken()

    override fun getBadgeCount() = selectedPlugin.getBadgeCount()

    override fun setBadgeCount(count: Int) = selectedPlugin.setBadgeCount(count)
}
