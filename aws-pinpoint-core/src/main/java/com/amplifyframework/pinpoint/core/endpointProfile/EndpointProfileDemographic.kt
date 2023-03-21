/*
 *  Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.pinpoint.core.endpointProfile

import android.os.Build
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import java.util.TimeZone
import kotlinx.serialization.Serializable

@Serializable
@InternalAmplifyApi
class EndpointProfileDemographic internal constructor(val appVersion: String?, val make: String, val locale: String) {
    internal constructor(
        appDetails: AndroidAppDetails,
        deviceDetails: AndroidDeviceDetails,
        locale: String
    ) : this(appDetails.versionName, deviceDetails.manufacturer, locale)

    val model: String = Build.MODEL ?: "TEST MODEL"
    val timezone: String = TimeZone.getDefault().id
    val platform = "ANDROID"
    val platformVersion: String = Build.VERSION.RELEASE ?: "TEST VERSION"
}
