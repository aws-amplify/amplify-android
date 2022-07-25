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
import android.content.pm.PackageManager
import com.amplifyframework.core.Amplify

class AndroidAppDetails {
    private var applicationContext: Context? = null
    var appTitle: String? = null
        private set
    private var packageName: String? = null
    private var versionCode: String? = null
    private var versionName: String? = null
    var appId: String? = null
        private set

    constructor() {}
    constructor(context: Context, appId: String?) {
        applicationContext = context.applicationContext
        try {
            val packageManager = applicationContext!!
                .packageManager
            val packageInfo = packageManager
                .getPackageInfo(applicationContext!!.packageName, 0)
            val appInfo = packageManager
                .getApplicationInfo(packageInfo.packageName, 0)
            appTitle = packageManager.getApplicationLabel(appInfo) as String
            packageName = packageInfo.packageName
            versionCode = packageInfo.versionCode.toString()
            versionName = packageInfo.versionName
            this.appId = appId
        } catch (e: PackageManager.NameNotFoundException) {
            LOG.warn(
                "Unable to get details for package " +
                        applicationContext!!.getPackageName()
            )
            appTitle = "Unknown"
            packageName = "Unknown"
            versionCode = "Unknown"
            versionName = "Unknown"
        }
    }

    constructor(
        packageName: String?,
        versionCode: String?,
        versionName: String?,
        appTitle: String?,
        appId: String?
    ) {
        this.packageName = packageName
        this.versionCode = versionCode
        this.versionName = versionName
        this.appTitle = appTitle
        this.appId = appId
    }

    fun packageName(): String? {
        return packageName
    }

    fun versionName(): String? {
        return versionName
    }

    fun versionCode(): String? {
        return versionCode
    }

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
    }
}
