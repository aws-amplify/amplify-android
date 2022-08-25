package com.amplifyframework.notifications.pushnotifications

typealias NotificationReceivedListener = (PushNotificationsDetails) -> Unit

class PushNotificationsDetails(val from: String, val data: Map<String, String>, val action: String)
