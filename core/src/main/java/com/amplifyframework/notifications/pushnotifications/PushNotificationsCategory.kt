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

import android.os.Bundle
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.Category

class PushNotificationsCategory : Category<PushNotificationsPlugin<*>>(), PushNotificationsCategoryBehavior {

    override fun getCategoryType() = selectedPlugin.categoryType

    override fun identifyUser(userId: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.identifyUser(userId, onSuccess, onError)

    override fun registerDevice(token: String, onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.registerDevice(token, onSuccess, onError)

    override fun recordNotificationReceived(
        data: Map<String, String>,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.recordNotificationReceived(data, onSuccess, onError)

    override fun recordNotificationOpened(
        data: Map<String, String>,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.recordNotificationOpened(data, onSuccess, onError)

    override fun handleNotificationReceived(
        details: Bundle,
        onSuccess: Consumer<PushNotificationResult>,
        onError: Consumer<PushNotificationsException>
    ) = selectedPlugin.handleNotificationReceived(details, onSuccess, onError)

    override fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>) =
        selectedPlugin.getInitialNotification(onSuccess, onError)

    override fun getToken() = selectedPlugin.getToken()

    override fun getBadgeCount() = selectedPlugin.getBadgeCount()

    override fun setBadgeCount(count: Int) = selectedPlugin.setBadgeCount(count)
}