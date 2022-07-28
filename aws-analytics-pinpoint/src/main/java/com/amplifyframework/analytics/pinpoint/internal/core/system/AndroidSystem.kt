/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.analytics.pinpoint.internal.core.system

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails

// UUID to identify a unique shared preferences and directory the library
// can use, will be concatenated with the appId to ensure no collision
private const val PREFERENCES_AND_FILE_MANAGER_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"

internal open class AndroidSystem {
    private val preferences: SharedPreferences
    private val appDetails: AndroidAppDetails
    private val deviceDetails: AndroidDeviceDetails

    constructor(preferences: SharedPreferences,
                appDetails: AndroidAppDetails,
                deviceDetails: AndroidDeviceDetails) {
        this.preferences = preferences;
        this.appDetails = appDetails;
        this.deviceDetails = deviceDetails;
    }

    constructor(context: Context, appId: String) {
        preferences = context.getSharedPreferences(
            appId + PREFERENCES_AND_FILE_MANAGER_SUFFIX, Context.MODE_PRIVATE
        )
        appDetails = AndroidAppDetails(context, appId)
        deviceDetails = AndroidDeviceDetails(getCarrier(context))
    }

    private fun getCarrier(context: Context): String {
        return try {
            val telephony = context
                .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephony.networkOperatorName.ifBlank {
                "Unknown"
            }
        } catch (ex: Exception) {
            "Unknown"
        }
    }

    fun getPreferences(): SharedPreferences {
        return preferences
    }

    fun getAppDetails(): AndroidAppDetails {
        return appDetails
    }

    fun getDeviceDetails(): AndroidDeviceDetails {
        return deviceDetails
    }
}