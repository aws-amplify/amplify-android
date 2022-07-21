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

import android.content.Context

internal class AndroidAppDetails {

    private val appId: String
    private val appTitle: String
    private val packageName: String
    private val versionCode: String
    private val versionName: String

    constructor(
        appId: String,
        appTitle: String,
        packageName: String,
        versionCode: String,
        versionName: String
    ) {
        this@AndroidAppDetails.appId = appId
        this@AndroidAppDetails.appTitle = appTitle
        this@AndroidAppDetails.packageName = packageName
        this@AndroidAppDetails.versionCode = versionCode
        this@AndroidAppDetails.versionName = versionName
    }

    constructor(context: Context, appId: String) {
        val applicationContext = context.applicationContext
        val packageManager = applicationContext.packageManager
        val packageInfo = packageManager.getPackageInfo(applicationContext.packageName, 0)
        val appInfo = packageManager.getApplicationInfo(applicationContext.packageName, 0)
        this@AndroidAppDetails.appId = appId
        this@AndroidAppDetails.appTitle = packageManager.getApplicationLabel(appInfo) as String
        this@AndroidAppDetails.packageName = packageInfo.packageName
        this@AndroidAppDetails.versionCode = packageInfo.versionCode.toString()
        this@AndroidAppDetails.versionName = packageInfo.versionName
    }
}
