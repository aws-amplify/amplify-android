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

package com.amplifyframework.notifications

import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException

interface NotificationsCategoryBehavior {
    fun identifyUser(
        userId: String,
        profile: UserProfile,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    )

    fun identifyUser(
        userId: String,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    )
}
