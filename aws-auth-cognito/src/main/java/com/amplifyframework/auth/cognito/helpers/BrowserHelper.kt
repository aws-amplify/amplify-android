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

package com.amplifyframework.auth.cognito.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import androidx.core.net.toUri

internal object BrowserHelper {

    /***
     * Check if a browser is installed on the device to launch HostedUI.
     * @return true if a browser exists else false.
     */
    fun isBrowserInstalled(context: Context): Boolean {
        val url = "https://docs.amplify.aws/"
        val webAddress = url.toUri()
        val intentWeb = Intent(Intent.ACTION_VIEW, webAddress)
        return intentWeb.resolveActivity(context.packageManager) != null
    }

    /***
     * Check if there are any browsers on the device that support custom tabs.
     * @return preferred custom tabs package if custom tabs are supported on the device
     */
    fun getDefaultCustomTabPackage(context: Context): String? {
        val supportedPackages = getSupportedCustomTabsPackages(context)
        return if (supportedPackages.isNotEmpty()) {
            CustomTabsClient.getPackageName(context, supportedPackages)
        } else {
            null
        }
    }

    /**
     * Get list of packages that support Custom Tabs Service.
     * @return list of package names that support Custom Tabs.
     */
    private fun getSupportedCustomTabsPackages(context: Context): List<String> {
        val packageManager: PackageManager = context.packageManager
        val serviceIntent = Intent().setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)

        // Get all services that can handle ACTION_CUSTOM_TABS_CONNECTION intents.
        return packageManager.queryIntentServices(serviceIntent, 0).map { it.serviceInfo.packageName }
    }
}
