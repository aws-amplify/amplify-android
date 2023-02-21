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

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
import kotlin.collections.HashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NotificationPayload internal constructor(
    val notificationId: Int,
    val messageId: String?,
    val senderId: String?,
    val sendTime: Long?,
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val channelId: String?,
    val action: HashMap<String, String>,
    val silentPush: Boolean,
    val rawData: HashMap<String, String>
) {
    constructor(
        messageId: String?,
        senderId: String?,
        sendTime: Long?,
        title: String?,
        body: String?,
        imageUrl: String?,
        channelId: String?,
        action: HashMap<String, String>,
        silentPush: Boolean = false,
        rawData: HashMap<String, String>
    ) : this(
        UUID.randomUUID().hashCode(),
        messageId, senderId, sendTime, title, body, imageUrl, channelId, action, silentPush, rawData
    )

    companion object {
        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    fun bundle(): Bundle {
        val payloadString = Json.encodeToString(this)
        return Bundle().apply { putString("payload", payloadString) }
    }

    class Builder {
        private var messageId: String? = null
        private var senderId: String? = null
        private var sendTime: Long? = null
        private var title: String? = null
        private var body: String? = null
        private var imageUrl: String? = null
        private var channelId: String? = null
        private var action: HashMap<String, String> = hashMapOf()
        var silentPush: Boolean = false
        var rawData: HashMap<String, String> = hashMapOf()

        fun notification(messageId: String?, senderId: String?, sendTime: Long?) {
            this.messageId = messageId
            this.senderId = senderId
            this.sendTime = sendTime
        }

        fun notificationContent(title: String?, body: String?, imageUrl: String?) = apply {
            this.title = title
            this.body = body
            this.imageUrl = imageUrl
        }

        fun notificationOptions(channelId: String?) {
            this.channelId = channelId
        }

        fun tapAction(action: HashMap<String, String>) = apply { this.action = action }

        fun build() = NotificationPayload(
            messageId, senderId, sendTime, title, body, imageUrl, channelId, action, silentPush, rawData
        )
    }
}

fun Bundle.toNotificationsPayload(): NotificationPayload {
    return Json.decodeFromString(getString("payload", ""))
}

fun processRemoteMessage(remoteMessage: RemoteMessage): NotificationPayload {
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
    val action: HashMap<String, String> = HashMap()

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

    return NotificationPayload {
        notification(messageId, senderId, sendTime)
        notificationContent(title, body, imageUrl)
        notificationOptions(channelId)
        tapAction(action)
        silentPush = data[PushNotificationsConstants.PINPOINT_NOTIFICATION_SILENTPUSH].equals("1")
        rawData = HashMap(remoteMessage.data)
    }
}
