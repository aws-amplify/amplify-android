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

class PushNotificationsConstants {
    companion object {
        const val AWS_PINPOINT_NOTIFICATION_CHANNEL = "pinpoint.notification.channel"

        const val AWS_PINPOINT_OPENAPP = "pinpoint.openApp"
        const val AWS_PINPOINT_URL = "pinpoint.url"
        const val AWS_PINPOINT_DEEPLINK = "pinpoint.deeplink"
        const val AWS_PINPOINT_NOTIFICATION_TITLE = "pinpoint.notification.title"
        const val AWS_PINPOINT_NOTIFICATION_BODY = "pinpoint.notification.body"
        const val AWS_PINPOINT_NOTIFICATION_IMAGE = "pinpoint.notification.imageUrl"
    }
}
