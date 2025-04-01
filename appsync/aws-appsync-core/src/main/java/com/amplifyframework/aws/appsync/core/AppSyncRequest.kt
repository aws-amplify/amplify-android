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
package com.amplifyframework.aws.appsync.core

/**
 * Request Interface that is passed into an [AppSyncAuthorizer] to generate auth headers.
 * Mainly used for IAM signing, but could be used for custom user-provided authorizers as well
 */
interface AppSyncRequest {
    val method: HttpMethod
    val url: String
    val headers: Map<String, String>
    val body: String?

    enum class HttpMethod {
        GET, POST
    }
}

internal object HeaderKeys {
    const val AMAZON_DATE = "x-amz-date"
    const val API_KEY = "x-api-key"
    const val AUTHORIZATION = "Authorization"
    const val HOST = "host"
    const val ACCEPT = "accept"
    const val CONTENT_TYPE = "content-type"
    const val SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol"
}

internal object HeaderValues {
    const val ACCEPT_APPLICATION_JSON = "application/json, text/javascript"
    const val CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8"
    const val SEC_WEBSOCKET_PROTOCOL_APPSYNC_EVENTS = "aws-appsync-event-ws"
}
