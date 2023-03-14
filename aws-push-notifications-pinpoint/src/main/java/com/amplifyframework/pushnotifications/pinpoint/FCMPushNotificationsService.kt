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
import android.util.Log
import com.amplifyframework.core.Amplify
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class FCMPushNotificationsService : FirebaseMessagingService() {

    private val TAG = "PushNotificationsService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Amplify.Notifications.Push.registerDevice(token, {
            Log.i(TAG, "Device token registered successfully.")
        }, {
            Log.i(TAG, "Device token registration failed.", it)
        })
    }

    override fun handleIntent(intent: Intent?) {
        val data = intent?.extras ?: Bundle()
        // First remove any parameters that shouldn't be passed to the app
        // * The wakelock ID set by the WakefulBroadcastReceiver
        data.remove("androidx.content.wakelockid")

        // get pinpoint notifications payload
        val notificationPayload = PinpointNotificationPayload.createFromRemoteMessage(RemoteMessage(data))
        when {
            // message contains pinpoint push notification payload, show notification
            notificationPayload != null -> onMessageReceived(notificationPayload)
            intent?.action == "com.google.firebase.messaging.NEW_TOKEN" -> super.handleIntent(intent)
            else -> {
                Log.i(
                    TAG, "Message payload does not contain pinpoint push notification message, which is not supported."
                )
                super.handleIntent(intent)
            }
        }
    }

    private fun onMessageReceived(payload: NotificationPayload) {
        Amplify.Notifications.Push.handleNotificationReceived(payload, {
            Log.i(TAG, "Notification handled successfully.")
        }, {
            Log.i(TAG, "Handle notification failed.", it)
        })
    }
}
