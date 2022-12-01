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

interface PushNotificationsCategoryBehavior {

    fun onForegroundNotificationReceived(listener: NotificationReceivedListener)

    fun onBackgroundNotificationReceived(listener: NotificationReceivedListener)

    fun onNotificationOpened(onSuccess: Action, onError: Consumer<PushNotificationsException>)

    fun handleNotificationReceived(details: PushNotificationsDetails): PushNotificationResult

    fun registerDevice(token: String)

    fun getInitialNotification(onSuccess: Action, onError: Consumer<PushNotificationsException>)

    fun getToken(): String?

    fun getBadgeCount(): Int

    fun setBadgeCount(count: Int)
}
