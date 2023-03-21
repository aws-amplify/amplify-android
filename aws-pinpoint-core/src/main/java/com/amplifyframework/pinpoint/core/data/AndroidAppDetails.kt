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

package com.amplifyframework.pinpoint.core.data

import android.content.Context
import com.amplifyframework.annotations.InternalAmplifyApi
import kotlinx.serialization.Serializable

@Serializable
@InternalAmplifyApi
class AndroidAppDetails {

    val appId: String
    val appTitle: String
    val packageName: String
    val versionCode: String
    val versionName: String?

    constructor(
        appId: String,
        appTitle: String,
        packageName: String,
        versionCode: String,
        versionName: String
    ) {
        this.appId = appId
        this.appTitle = appTitle
        this.packageName = packageName
        this.versionCode = versionCode
        this.versionName = versionName
    }

    constructor(context: Context, appId: String) {
        val applicationContext = context.applicationContext
        val packageManager = applicationContext.packageManager
        val packageInfo = packageManager.getPackageInfo(applicationContext.packageName, 0)
        val appInfo = packageManager.getApplicationInfo(applicationContext.packageName, 0)
        this.appId = appId
        this.appTitle = packageManager.getApplicationLabel(appInfo) as String
        this.packageName = packageInfo.packageName
        this.versionCode = packageInfo.versionCode.toString()
        this.versionName = packageInfo.versionName
    }
}
