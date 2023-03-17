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

package com.amplifyframework.notifications.pushnotifications

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
open class NotificationContentProvider : Parcelable {
    @Parcelize
    data class FCM(val from: String?, val content: Map<String, String>) : NotificationContentProvider()
}

@Parcelize
open class NotificationPayload(
    val contentProvider: NotificationContentProvider?,
    val channelId: String? = null,
    val targetClass: Class<*>? = null
) : Parcelable {

    @IgnoredOnParcel
    val rawData: Map<String, String> = extractRawData()

    internal constructor(builder: Builder) : this(builder.contentProvider, builder.channelId, builder.targetClass)

    private fun extractRawData() = when (contentProvider) {
        is NotificationContentProvider.FCM -> {
            contentProvider.content.plus("from" to contentProvider.from.toString())
        }
        else -> mapOf()
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()

        fun fromIntent(intent: Intent?): NotificationPayload? {
            return intent?.getParcelableExtra("amplifyNotificationPayload")
        }
    }

    class Builder {
        var contentProvider: NotificationContentProvider? = null
            private set
        var channelId: String? = null
            private set
        var targetClass: Class<*>? = null
            private set

        fun notificationContentProvider(contentProvider: NotificationContentProvider) = apply {
            this.contentProvider = contentProvider
        }

        fun notificationChannelId(channelId: String?) = apply { this.channelId = channelId }

        fun targetClass(targetClass: Class<*>?) = apply { this.targetClass = targetClass }

        fun build() = NotificationPayload(this)
    }
}
