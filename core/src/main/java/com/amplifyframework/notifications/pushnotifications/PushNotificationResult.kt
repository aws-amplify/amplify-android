package com.amplifyframework.notifications.pushnotifications

class PushNotificationResult(val type: PushResultType)

/**
 * Result values of handling a Pinpoint push message.
 */
sealed class PushResultType {
    /**
     * The message wasn't for pinpoint.
     */
    data class NotHandled(val id: String = "") : PushResultType()

    /**
     * The SDK handled the message and posted a local notification.
     */
    data class NotificationPosted(val id: String = "") : PushResultType()

    /**
     * The SDK handled the message, but no notification was posted, since
     * the app was in the foreground.
     */
    data class AppInForeground(val id: String = "") : PushResultType()

    /**
     * The SDK handled the message, but no notification was posted, since
     * the app was opted out.
     */
    data class OptedOut(val id: String = "") : PushResultType()

    /**
     * The SDK handled the message that indicated the local campaign
     * notification was opened.
     */
    data class NotificationOpened(val id: String = "") : PushResultType()

    /**
     * The SDK handled the message that indicated the local campaign
     * notification was opened.
     */
    data class Silent(val id: String = "") : PushResultType()
}
