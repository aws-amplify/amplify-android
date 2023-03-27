/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.pushnotifications.pinpoint.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlinx.coroutines.flow.first

internal const val PermissionRequiredApiLevel = 33
internal const val PermissionName = "android.permission.POST_NOTIFICATIONS"
internal const val PermissionRequestId = "com.amplifyframework.permissions.requestId"

class PushNotificationPermission(private val context: Context) {

    val hasRequiredPermission: Boolean
        get() = Build.VERSION.SDK_INT < PermissionRequiredApiLevel ||
            ContextCompat.checkSelfPermission(context, PermissionName) == PackageManager.PERMISSION_GRANTED

    /**
     * Launches an Activity to request notification permissions and suspends until the user makes a selection or
     * dismisses the dialog. The behavior of this function depends on the device, current permission status, and
     * build configuration.
     *
     * 1. If the device API level is < 33 then this will immediately return [PermissionRequestResult.Granted] because
     *    no permission is required on this device.
     * 2. If the device API level is >= 33 but the application is targeting API level < 33 then this function will not
     *    show a permission dialog, but will return the current status of the notification permission. The permission
     *    request dialog will instead appear whenever the app tries to create a notification channel.
     * 3. Otherwise, the dialog will be shown or not as per normal runtime permission request rules
     * See https://developer.android.com/develop/ui/views/notifications/notification-permission for details
     */
    suspend fun requestPermission(): PermissionRequestResult {
        if (hasRequiredPermission) {
            return PermissionRequestResult.Granted
        }

        val requestId = UUID.randomUUID().toString()

        // Start the activity
        val intent = Intent(context, PermissionsRequestActivity::class.java).apply {
            putExtra(PermissionRequestId, requestId)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)

        // Listen for the result
        return PermissionRequestChannel.listen(requestId).first()
    }

    /**
     * Opens the application's settings page, where the user can manually grant/revoke application permissions
     */
    fun openSettings() {
        // Build a uri like "package:com.example.myapplication". See docs for ACTION_APPLICATION_DETAILS_SETTINGS.
        val uri = Uri.fromParts("package", context.packageName, null)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        context.startActivity(intent)
    }
}
