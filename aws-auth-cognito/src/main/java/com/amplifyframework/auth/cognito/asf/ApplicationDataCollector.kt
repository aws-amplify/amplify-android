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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Collects application related data for the device.
 */
class ApplicationDataCollector : DataCollector {
    companion object {
        private val TAG = ApplicationDataCollector::class.java.simpleName
        private const val ALL_FLAGS_OFF = 0

        /**
         * Name of the application using user pools.
         */
        const val APP_NAME = "ApplicationName"

        /**
         * Target SDK version for the application.
         */
        const val APP_TARGET_SDK = "ApplicationTargetSdk"

        /**
         * Version of the application installed on device.
         */
        const val APP_VERSION = "ApplicationVersion"
    }

    private fun getAppName(context: Context) =
        context.packageManager.getApplicationLabel(context.applicationInfo).toString()

    @SuppressLint("WrongConstant")
    private fun getAppVersion(context: Context) = try {
        context.packageManager.getPackageInfo(context.packageName, ALL_FLAGS_OFF).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Log.i(TAG, "Unable to get app version. Provided package name could not be found.")
        ""
    }

    private fun getAppTargetSdk(context: Context) = context.applicationInfo.targetSdkVersion.toString()

    /**
     * {@inheritDoc}
     */
    override fun collect(context: Context): Map<String, String?> = mapOf(
        APP_NAME to getAppName(context),
        APP_TARGET_SDK to getAppTargetSdk(context),
        APP_VERSION to getAppVersion(context)
    )
}
