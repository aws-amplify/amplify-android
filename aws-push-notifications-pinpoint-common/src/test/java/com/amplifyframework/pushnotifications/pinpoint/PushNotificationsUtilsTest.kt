/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.pushnotifications.pinpoint

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.pushnotifications.pinpoint.permissions.PERMISSION_NAME
import com.amplifyframework.pushnotifications.pinpoint.permissions.PushNotificationPermission
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PushNotificationsUtilsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val robolectricSdk = Build.VERSION.SDK_INT

    @After
    fun restoreSdk() {
        setSdk(robolectricSdk)
    }

    @Test
    fun `does not require notification permission below API 33`() {
        setSdk(Build.VERSION_CODES.S_V2)
        shadowOf(context).denyPermissions(PERMISSION_NAME)

        PushNotificationPermission(context).hasRequiredPermission shouldBe true
    }

    @Test
    fun `rejects missing notification permission on API 33`() {
        setSdk(Build.VERSION_CODES.TIRAMISU)
        shadowOf(context).denyPermissions(PERMISSION_NAME)

        PushNotificationPermission(context).hasRequiredPermission shouldBe false
    }

    @Test
    fun `accepts granted notification permission on API 33`() {
        setSdk(Build.VERSION_CODES.TIRAMISU)
        shadowOf(context).grantPermissions(PERMISSION_NAME)

        PushNotificationPermission(context).hasRequiredPermission shouldBe true
    }

    private fun setSdk(sdk: Int) = ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
}
