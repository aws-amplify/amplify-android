package com.amplifyframework.pushnotifications.pinpoint.utils.permissions

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
