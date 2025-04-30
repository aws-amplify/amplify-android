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

/**
 * [AppSyncAuthorizer] implementation for using auth tokens for authorization with AppSync. You can use this class to
 * authorize requests with Cognito User Pools, OIDC, or Lambda-based custom authorization.
 * @param fetchLatestAuthToken Delegate to return a valid auth token. This delegate will be invoked on every request, so
 * it should implement a reasonable caching mechanism if necessary.
 */
class AuthTokenAuthorizer(private val fetchLatestAuthToken: suspend () -> String) :
    AppSyncAuthorizer {

    override suspend fun getAuthorizationHeaders(request: AppSyncRequest) = mapOf(
        HeaderKeys.AUTHORIZATION to fetchLatestAuthToken()
    )
}
