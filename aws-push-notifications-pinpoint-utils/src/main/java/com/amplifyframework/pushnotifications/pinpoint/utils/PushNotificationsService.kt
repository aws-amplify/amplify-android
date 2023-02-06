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

package com.amplifyframework.pushnotifications.pinpoint.utils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.amplifyframework.core.Amplify
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

abstract class PushNotificationsService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Amplify.Notifications.Push.registerDevice(token, { }, { })
    }

    override fun handleIntent(intent: Intent?) {
        val data = intent?.extras ?: Bundle()
        // First remove any parameters that shouldn't be passed to the app
        // * The wakelock ID set by the WakefulBroadcastReceiver
        data.remove("androidx.content.wakelockid")

        if (data.getString("pinpoint.campaign.campaign_id").isNullOrEmpty()) {
            Log.i(
                "PushNotificationsService",
                "Message payload does not contain pinpoint push notification message, which is not supported."
            )

            super.handleIntent(intent)
        } else {
            // message contains pinpoint push notification payload, handle the payload and show notification
            onMessageReceived(RemoteMessage(data))
        }
    }

    open fun processRemoteMessage(remoteMessage: RemoteMessage): NotificationPayload {
        val data = remoteMessage.data
        val action: HashMap<String, String> = HashMap()
        data[PushNotificationsConstants.PINPOINT_OPENAPP]?.let {
            action.put(PushNotificationsConstants.PINPOINT_OPENAPP, it)
        }
        data[PushNotificationsConstants.PINPOINT_URL]?.let {
            // force HTTPS URL scheme
            val urlHttps = it.replaceFirst("http", "https")
            action.put(PushNotificationsConstants.PINPOINT_URL, urlHttps)
        }
        data[PushNotificationsConstants.PINPOINT_DEEPLINK]?.let {
            action.put(PushNotificationsConstants.PINPOINT_DEEPLINK, it)
        }
        val title = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_TITLE]
        val body = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_BODY]
        val imageUrl = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_IMAGE]

        return NotificationPayload {
            notification(title, body, imageUrl)
            tapAction(action)
            silentPush = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_SILENTPUSH].equals("1")
            rawData = HashMap(remoteMessage.data)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("TAG", "Message: " + remoteMessage.data + "," + remoteMessage.notification)
        // handle payload and show notification
        val notificationPayload = processRemoteMessage(remoteMessage)
        val notificationDetails = notificationPayload.bundle()
        Amplify.Notifications.Push.handleNotificationReceived(notificationDetails, { }, { })
    }
}
