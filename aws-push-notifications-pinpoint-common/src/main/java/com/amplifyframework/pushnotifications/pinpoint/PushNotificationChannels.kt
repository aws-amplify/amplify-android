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

import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat

enum class NotificationImportance(val intValue: Int) {
    None(NotificationManagerCompat.IMPORTANCE_NONE),
    Min(NotificationManagerCompat.IMPORTANCE_MIN),
    Low(NotificationManagerCompat.IMPORTANCE_LOW),
    Default(NotificationManagerCompat.IMPORTANCE_DEFAULT),
    High(NotificationManagerCompat.IMPORTANCE_HIGH),
    Max(NotificationManagerCompat.IMPORTANCE_MAX)
}

class PushNotificationChannels internal constructor(private val manager: NotificationManagerCompat) {

    constructor(context: Context) : this(NotificationManagerCompat.from(context))

    fun create(
        configure: ChannelsCreator.() -> Unit
    ) = ChannelsCreator(manager).apply(configure)

    fun channelExists(channelId: String) = manager.getNotificationChannelCompat(channelId) != null
    fun groupExists(groupId: String) = manager.getNotificationChannelGroupCompat(groupId) != null

    fun deleteChannel(channelId: String) = manager.deleteNotificationChannel(channelId)
    fun deleteGroup(groupId: String) = manager.deleteNotificationChannelGroup(groupId)

    val channels: List<NotificationChannelCompat>
        get() = manager.notificationChannelsCompat

    val groups: List<NotificationChannelGroupCompat>
        get() = manager.notificationChannelGroupsCompat
}

@DslMarker
annotation class NotificationDslMarker

@NotificationDslMarker
class ChannelsCreator(private val managerCompat: NotificationManagerCompat) {
    fun channel(
        id: String,
        name: String,
        importance: NotificationImportance = NotificationImportance.Default,
        configure: ChannelBuilder.() -> Unit = {}
    ) {
        val channel = ChannelBuilder(id, name, importance).apply(configure).build()
        managerCompat.createNotificationChannel(channel)
    }

    fun group(
        id: String,
        name: String,
        configure: ChannelGroupBuilder.() -> Unit
    ) {
        val builder = ChannelGroupBuilder(id, name).apply(configure)
        val group = builder.build()
        managerCompat.createNotificationChannelGroup(group)
        for (channel in builder.channels) {
            managerCompat.createNotificationChannel(channel)
        }
    }
}

@NotificationDslMarker
class ChannelGroupBuilder(private val id: String, name: String) {
    private val builder = NotificationChannelGroupCompat.Builder(id).setName(name)
    internal val channels = mutableListOf<NotificationChannelCompat>()

    var description: String? = null
        set(value) {
            field = value
            builder.setDescription(value)
        }

    fun channel(
        id: String,
        name: String,
        importance: NotificationImportance = NotificationImportance.Default,
        configure: ChannelBuilder.() -> Unit = {}
    ) {
        channels += ChannelBuilder(id, name, importance).apply {
            configure()
            groupId = this@ChannelGroupBuilder.id
        }.build()
    }

    fun build() = builder.build()
}

@NotificationDslMarker
class ChannelBuilder(id: String, name: String, importance: NotificationImportance) {
    private val builder = NotificationChannelCompat.Builder(id, importance.intValue).setName(name)

    var groupId: String? = null
        set(value) {
            field = value
            builder.setGroup(value)
        }

    var description: String? = null
        set(value) {
            field = value
            builder.setDescription(value)
        }

    var showBadge: Boolean = true
        set(value) {
            field = value
            builder.setShowBadge(value)
        }

    var sound: Uri? = Settings.System.DEFAULT_NOTIFICATION_URI
        set(value) {
            field = value
            builder.setSound(value, audioAttributes)
        }

    var audioAttributes: AudioAttributes? = null
        set(value) {
            field = value
            builder.setSound(sound, value)
        }

    var lightsEnabled: Boolean = false
        set(value) {
            field = value
            builder.setLightsEnabled(value)
        }

    var lightColor: Int = 0
        set(value) {
            field = value
            builder.setLightColor(value)
        }

    var vibrationEnabled: Boolean = false
        set(value) {
            field = value
            builder.setVibrationEnabled(value)
        }

    var vibrationPattern: LongArray? = null
        set(value) {
            field = value
            builder.setVibrationEnabled(value != null)
            builder.setVibrationPattern(value)
        }

    fun addToConversation(parentChannelId: String, conversationId: String) {
        builder.setConversationId(parentChannelId, conversationId)
    }

    fun build() = builder.build()
}
