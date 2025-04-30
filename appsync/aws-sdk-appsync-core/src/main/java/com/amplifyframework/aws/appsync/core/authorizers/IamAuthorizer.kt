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
package com.amplifyframework.aws.appsync.core.authorizers

import com.amplifyframework.aws.appsync.core.AppSyncAuthorizer
import com.amplifyframework.aws.appsync.core.AppSyncRequest

/**
 * [AppSyncAuthorizer] implementation that uses IAM policies for authorization. This authorizer delegates to the
 * [generateSignatureHeaders] function to generate a signature to add to each request.
 * The signature generation should use AWS SigV4 signing. There is an implementation of this signing logic in the
 * apollo-appsync-amplify library, or you can use the AWS SDK for Kotlin, or any other SigV4 implementation.
 * @param generateSignatureHeaders The delegate that performs the signing. It should return all of the SigV4 headers
 * necessary for a signed request.
 */
class IamAuthorizer(private val generateSignatureHeaders: suspend (AppSyncRequest) -> Map<String, String>) :
    AppSyncAuthorizer {

    override suspend fun getAuthorizationHeaders(request: AppSyncRequest) = generateSignatureHeaders(request)
}
