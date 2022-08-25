package com.amplifyframework.notifications.pushnotifications

import com.amplifyframework.AmplifyException

class PushNotificationsException(message: String, recoverySuggestion: String) :
    AmplifyException(message, recoverySuggestion) {
    companion object {
        fun default() = PushNotificationsException("Push Notifications Exception", REPORT_BUG_TO_AWS_SUGGESTION)
    }
}
