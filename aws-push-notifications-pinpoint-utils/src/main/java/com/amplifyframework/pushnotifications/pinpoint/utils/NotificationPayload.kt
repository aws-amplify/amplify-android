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
import java.util.UUID
import kotlin.collections.HashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NotificationPayload internal constructor(
    val notificationId: Int,
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val action: HashMap<String, String>,
    val silentPush: Boolean,
    val rawData: HashMap<String, String>
) {
    constructor(
        title: String?,
        body: String?,
        imageUrl: String?,
        action: HashMap<String, String>,
        silentPush: Boolean = false,
        rawData: HashMap<String, String>
    ) : this(UUID.randomUUID().hashCode(), title, body, imageUrl, action, silentPush, rawData)

    companion object {
        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    fun bundle(): Bundle {
        val payloadString = Json.encodeToString(this)
        return Bundle().apply { putString("payload", payloadString) }
    }

    class Builder {
        private var title: String? = null
        private var body: String? = null
        private var imageUrl: String? = null
        private var action: HashMap<String, String> = hashMapOf()
        var silentPush: Boolean = false
        var rawData: HashMap<String, String> = hashMapOf()

        fun notification(title: String?, body: String?, imageUrl: String?) = apply {
            this.title = title
            this.body = body
            this.imageUrl = imageUrl
        }

        fun tapAction(action: HashMap<String, String>) = apply { this.action = action }

        fun build() = NotificationPayload(title, body, imageUrl, action, silentPush, rawData)
    }
}

fun Bundle.toNotificationsPayload(): NotificationPayload {
    return Json.decodeFromString(getString("payload", ""))
}
