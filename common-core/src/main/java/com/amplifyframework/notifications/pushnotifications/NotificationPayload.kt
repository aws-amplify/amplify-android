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
open class NotificationContentProvider internal constructor(open val content: Map<String, String>) : Parcelable {
    @Parcelize
    class FCM(override val content: Map<String, String>) : NotificationContentProvider(content)
}

@Parcelize
open class NotificationPayload(
    val contentProvider: NotificationContentProvider,
    val channelId: String? = null,
    val targetClass: Class<*>? = null
) : Parcelable {

    @IgnoredOnParcel
    val rawData: Map<String, String> = extractRawData()

    internal constructor(builder: Builder) : this(builder.contentProvider, builder.channelId, builder.targetClass)

    private fun extractRawData() = when (contentProvider) {
        is NotificationContentProvider.FCM -> contentProvider.content
        else -> mapOf()
    }

    companion object {
        @JvmStatic
        fun builder(contentProvider: NotificationContentProvider) = Builder(contentProvider)

        inline operator fun invoke(
            contentProvider: NotificationContentProvider,
            block: Builder.() -> Unit
        ) = Builder(contentProvider).apply(block).build()

        @JvmStatic
        fun fromIntent(intent: Intent?): NotificationPayload? {
            return intent?.getParcelableExtra("amplifyNotificationPayload")
        }
    }

    class Builder(val contentProvider: NotificationContentProvider) {
        var channelId: String? = null
            private set
        var targetClass: Class<*>? = null
            private set

        fun notificationChannelId(channelId: String?) = apply { this.channelId = channelId }

        fun targetClass(targetClass: Class<*>?) = apply { this.targetClass = targetClass }

        fun build() = NotificationPayload(this)
    }
}
