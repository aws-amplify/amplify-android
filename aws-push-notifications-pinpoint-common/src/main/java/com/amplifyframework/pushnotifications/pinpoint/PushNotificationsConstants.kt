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

package com.amplifyframework.pushnotifications.pinpoint

import com.amplifyframework.annotations.InternalAmplifyApi

@InternalAmplifyApi
class PushNotificationsConstants {
    companion object {
        const val PINPOINT_PREFIX = "pinpoint" // pinpoint
        const val NOTIFICATION_PREFIX = "$PINPOINT_PREFIX.notification." // pinpoint.notification.
        const val CAMPAIGN_PREFIX = "$PINPOINT_PREFIX.campaign." // pinpoint.campaign.
        const val OPENAPP = "openApp" // openApp
        const val URL = "url" // url
        const val DEEPLINK = "deeplink" // deeplink
        const val TITLE = "title" // title
        const val MESSAGE = "message" // message
        const val IMAGEURL = "imageUrl" // imageUrl
        const val JOURNEY = "journey" // journey
        const val JOURNEY_ID = "journey_id" // journey_id
        const val JOURNEY_ACTIVITY_ID = "journey_activity_id" // journey_activity_id
        const val PINPOINT_OPENAPP = "$PINPOINT_PREFIX.$OPENAPP" // pinpoint.openApp
        const val PINPOINT_URL = "$PINPOINT_PREFIX.$URL" // pinpoint.url
        const val PINPOINT_DEEPLINK = "$PINPOINT_PREFIX.$DEEPLINK" // pinpoint.deeplink
        const val PINPOINT_NOTIFICATION_TITLE = "$NOTIFICATION_PREFIX$TITLE" // pinpoint.notification.title
        const val PINPOINT_NOTIFICATION_BODY = "${NOTIFICATION_PREFIX}body" // pinpoint.notification.body
        const val PINPOINT_NOTIFICATION_IMAGEURL = "$NOTIFICATION_PREFIX$IMAGEURL" // pinpoint.notification.imageUrl
        // pinpoint.notification.silentPush
        const val PINPOINT_NOTIFICATION_SILENTPUSH = "${NOTIFICATION_PREFIX}silentPush"
        const val CAMPAIGN_ID = "campaign_id" // campaign_id
        const val CAMPAIGN_ACTIVITY_ID = "campaign_activity_id" // campaign_activity_id
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ID = "$CAMPAIGN_PREFIX$CAMPAIGN_ID" // pinpoint.campaign.campaign_id
        // pinpoint.campaign.campaign_activity_id
        const val PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID = "$CAMPAIGN_PREFIX$CAMPAIGN_ACTIVITY_ID"
        const val DEFAULT_NOTIFICATION_CHANNEL_ID = "PINPOINT.NOTIFICATION" // PINPOINT.NOTIFICATION
        const val DIRECT_CAMPAIGN_SEND = "_DIRECT" // _DIRECT
    }
}
