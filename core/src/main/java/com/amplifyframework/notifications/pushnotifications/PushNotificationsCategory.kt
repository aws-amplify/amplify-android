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

package com.amplifyframework.notifications.pushnotifications

import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.Category
import com.amplifyframework.notifications.NotificationsCategoryBehavior

typealias NotificationReceivedListener = (PushNotificationsDetails) -> Unit

class PushNotificationsCategory :
    Category<PushNotificationsPlugin<*>>(),
    PushNotificationsCategoryBehavior,
    NotificationsCategoryBehavior {

    override fun getCategoryType() = selectedPlugin.categoryType

    override fun identifyUser(userId: String) = selectedPlugin.identifyUser(userId)

    override fun registerDevice(token: String) = selectedPlugin.registerDevice(token)

    override fun onForegroundNotificationReceived(listener: NotificationReceivedListener) =
        selectedPlugin.onForegroundNotificationReceived(listener)

    override fun onBackgroundNotificationReceived(listener: NotificationReceivedListener) =
        selectedPlugin.onBackgroundNotificationReceived(listener)

    override fun onNotificationOpened(onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.onNotificationOpened(onSuccess, onError)

    override fun handleNotificationReceived(details: PushNotificationsDetails): PushNotificationResult =
        selectedPlugin.handleNotificationReceived(details)

    override fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.getInitialNotification(onSuccess, onError)

    override fun getToken() = selectedPlugin.getToken()

    override fun getBadgeCount() = selectedPlugin.getBadgeCount()

    override fun setBadgeCount(count: Int) = selectedPlugin.setBadgeCount(count)
}
