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
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

sealed class NotificationContentProvider {
    data class FCM(val from: String?, val content: Map<String, String>) : NotificationContentProvider()
    data class ADM(val content: Intent?) : NotificationContentProvider()
    data class Baidu(val content: String?) : NotificationContentProvider()
}

@Parcelize
open class NotificationPayload(
    var title: String? = null,
    var body: String? = null,
    var imageUrl: String? = null,
    var channelId: String? = null,
    var action: Map<String, String> = mapOf(),
    var silentPush: Boolean = false,
    var rawData: Map<String, String> = mapOf(),
    var targetClass: Class<*>? = null
) : Parcelable {

    internal constructor(builder: Builder) : this() {
        targetClass = builder.targetClass
        channelId = builder.channelId

        when (val provider = builder.contentProvider) {
            is NotificationContentProvider.FCM -> {
                rawData = provider.content.plus("from" to provider.from.toString())
            }
            is NotificationContentProvider.ADM -> {
                val bundle = provider.content?.extras ?: Bundle()
                rawData = buildMap {
                    bundle.keySet().forEach { key ->
                        bundle.getString(key)?.let { put(key, it) }
                    }
                }
            }
            is NotificationContentProvider.Baidu -> {
                val jsonObject = provider.content?.let { JSONObject(it) }
                rawData = buildMap {
                    jsonObject?.keys()?.forEach { key ->
                        put(key, jsonObject.getString(key))
                    }
                }
            }
            else -> Unit
        }
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
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
