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

package com.amplifyframework.kotlin.notifications

import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.notifications.NotificationsCategoryBehavior
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class KotlinNotificationsFacadeTest {
    private val delegate = mockk<NotificationsCategoryBehavior>()
    private val notifications = KotlinNotificationsFacade(delegate)

    @Test
    fun identifyUserSucceeds() = runBlocking {
        val userId = "userId"

        every {
            delegate.identifyUser(eq(userId), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        notifications.identifyUser(userId)
        verify {
            delegate.identifyUser(eq(userId), any(), any())
        }
    }

    @Test
    fun identifyUserWithProfileSucceeds() = runBlocking {
        val userId = "userId"
        val profile = UserProfile.builder().name("test").build()

        every {
            delegate.identifyUser(eq(userId), eq(profile), any(), any())
        } answers {
            val indexOfCompletionAction = 2
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        notifications.identifyUser(userId, profile)
        verify {
            delegate.identifyUser(eq(userId), eq(profile), any(), any())
        }
    }

    @Test(expected = PushNotificationsException::class)
    fun identifyUserThrows() = runBlocking {
        val userId = "userId"

        val error = PushNotificationsException("uh", "oh")
        every {
            delegate.identifyUser(eq(userId), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        notifications.identifyUser(userId)
    }

    @Test(expected = PushNotificationsException::class)
    fun identifyUserWithProfileThrows() = runBlocking {
        val userId = "userId"
        val profile = UserProfile.builder().name("test").build()

        val error = PushNotificationsException("uh", "oh")
        every {
            delegate.identifyUser(eq(userId), eq(profile), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        notifications.identifyUser(userId, profile)
    }
}
