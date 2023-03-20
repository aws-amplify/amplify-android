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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission

/**
 * Activity that is launched to request the post notification permission
 */
class PermissionsRequestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestId = intent.extras?.getString(PermissionRequestId)
        if (requestId != null) {
            launchPermissionRequest(requestId)
        } else {
            finishWithNoAnimation()
        }
    }

    private fun launchPermissionRequest(requestId: String) {
        val launcher = registerForActivityResult(RequestPermission()) { granted ->
            val result = if (granted) {
                PermissionRequestResult.Granted
            } else {
                PermissionRequestResult.NotGranted(shouldShowRequestPermissionRationale(PermissionName))
            }
            PermissionRequestChannel.send(requestId, result)
            finishWithNoAnimation()
        }

        launcher.launch(PermissionName)
    }

    private fun finishWithNoAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }
}
