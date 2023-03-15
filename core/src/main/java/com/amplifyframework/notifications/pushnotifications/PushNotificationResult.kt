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

/**
 * Result values of handling a Pinpoint push message.
 */
sealed class PushNotificationResult {
    /**
     * Amplify handled the message and posted a local notification.
     */
    object NotificationPosted : PushNotificationResult()

    /**
     * Amplify handled the message, but no notification was posted, since
     * the app was in the foreground.
     */
    object AppInForeground : PushNotificationResult()

    /**
     * Amplify handled the message, but no notification was posted, since
     * the app was opted out.
     */
    object OptedOut : PushNotificationResult()

    /**
     * The SDK handled the message that indicated the local campaign
     * notification was opened.
     */
    object Silent : PushNotificationResult()
}
