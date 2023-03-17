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

package com.amplifyframework.kotlin.notifications.pushnotifications

import com.amplifyframework.kotlin.notifications.Notifications
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException

interface Push : Notifications {
    /**
     * Registers device token from FCM with the service.
     */
    @Throws(PushNotificationsException::class)
    suspend fun registerDevice(token: String)

    /**
     * Registers that a notification was received while the app was in the foreground/background/kill.
     */
    @Throws(PushNotificationsException::class)
    suspend fun recordNotificationReceived(payload: NotificationPayload)

    /**
     * Registers that a user opened a notification.
     */
    @Throws(PushNotificationsException::class)
    suspend fun recordNotificationOpened(payload: NotificationPayload)

    /**
     * Returns whether Amplify can handle the notification payload.
     */
    fun shouldHandleNotification(payload: NotificationPayload): Boolean

    /**
     * Displays notification on the system tray if app is background/killed state.
     */
    @Throws(PushNotificationsException::class)
    suspend fun handleNotificationReceived(payload: NotificationPayload): PushNotificationResult
}
