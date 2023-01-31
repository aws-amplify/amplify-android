package com.amplifyframework.pushnotifications.pinpoint.utils.permissions

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Communication channel for relaying permission request results from the [PermissionsRequestActivity] to
 * the [PushNotificationPermission] utility class.
 */
internal object PermissionRequestChannel {
    private class IdAndResult(val requestId: String, val result: PermissionRequestResult)

    private val flow = MutableSharedFlow<IdAndResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Get a flow for the result of a particular permission request
     */
    fun listen(requestId: String) = flow.filter { it.requestId == requestId }.map { it.result }

    /**
     * Send the result of a permission request
     */
    fun send(requestId: String, result: PermissionRequestResult) = flow.tryEmit(IdAndResult(requestId, result))
}
