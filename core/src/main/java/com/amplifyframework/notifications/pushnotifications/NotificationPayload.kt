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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class NotificationPayload(
    val messageId: String? = null,
    val senderId: String? = null,
    val sendTime: Long? = null,
    val title: String? = null,
    val body: String? = null,
    val imageUrl: String? = null,
    val channelId: String? = null,
    val action: Map<String, String> = mapOf(),
    val silentPush: Boolean = false,
    val rawData: Map<String, String> = mapOf()
) : Parcelable
