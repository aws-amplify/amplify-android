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
package com.amplifyframework.eventenrichment.metadata

import android.os.Build
import java.util.Locale

private const val PLATFORM_ANDROID = "Android"

/**
 * Provides device metadata for event enrichment.
 *
 * Implement this to supply custom device information. The default
 * [AndroidDeviceMetadataProvider] resolves values from [android.os.Build] and
 * the default [java.util.Locale].
 */
fun interface DeviceMetadataProvider {
    /** Returns device metadata for the current device. */
    fun getDeviceMetadata(): DeviceMetadata
}

/**
 * Default [DeviceMetadataProvider] backed by [android.os.Build] and
 * [java.util.Locale].
 *
 * Resolves the platform name, OS version, manufacturer, model, and locale so
 * that events carry real device context. This is wired automatically by
 * [com.amplifyframework.eventenrichment.EventEnrichmentClient]; there is no
 * need to construct it directly outside of testing.
 */
class AndroidDeviceMetadataProvider : DeviceMetadataProvider {
    override fun getDeviceMetadata(): DeviceMetadata = DeviceMetadata(
        platform = PLATFORM_ANDROID,
        platformVersion = Build.VERSION.RELEASE,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        locale = Locale.getDefault().toString()
    )
}
