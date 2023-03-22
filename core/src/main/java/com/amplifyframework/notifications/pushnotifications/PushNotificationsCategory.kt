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

import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.Category

class PushNotificationsCategory : Category<PushNotificationsPlugin<*>>(), PushNotificationsCategoryBehavior {

    override fun getCategoryType() = selectedPlugin.categoryType

    override fun identifyUser(
        userId: String,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.identifyUser(userId, onSuccess, onError)

    override fun identifyUser(
        userId: String,
        profile: UserProfile,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.identifyUser(userId, profile, onSuccess, onError)

    override fun registerDevice(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.registerDevice(token, onSuccess, onError)

    override fun recordNotificationReceived(
        payload: NotificationPayload,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.recordNotificationReceived(payload, onSuccess, onError)

    override fun recordNotificationOpened(
        payload: NotificationPayload,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.recordNotificationOpened(payload, onSuccess, onError)

    override fun shouldHandleNotification(
        payload: NotificationPayload
    ) = selectedPlugin.shouldHandleNotification(payload)

    override fun handleNotificationReceived(
        payload: NotificationPayload,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.handleNotificationReceived(payload, onSuccess, onError)
}
