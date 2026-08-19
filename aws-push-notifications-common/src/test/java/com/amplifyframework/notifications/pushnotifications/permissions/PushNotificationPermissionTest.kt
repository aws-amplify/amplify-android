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

package com.amplifyframework.notifications.pushnotifications.permissions

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for [PushNotificationPermission].
 */
@RunWith(RobolectricTestRunner::class)
class PushNotificationPermissionTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val permission = PushNotificationPermission(application)

    @Test
    fun `openSettings launches app details settings with NEW_TASK flag`() {
        permission.openSettings()

        val started = shadowOf(application).nextStartedActivity
        started.action shouldBe Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ((started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0).shouldBeTrue()
    }
}
