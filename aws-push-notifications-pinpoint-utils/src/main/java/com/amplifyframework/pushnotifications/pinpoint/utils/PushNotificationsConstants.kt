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
        const val PINPOINT_ATTRIBUTE_KEY = "pinpoint"
        const val PINPOINT_OPENAPP = "pinpoint.openApp"
        const val PINPOINT_URL = "pinpoint.url"
        const val PINPOINT_DEEPLINK = "pinpoint.deeplink"
        const val PINPOINT_NOTIFICATION_CHANNEL = "pinpoint.notification.channel"
        const val PINPOINT_NOTIFICATION_TITLE = "pinpoint.notification.title"
        const val PINPOINT_NOTIFICATION_BODY = "pinpoint.notification.body"
        const val PINPOINT_NOTIFICATION_IMAGE = "pinpoint.notification.imageUrl"
        const val PINPOINT_NOTIFICATION_SILENTPUSH = "pinpoint.notification.silentPush"
        const val PINPOINT_CAMPAIGN_PREFIX = "pinpoint.campaign."
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ID = "pinpoint.campaign.campaign_id"
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID = "pinpoint.campaign.campaign_activity_id"
        const val JOURNEY_ATTRIBUTE_KEY = "journey"
        const val JOURNEY_ID = "journey_id"
        const val JOURNEY_ACTIVITY_ID = "journey_activity_id"
    }
}
