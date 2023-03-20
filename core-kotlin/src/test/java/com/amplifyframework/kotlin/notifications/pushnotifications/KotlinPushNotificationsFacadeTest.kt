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
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.kotlin.notifications.KotlinNotificationsFacade
import com.amplifyframework.notifications.NotificationsCategoryBehavior
import com.amplifyframework.notifications.pushnotifications.NotificationContentProvider
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategoryBehavior
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class KotlinPushNotificationsFacadeTest {
    private val pushDelegate = mockk<PushNotificationsCategoryBehavior>()
    private val push = KotlinPushFacade(pushDelegate)

    private val payload = NotificationPayload(NotificationContentProvider.FCM(mapOf()))

    @Test
    fun identifyUserCategoryLevelSucceeds() = runBlocking {
        val notificationsDelegate = mockk<NotificationsCategoryBehavior>()
        val notifications = KotlinNotificationsFacade(notificationsDelegate)

        val userId = "userId"
        coEvery {
            pushDelegate.identifyUser(eq(userId), any(), any())
        } coAnswers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }

        coEvery {
            notificationsDelegate.identifyUser(eq(userId), any(), any())
        } coAnswers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            pushDelegate.identifyUser(userId, onComplete, { })
        }

        notifications.identifyUser(userId)
        coVerify {
            pushDelegate.identifyUser(eq(userId), any(), any())
        }
    }

    @Test
    fun identifyUserWithProfileCategoryLevelSucceeds() = runBlocking {
        val notificationsDelegate = mockk<NotificationsCategoryBehavior>()
        val notifications = KotlinNotificationsFacade(notificationsDelegate)

        val userId = "userId"
        val profile = UserProfile.builder().name("test").build()
        coEvery {
            pushDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        } coAnswers {
            val indexOfCompletionAction = 2
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }

        coEvery {
            notificationsDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        } coAnswers {
            val indexOfCompletionAction = 2
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            pushDelegate.identifyUser(userId, profile, onComplete, { })
        }

        notifications.identifyUser(userId, profile)
        coVerify {
            pushDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        }
    }

    @Test
    fun identifyUserSucceeds() = runBlocking {
        val userId = "userId"

        every {
            pushDelegate.identifyUser(eq(userId), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        push.identifyUser(userId)
        verify {
            pushDelegate.identifyUser(eq(userId), any(), any())
        }
    }

    @Test
    fun identifyUserWithProfileSucceeds() = runBlocking {
        val userId = "userId"
        val profile = UserProfile.builder().name("test").build()

        every {
            pushDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        } answers {
            val indexOfCompletionAction = 2
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        push.identifyUser(userId, profile)
        verify {
            pushDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        }
    }

    @Test(expected = PushNotificationsException::class)
    fun identifyUserThrows() = runBlocking {
        val userId = "userId"

        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.identifyUser(eq(userId), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.identifyUser(userId)
    }

    @Test(expected = PushNotificationsException::class)
    fun identifyUserWithProfileThrows() = runBlocking {
        val userId = "userId"
        val profile = UserProfile.builder().name("test").build()

        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.identifyUser(eq(userId), eq(profile), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.identifyUser(userId, profile)
    }

    @Test
    fun registerDeviceSucceeds() = runBlocking {
        val token = "token"
        every {
            pushDelegate.registerDevice(eq(token), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        push.registerDevice(token)
        verify {
            pushDelegate.registerDevice(eq(token), any(), any())
        }
    }

    @Test(expected = PushNotificationsException::class)
    fun registerDeviceThrows() = runBlocking {
        val token = "token"
        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.registerDevice(eq(token), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.registerDevice(token)
    }

    @Test
    fun recordNotificationReceivedSucceeds() = runBlocking {
        every {
            pushDelegate.recordNotificationReceived(eq(payload), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        push.recordNotificationReceived(payload)
        verify {
            pushDelegate.recordNotificationReceived(eq(payload), any(), any())
        }
    }

    @Test(expected = PushNotificationsException::class)
    fun recordNotificationReceivedThrows() = runBlocking {
        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.recordNotificationReceived(eq(payload), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.recordNotificationReceived(payload)
    }

    @Test
    fun recordNotificationOpenedSucceeds() = runBlocking {
        every {
            pushDelegate.recordNotificationOpened(eq(payload), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        push.recordNotificationOpened(payload)
        verify {
            pushDelegate.recordNotificationOpened(eq(payload), any(), any())
        }
    }

    @Test(expected = PushNotificationsException::class)
    fun recordNotificationOpenedThrows() = runBlocking {
        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.recordNotificationOpened(eq(payload), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.recordNotificationOpened(payload)
    }

    @Test
    fun shouldHandleNotificationReturns() {
        every {
            pushDelegate.shouldHandleNotification(eq(payload))
        } returns(true)

        push.shouldHandleNotification(payload)
        verify {
            pushDelegate.shouldHandleNotification(eq(payload))
        }
    }

    @Test
    fun handleNotificationReceivedSucceeds() = runBlocking {
        val result = PushNotificationResult.NotificationPosted
        every {
            pushDelegate.handleNotificationReceived(eq(payload), any(), any())
        } answers {
            val indexOfResultConsumer = 1
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<PushNotificationResult>
            onResult.accept(result)
        }

        assert(result == push.handleNotificationReceived(payload))
    }

    @Test(expected = PushNotificationsException::class)
    fun handleNotificationReceivedThrows(): Unit = runBlocking {
        val error = PushNotificationsException("uh", "oh")
        every {
            pushDelegate.handleNotificationReceived(eq(payload), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<PushNotificationsException>
            onError.accept(error)
        }
        push.handleNotificationReceived(payload)
    }
}
