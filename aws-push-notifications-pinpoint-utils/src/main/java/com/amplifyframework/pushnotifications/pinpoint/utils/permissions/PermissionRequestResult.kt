package com.amplifyframework.pushnotifications.pinpoint.utils.permissions

sealed interface PermissionRequestResult {
    object Granted : PermissionRequestResult
    data class NotGranted(val shouldShowRationale: Boolean) : PermissionRequestResult
}
