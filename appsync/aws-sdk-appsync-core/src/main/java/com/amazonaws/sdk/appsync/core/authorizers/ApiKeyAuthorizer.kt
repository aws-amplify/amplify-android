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
package com.amazonaws.sdk.appsync.core.authorizers

import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.core.HeaderKeys
import com.amazonaws.sdk.appsync.core.util.Iso8601Timestamp

/**
 * [AppSyncAuthorizer] implementation that authorizes requests via API Key.
 * @param fetchApiKey Delegate that provides the API Key to use. This provider will be invoked on every request, so
 * it should implement a reasonable caching mechanism if necessary.
 */
class ApiKeyAuthorizer(private val fetchApiKey: suspend () -> String) : AppSyncAuthorizer {

    /**
     * Provide a static API Key
     * @param apiKey The API Key
     */
    constructor(apiKey: String) : this({ apiKey })

    override suspend fun getAuthorizationHeaders(request: AppSyncRequest) = mapOf(
        HeaderKeys.AMAZON_DATE to Iso8601Timestamp.now(),
        HeaderKeys.API_KEY to fetchApiKey()
    )
}
