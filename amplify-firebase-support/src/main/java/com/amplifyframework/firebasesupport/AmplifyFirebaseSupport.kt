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

package com.amplifyframework.firebasesupport

import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.google.firebase.messaging.RemoteMessage

class AmplifyFirebaseSupport {
    companion object {
        fun createFromRemoteMessage(remoteMessage: RemoteMessage) = NotificationPayload(
            remoteMessage.messageId,
            remoteMessage.senderId,
            remoteMessage.sentTime,
            remoteMessage.notification?.title,
            remoteMessage.notification?.body,
            remoteMessage.notification?.imageUrl.toString(),
            remoteMessage.notification?.channelId,
            rawData = remoteMessage.data
        )
    }
}
