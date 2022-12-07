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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class NotificationsPayload internal constructor(val notificationId:Int, val title: String?, val body: String?, val imageUrl: String?) {

    constructor(title: String?, body: String?, imageUrl: String?):this(UUID.randomUUID().hashCode(), title, body, imageUrl)

    fun bundle(): Bundle {
        val payloadString = toString()
        return Bundle().apply { putString("payload", Json.encodeToString(payloadString)) }
    }
}

fun Bundle.toNotificationsPayload(): NotificationsPayload {
    return Json.decodeFromString(getString("payload", ""))
}