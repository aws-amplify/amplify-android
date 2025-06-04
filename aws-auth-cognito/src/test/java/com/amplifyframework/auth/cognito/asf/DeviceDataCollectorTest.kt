/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.asf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.DEVICE_AGENT
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.DEVICE_HEIGHT
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.DEVICE_LANGUAGE
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.DEVICE_WIDTH
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.PLATFORM_KEY
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.THIRD_PARTY_DEVICE_AGENT
import com.amplifyframework.auth.cognito.asf.DeviceDataCollector.Companion.TIMEZONE
import io.kotest.matchers.maps.shouldContainExactly
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.SimpleTimeZone
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w75dp-h150dp")
class DeviceDataCollectorTest {

    @Before
    fun setup() {
        mockkStatic(TimeZone::class)
        every { TimeZone.getDefault() } returns SimpleTimeZone(0, "UTC")
    }

    @After
    fun teardown() {
        unmockkStatic(TimeZone::class)
    }

    @Test
    fun `returns expected values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val collector = DeviceDataCollector("deviceId")
        val data = collector.collect(context)

        data shouldContainExactly mapOf(
            TIMEZONE to "00:00",
            PLATFORM_KEY to "ANDROID",
            THIRD_PARTY_DEVICE_AGENT to "android_id",
            DEVICE_AGENT to "deviceId",
            DEVICE_LANGUAGE to "en_US",
            DEVICE_HEIGHT to "150",
            DEVICE_WIDTH to "75"
        )
    }
}
