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
