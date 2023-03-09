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

import androidx.annotation.RestrictTo
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsConstants
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
import kotlin.collections.HashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PinpointNotificationPayload internal constructor(
    messageId: String? = null,
    senderId: String? = null,
    sendTime: Long? = null,
    title: String? = null,
    body: String? = null,
    imageUrl: String? = null,
    channelId: String? = null,
    action: Map<String, String> = mapOf(),
    silentPush: Boolean = false,
    rawData: Map<String, String> = mapOf()
) : NotificationPayload(
    UUID.randomUUID().hashCode(), messageId, senderId, sendTime,
    title, body, imageUrl,
    channelId, action, silentPush, rawData
) {
    companion object {
        fun createFromRemoteMessage(remoteMessage: RemoteMessage): PinpointNotificationPayload? {
            if (!remoteMessage.data.keys.any { it.contains(PushNotificationsConstants.PINPOINT_PREFIX) }) return null

            val data = remoteMessage.data
            val messageId = remoteMessage.messageId
            val senderId = remoteMessage.senderId
            val sendTime = remoteMessage.sentTime
            val title = data[PushNotificationsConstants.TITLE]
                ?: data[PushNotificationsConstants.PINPOINT_NOTIFICATION_TITLE]
            val body = data[PushNotificationsConstants.MESSAGE]
                ?: data[PushNotificationsConstants.PINPOINT_NOTIFICATION_BODY]
            val imageUrl = data[PushNotificationsConstants.IMAGEURL]
                ?: data[PushNotificationsConstants.PINPOINT_NOTIFICATION_IMAGEURL]
            val channelId = PushNotificationsConstants.DEFAULT_NOTIFICATION_CHANNEL_ID
            val silentPush = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_SILENTPUSH].equals("1")
            val action: MutableMap<String, String> = mutableMapOf()

            data[PushNotificationsConstants.PINPOINT_OPENAPP]?.let {
                action.put(PushNotificationsConstants.OPENAPP, it)
            }
            data[PushNotificationsConstants.PINPOINT_URL]?.let {
                // force HTTPS URL scheme
                val urlHttps = it.replaceFirst("http://", "https://")
                action.put(PushNotificationsConstants.URL, urlHttps)
            }
            data[PushNotificationsConstants.PINPOINT_DEEPLINK]?.let {
                action.put(PushNotificationsConstants.DEEPLINK, it)
            }

            return PinpointNotificationPayload(
                messageId, senderId, sendTime,
                title, body, imageUrl,
                channelId, action, silentPush,
                HashMap(remoteMessage.data)
            )
        }
    }
}
