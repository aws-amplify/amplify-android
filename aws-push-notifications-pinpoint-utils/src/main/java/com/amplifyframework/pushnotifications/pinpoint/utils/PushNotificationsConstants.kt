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
        const val PINPOINT_PREFIX = "pinpoint."
        const val NOTIFICATION_PREFIX = PINPOINT_PREFIX + "notification."
        const val CAMPAIGN_PREFIX = PINPOINT_PREFIX + "campaign."
        const val JOURNEY_ATTRIBUTE_KEY = "journey"
        const val MESSAGE_ATTRIBUTE_KEY = "message"
        const val IMAGEURL_ATTRIBUTE_KEY = "imageUrl"
        const val TITLE_ATTRIBUTE_KEY = "title"
        const val PINPOINT_OPENAPP = PINPOINT_PREFIX + "openApp"
        const val PINPOINT_URL = PINPOINT_PREFIX + "url"
        const val PINPOINT_DEEPLINK = PINPOINT_PREFIX + "deeplink"
        const val PINPOINT_NOTIFICATION_TITLE = NOTIFICATION_PREFIX + "title"
        const val PINPOINT_NOTIFICATION_BODY = NOTIFICATION_PREFIX + "body"
        const val PINPOINT_NOTIFICATION_IMAGEURL = NOTIFICATION_PREFIX + "imageUrl"
        const val PINPOINT_NOTIFICATION_SILENTPUSH = NOTIFICATION_PREFIX + "silentPush"
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ID = CAMPAIGN_PREFIX + "campaign_id"
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID = CAMPAIGN_PREFIX + "campaign_activity_id"
        const val JOURNEY_ID = "journey_id"
        const val JOURNEY_ACTIVITY_ID = "journey_activity_id"
        const val DEFAULT_NOTIFICATION_CHANNEL_ID = "PINPOINT.NOTIFICATION"
    }
}
