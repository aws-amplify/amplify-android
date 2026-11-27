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

import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for [PushNotificationsUtils] display behaviour.
 */
@RunWith(RobolectricTestRunner::class)
class PushNotificationsUtilsTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val notificationManager = application.getSystemService(NotificationManager::class.java)

    // Unconfined dispatcher so the launched coroutine runs synchronously within the test.
    private val scope = TestScope()

    private fun utils() = PushNotificationsUtils(
        context = application,
        smallIconRes = android.R.drawable.ic_dialog_info,
        scope = scope
    )

    @Test
    fun `does not post a notification when notifications are disabled`() {
        shadowOf(notificationManager).setNotificationsEnabled(false)
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.TITLE to "Title",
                PushNotificationsConstants.BODY to "Body"
            )
        )!!

        utils().showNotification(1, payload, PushNotificationsUtilsTest::class.java)

        // The permission pre-check should short-circuit before anything is posted.
        shadowOf(notificationManager).size() shouldBe 0
    }

    @Test
    fun `posts a notification when notifications are enabled`() {
        shadowOf(notificationManager).setNotificationsEnabled(true)
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.TITLE to "Title",
                PushNotificationsConstants.BODY to "Body"
            )
        )!!

        utils().showNotification(2, payload, PushNotificationsUtilsTest::class.java)
        scope.testScheduler.advanceUntilIdle()

        shadowOf(notificationManager).size() shouldBe 1
    }
}
