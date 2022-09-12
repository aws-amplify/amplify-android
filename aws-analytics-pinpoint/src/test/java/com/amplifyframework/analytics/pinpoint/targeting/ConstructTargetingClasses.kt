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

package com.amplifyframework.analytics.pinpoint.targeting

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.pinpoint.internal.core.idresolver.SharedPrefsUniqueIdService
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfile
import com.amplifyframework.analytics.pinpoint.targeting.notification.PinpointNotificationClient
import io.mockk.every
import io.mockk.mockk
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

internal val pinpointNotificationClient = PinpointNotificationClient()
internal val idService = mockk<SharedPrefsUniqueIdService>()
internal val appDetails = AndroidAppDetails(appID, appTitle, packageName, versionCode, versionName)
internal val deviceDetails = AndroidDeviceDetails(carrier = carrier, locale = locale)
internal val applicationContext = mockk<Context>()

internal fun setup() {
    every { idService.getUniqueId() }.returns(uniqueID)
    every { applicationContext.resources.configuration.locales[0].isO3Country }
        .returns(country)
}

internal fun constructEndpointProfile(): EndpointProfile {
    setup()
    val endpointProfile = EndpointProfile(
        pinpointNotificationClient,
        idService,
        appDetails,
        deviceDetails,
        applicationContext
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
        pinpointClient,
        pinpointNotificationClient,
        idService,
        prefs,
        appDetails,
        deviceDetails,
        applicationContext
    )
}
