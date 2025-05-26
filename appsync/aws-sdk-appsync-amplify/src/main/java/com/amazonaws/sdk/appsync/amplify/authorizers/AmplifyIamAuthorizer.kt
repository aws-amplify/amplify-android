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
package com.amazonaws.sdk.appsync.amplify.authorizers

import com.amazonaws.sdk.appsync.amplify.util.AppSyncRequestSigner
import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.core.authorizers.IamAuthorizer
import org.jetbrains.annotations.VisibleForTesting

/**
 * Authorizer implementation that provides IAM signing through Amplify & AWS Kotlin SDK (Smithy).
 */
class AmplifyIamAuthorizer @VisibleForTesting internal constructor(
    private val region: String,
    private val requestSigner: AppSyncRequestSigner
) : AppSyncAuthorizer {

    constructor(region: String) : this(region, requestSigner = AppSyncRequestSigner())

    private val iamAuthorizer = IamAuthorizer { requestSigner.signAppSyncRequest(it, region) }

    override suspend fun getAuthorizationHeaders(request: AppSyncRequest) =
        iamAuthorizer.getAuthorizationHeaders(request)
}
