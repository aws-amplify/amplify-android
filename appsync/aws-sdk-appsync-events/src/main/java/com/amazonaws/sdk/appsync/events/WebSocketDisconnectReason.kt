/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events

import com.amazonaws.sdk.appsync.events.WebSocketDisconnectReason.UserInitiated
import com.amazonaws.sdk.appsync.events.data.ConnectionClosedException
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.UserClosedConnectionException

internal sealed class WebSocketDisconnectReason(val throwable: Throwable?) {
    data object UserInitiated : WebSocketDisconnectReason(null)
    data object Timeout : WebSocketDisconnectReason(EventsException("Connection timed out."))
    class Service(throwable: Throwable? = null) : WebSocketDisconnectReason(throwable) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Service) return false
            return true
        }

        override fun hashCode() = javaClass.hashCode()
    }
}

internal fun WebSocketDisconnectReason.toCloseException(): EventsException = when (this) {
    is UserInitiated -> UserClosedConnectionException()
    else -> ConnectionClosedException(throwable)
}
