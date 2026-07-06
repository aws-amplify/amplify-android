/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.notifications.pushnotifications

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage

/**
 * A parsed, displayable view of a push notification, built from a backend-agnostic FCM payload.
 *
 * This is the vendor-neutral successor to the Pinpoint-specific payload parser. Unlike that parser,
 * it reads only standard payload keys (see [PushNotificationsConstants]) and never drops a valid FCM
 * message: any payload that carries data is parsed, so plain (non-Pinpoint) pushes are displayable.
 *
 * @property title notification title, if present
 * @property body notification body, if present
 * @property imageUrl remote image URL for the large icon, if present
 * @property action tap actions (any of "openApp", "url", "deeplink"), keyed by [PushNotificationsConstants]
 * @property silentPush true when the sender marked the message as data-only (do not display)
 */
class PushNotificationPayload internal constructor(
    val title: String? = null,
    val body: String? = null,
    val imageUrl: String? = null,
    val action: Map<String, String> = mapOf(),
    val silentPush: Boolean = false,
    channelId: String? = null,
    targetClass: Class<*>? = null,
    contentProvider: NotificationContentProvider
) : NotificationPayload(contentProvider, channelId, targetClass) {

    /**
     * Whether this payload has content that can be rendered as a system notification. A payload is
     * displayable when it is not a silent push and carries at least a title or a body.
     */
    val canBeDisplayed: Boolean
        get() = !silentPush && (!title.isNullOrEmpty() || !body.isNullOrEmpty())

    companion object {
        /**
         * Parse a [NotificationPayload] (as produced by the FCM service layer) into a displayable
         * [PushNotificationPayload].
         *
         * Returns null only when there is no data to parse. A payload that carries any data is always
         * returned, regardless of which backend produced it.
         */
        @JvmStatic
        fun fromNotificationPayload(payload: NotificationPayload): PushNotificationPayload? {
            val data = payload.rawData
            if (data.isEmpty()) return null

            val channelId = payload.channelId
                ?: data[PushNotificationsConstants.CHANNEL_ID]
                ?: PushNotificationsConstants.DEFAULT_NOTIFICATION_CHANNEL_ID

            return PushNotificationPayload(
                title = data[PushNotificationsConstants.TITLE],
                body = data[PushNotificationsConstants.BODY] ?: data[PushNotificationsConstants.MESSAGE],
                imageUrl = data[PushNotificationsConstants.IMAGE_URL],
                action = parseAction(data),
                silentPush = parseSilentPush(data[PushNotificationsConstants.SILENT_PUSH]),
                channelId = channelId,
                targetClass = payload.targetClass,
                contentProvider = payload.contentProvider
            )
        }

        /**
         * Parse a raw FCM data map into a displayable [PushNotificationPayload].
         *
         * Returns null only when [data] is empty.
         */
        @JvmStatic
        fun fromData(data: Map<String, String>): PushNotificationPayload? =
            fromNotificationPayload(NotificationPayload(NotificationContentProvider.FCM(data)))

        /**
         * Parse an FCM [RemoteMessage]'s data payload into a displayable [PushNotificationPayload].
         *
         * Returns null only when the message carries no data.
         */
        @JvmStatic
        fun fromRemoteMessage(message: RemoteMessage): PushNotificationPayload? = fromData(message.data)

        /**
         * Parse the [NotificationPayload] carried on an [Intent] (e.g. from a tapped notification) into
         * a displayable [PushNotificationPayload]. Returns null when the intent carries no payload.
         */
        @JvmStatic
        fun fromIntent(intent: Intent?): PushNotificationPayload? =
            NotificationPayload.fromIntent(intent)?.let { fromNotificationPayload(it) }

        private fun parseSilentPush(value: String?): Boolean = value == "1" || value.equals("true", ignoreCase = true)

        private fun parseAction(data: Map<String, String>): Map<String, String> {
            val action = mutableMapOf<String, String>()
            data[PushNotificationsConstants.OPEN_APP]?.let { action[PushNotificationsConstants.OPEN_APP] = it }
            data[PushNotificationsConstants.URL]?.let {
                // force HTTPS URL scheme
                action[PushNotificationsConstants.URL] = it.replaceFirst("http://", "https://")
            }
            data[PushNotificationsConstants.DEEPLINK]?.let { action[PushNotificationsConstants.DEEPLINK] = it }
            return action
        }
    }
}
