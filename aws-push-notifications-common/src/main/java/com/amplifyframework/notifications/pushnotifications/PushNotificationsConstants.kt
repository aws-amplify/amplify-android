/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Standard, backend-agnostic keys used to read push notification content out of an FCM data payload.
 *
 * These are the vendor-neutral keys that [PushNotificationPayload] parses. They intentionally do not
 * include any Pinpoint campaign/journey specific keys, so that a plain FCM push (sent by any backend
 * or directly by an app developer) is parsed and displayed.
 */
object PushNotificationsConstants {
    /** Notification title. */
    const val TITLE = "title"

    /** Notification body. Preferred key. */
    const val BODY = "body"

    /** Alternate notification body key, accepted for compatibility with senders that use "message". */
    const val MESSAGE = "message"

    /** Remote image URL rendered as the notification's large icon. */
    const val IMAGE_URL = "imageUrl"

    /** Action key indicating the app should be opened when the notification is tapped. */
    const val OPEN_APP = "openApp"

    /**
     * Action key holding a URL to open when the notification is tapped.
     *
     * The library enforces HTTPS: a value whose scheme is `http` is upgraded to `https` before it is
     * used. Values with any other scheme (including custom schemes) are left untouched. Senders that
     * need to reach an HTTP-only host should use [DEEPLINK] instead.
     */
    const val URL = "url"

    /** Action key holding a deep link URI to open when the notification is tapped. */
    const val DEEPLINK = "deeplink"

    /** When set to "1" or "true", the message is data-only and no notification should be displayed. */
    const val SILENT_PUSH = "silentPush"

    /** Optional per-message notification channel id. */
    const val CHANNEL_ID = "channelId"

    /**
     * Optional stable notification id. When present, senders can use it to de-duplicate: two
     * deliveries carrying the same id replace one another instead of stacking. See
     * [PushNotificationPayload.notificationId] for the fallback used when this key is absent.
     */
    const val NOTIFICATION_ID = "notificationId"

    /** Default notification channel id used when a payload does not specify one. */
    const val DEFAULT_NOTIFICATION_CHANNEL_ID = "amplify.notifications"
}
