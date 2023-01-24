/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    val action: HashMap<String, String>?,
    val imageUrl: String?,
) {

    constructor(title: String?, body: String?, action: HashMap<String, String>?, imageUrl: String?) : this(
        UUID.randomUUID().hashCode(),
        title,
        body,
        action,
        imageUrl
    )

    fun bundle(): Bundle {
        val payloadString = Json.encodeToString(this)
        return Bundle().apply { putString("payload", payloadString) }
    }
}

fun Bundle.toNotificationsPayload(): NotificationPayload {
    return Json.decodeFromString(getString("payload", ""))
}
