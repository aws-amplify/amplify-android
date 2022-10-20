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
import android.os.Build

/**
 * Collects build information for underlying device hardware.
 */
class BuildDataCollector : DataCollector {
    internal companion object {
        /**
         * The consumer-visible brand with which the product/hardware will be associated
         */
        const val BRAND = "DeviceBrand"

        /**
         * A string that uniquely identifies this build on device.
         */
        const val FINGERPRINT = "DeviceFingerprint"

        /**
         * Name of the underlying device hardware.
         */
        const val HARDWARE = "DeviceHardware"

        /**
         * Boot-loader version of the system
         */
        const val BOOTLOADER = "Bootloader"

        /**
         * The end-user-visible name for the end product.
         */
        const val MODEL = "DeviceName"

        /**
         * The name of the overall product.
         */
        const val PRODUCT = "Product"

        /**
         * The manufacturer of the product/hardware.
         */
        const val MANUFACTURER = "DeviceManufacturer"

        /**
         * The type of build, like "user" or "eng".
         */
        const val BUILD_TYPE = "BuildType"

        /**
         * The user-visible version string for Android release.
         */
        const val VERSION_RELEASE = "DeviceOsReleaseVersion"

        /**
         * The user-visible SDK version of the framework
         */
        const val VERSION_SDK = "DeviceSdkVersion"
    }

    /**
     * {@inheritDoc}
     */
    override fun collect(context: Context): Map<String, String?> = mapOf(
        BRAND to Build.BRAND,
        FINGERPRINT to Build.FINGERPRINT,
        HARDWARE to Build.HARDWARE,
        MODEL to Build.MODEL,
        PRODUCT to Build.PRODUCT,
        BUILD_TYPE to Build.TYPE,
        VERSION_RELEASE to Build.VERSION.RELEASE,
        VERSION_SDK to Build.VERSION.SDK
    )
}
