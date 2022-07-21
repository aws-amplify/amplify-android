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

package com.amplifyframework.analytics.pinpoint.models

import android.os.Build
import java.util.Locale

internal data class AndroidDeviceDetails(
    val carrier: String? = null,
    val platformVersion: String? = Build.VERSION.RELEASE,
    val platform: String = "ANDROID",
    val manufacturer: String? = Build.MANUFACTURER,
    val model: String? = Build.MODEL,
    val locale: Locale = Locale.getDefault()
)
