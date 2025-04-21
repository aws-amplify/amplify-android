/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.events.data.ConnectionClosedException
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.UserClosedConnectionException

internal sealed class WebSocketDisconnectReason(val throwable: Throwable?) {
    data object UserInitiated : WebSocketDisconnectReason(null)
    data object Timeout : WebSocketDisconnectReason(EventsException("Connection timed out."))
    class Service(throwable: Throwable? = null) : WebSocketDisconnectReason(throwable)

    internal fun toCloseException(): EventsException {
        return when (this) {
            is UserInitiated -> UserClosedConnectionException()
            else -> ConnectionClosedException(throwable)
        }
    }
}
