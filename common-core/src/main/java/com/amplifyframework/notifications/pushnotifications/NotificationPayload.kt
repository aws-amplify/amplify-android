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
import kotlinx.parcelize.Parcelize

open class NotificationContentProvider(val providerName: String) {
    data class FCM(val from: String?, val content: Map<String, String>) : NotificationContentProvider("FCM")
}

@Parcelize
open class NotificationPayload(
    var providerName: String? = null,
    var channelId: String? = null,
    var rawData: Map<String, String> = mapOf(),
    var targetClass: Class<*>? = null
) : Parcelable {

    internal constructor(builder: Builder) : this() {
        targetClass = builder.targetClass
        channelId = builder.channelId

        when (val provider = builder.contentProvider) {
            is NotificationContentProvider.FCM -> {
                providerName = provider.providerName
                rawData = provider.content.plus("from" to provider.from.toString())
            }
            else -> Unit
        }
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
