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

import android.content.Intent
import android.os.Bundle
import com.amplifyframework.core.Amplify
import com.amplifyframework.notifications.pushnotifications.NotificationContentProvider
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMPushNotificationService : FirebaseMessagingService() {
    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-push-notifications-pinpoint-utils")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Amplify.Notifications.Push.registerDevice(token, {
            LOG.info("Device token registered successfully.")
        }, {
            LOG.error("Device token registration failed.", it)
        })
    }

    override fun handleIntent(intent: Intent?) {
        val data = intent?.extras ?: Bundle()
        // First remove any parameters that shouldn't be passed to the app
        // * The wakelock ID set by the WakefulBroadcastReceiver
        data.remove("androidx.content.wakelockid")

        // create notifications payload
        val remoteMessage = RemoteMessage((data))
        val notificationPayload = NotificationPayload(NotificationContentProvider.FCM(remoteMessage.data))

        val isAmplifyMessage = Amplify.Notifications.Push.shouldHandleNotification(notificationPayload)
        if (isAmplifyMessage) {
            // message contains pinpoint push notification payload, show notification
            onMessageReceived(notificationPayload)
        } else {
            LOG.info("Ignoring messages that does not contain pinpoint push notification payload.")
            super.handleIntent(intent)
        }
    }

    private fun onMessageReceived(payload: NotificationPayload) {
        LOG.debug("Payload: ${payload.rawData}")

        Amplify.Notifications.Push.handleNotificationReceived(payload, {
            LOG.info("Notification handled successfully.")
        }, {
            LOG.error("Handle notification failed.", it)
        })
    }
}
