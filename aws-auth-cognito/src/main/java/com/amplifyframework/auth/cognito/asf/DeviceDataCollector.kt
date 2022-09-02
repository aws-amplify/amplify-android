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

package com.amplifyframework.auth.cognito.asf

import android.content.Context
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Collects information that identifies the device.
 */
class DeviceDataCollector : DataCollector {
    companion object {
        private const val PLATFORM_VALUE = "ANDROID"
        private const val LOCAL_STORAGE_PATH = "AWS.Cognito.ContextData"
        private const val LOCAL_STORAGE_DEVICE_ID_KEY = "CognitoDeviceId"

        /**
         * DeviceId that Cognito has associated with the device
         */
        const val DEVICE_AGENT = "DeviceId"

        /**
         * Third party device id provided on the device
         */
        const val THIRD_PARTY_DEVICE_AGENT = "ThirdPartyDeviceId"

        /**
         * Platform like Android, iOS or JS.
         */
        const val PLATFORM_KEY = "Platform"

        /**
         * Device time zone.
         */
        const val TIMEZONE = "ClientTimezone"

        /**
         * Device display dimensions.
         */
        const val DEVICE_HEIGHT = "ScreenHeightPixels"
        const val DEVICE_WIDTH = "ScreenWidthPixels"

        /**
         * Language on device.
         */
        const val DEVICE_LANGUAGE = "DeviceLanguage"
    }

    private val thirdPartyDeviceAgent = Settings.Secure.ANDROID_ID

    private val language = Locale.getDefault().toString()

    private val timezone: TimeZone = TimeZone.getDefault()

    private val timezoneOffset: String
        get() {
            val rawTimezoneOffset = timezone.rawOffset.toLong()
            val hours = TimeUnit.MILLISECONDS.toHours(rawTimezoneOffset)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(rawTimezoneOffset) - TimeUnit.HOURS.toMinutes(hours)
            return (if (hours < 0) "-" else "") + String.format(Locale.US, "%02d:%02d", abs(hours), minutes)
        }

    private fun getDisplay(context: Context): Display {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay
    }

    private fun getCognitoDeviceAgent(context: Context): String {
        // TODO: use credential store
        val sharedPreferences = context.getSharedPreferences(LOCAL_STORAGE_PATH, Context.MODE_PRIVATE)
        val storedId = sharedPreferences?.getString(LOCAL_STORAGE_DEVICE_ID_KEY, null)
        if (storedId != null) {
            return storedId
        }
        val deviceId = "${UUID.randomUUID()}:${Date().time}"
        val editor = sharedPreferences.edit()
        editor.putString(LOCAL_STORAGE_DEVICE_ID_KEY, deviceId)
        editor.apply()
        return deviceId
    }

    /**
     * {@inheritDoc}
     */
    override fun collect(context: Context): Map<String, String?> {
        val display = getDisplay(context)
        return mapOf(
            TIMEZONE to timezoneOffset,
            PLATFORM_KEY to PLATFORM_VALUE,
            THIRD_PARTY_DEVICE_AGENT to thirdPartyDeviceAgent,
            DEVICE_AGENT to getCognitoDeviceAgent(context),
            DEVICE_LANGUAGE to language,
            DEVICE_HEIGHT to display.height.toString(),
            DEVICE_WIDTH to display.width.toString(),
        )
    }
}
