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

package com.amplifyframework.pinpoint.core

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfile
import com.amplifyframework.pinpoint.core.util.getUniqueId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale

internal fun constructSharedPreferences(): SharedPreferences {
    return ApplicationProvider.getApplicationContext<Context>().getSharedPreferences(
        "preferences",
        Context.MODE_PRIVATE
    )
}

internal val uniqueID = "unique-id"
internal val appID = "app id"
internal val appTitle = "app title"
internal val packageName = "package name"
internal val versionCode = "1"
internal val versionName = "1.0.0"
internal val carrier = "carrier"
internal val locale = Locale.US
internal val country = "en_US"
internal val effectiveDate = 0L

internal val preferences = mockk<SharedPreferences>()
internal val store = mockk<KeyValueRepository>()
internal val appDetails = AndroidAppDetails(appID, appTitle, packageName, versionCode, versionName)
internal val deviceDetails = AndroidDeviceDetails(carrier = carrier, locale = locale)
internal val applicationContext = mockk<Context>()

internal fun setup() {
    mockkStatic("com.amplifyframework.pinpoint.core.util.SharedPreferencesUtilKt")
    every { preferences.getUniqueId() }.returns(uniqueID)
    every { store.get(TargetingClient.AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY) } returns ""
    every { applicationContext.resources.configuration.locales[0].isO3Country }
        .returns(country)
}

internal fun constructEndpointProfile(): EndpointProfile {
    setup()
    val endpointProfile = EndpointProfile(
        preferences.getUniqueId(),
        appDetails,
        deviceDetails,
        applicationContext,
        store
    )
    endpointProfile.effectiveDate = effectiveDate
    return endpointProfile
}

internal lateinit var pinpointClient: PinpointClient
internal fun constructPinpointClient(): PinpointClient {
    pinpointClient = mockk()
    return pinpointClient
}

internal fun constructTargetingClient(): TargetingClient {
    setup()
    val prefs = constructSharedPreferences()
    return TargetingClient(
        applicationContext,
        pinpointClient,
        store,
        prefs,
        appDetails,
        deviceDetails,
    )
}
