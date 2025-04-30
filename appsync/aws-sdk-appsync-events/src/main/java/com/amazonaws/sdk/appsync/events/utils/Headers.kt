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

package com.amazonaws.sdk.appsync.events.utils

import com.amazonaws.sdk.appsync.events.BuildConfig

internal object HeaderKeys {
    const val HOST = "host"
    const val ACCEPT = "accept"
    const val CONTENT_TYPE = "content-type"
    const val SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol"
    const val USER_AGENT = "User-Agent"
    const val X_AMZ_USER_AGENT = "x-amz-user-agent"
}

internal object HeaderValues {
    const val ACCEPT_APPLICATION_JSON = "application/json, text/javascript"
    const val CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8"
    const val SEC_WEBSOCKET_PROTOCOL_APPSYNC_EVENTS = "aws-appsync-event-ws"
    const val USER_AGENT = "aws-appsync-events-android#${BuildConfig.VERSION_NAME}"
}
