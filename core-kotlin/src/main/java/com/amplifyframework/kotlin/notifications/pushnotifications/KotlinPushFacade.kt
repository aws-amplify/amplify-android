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

package com.amplifyframework.kotlin.notifications.pushnotifications

import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.core.Amplify
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategoryBehavior
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KotlinPushFacade(private val delegate: PushNotificationsCategoryBehavior = Amplify.Notifications.Push) : Push {
    override suspend fun identifyUser(userId: String) = suspendCoroutine { continuation ->
        delegate.identifyUser(
            userId,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun identifyUser(userId: String, profile: UserProfile) = suspendCoroutine { continuation ->
        delegate.identifyUser(
            userId,
            profile,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun registerDevice(token: String) = suspendCoroutine { continuation ->
        delegate.registerDevice(
            token,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun recordNotificationReceived(payload: NotificationPayload) = suspendCoroutine { continuation ->
        delegate.recordNotificationReceived(
            payload,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun recordNotificationOpened(payload: NotificationPayload) = suspendCoroutine { continuation ->
        delegate.recordNotificationOpened(
            payload,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override fun shouldHandleNotification(payload: NotificationPayload) = delegate.shouldHandleNotification(payload)

    override suspend fun handleNotificationReceived(payload: NotificationPayload) = suspendCoroutine { continuation ->
        delegate.handleNotificationReceived(
            payload,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }
}
