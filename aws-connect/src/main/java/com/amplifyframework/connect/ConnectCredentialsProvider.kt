/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

import com.amplifyframework.foundation.credentials.AwsCredentials

/**
 * The resolved auth material used to authorize an identify request.
 *
 * Exactly one path applies:
 *  - Authenticated: [accessToken] is non-null. The auth route is used with
 *    `Authorization: Bearer <accessToken>`.
 *  - Guest: [accessToken] is null but [credentials] are present. The guest
 *    route is used, SigV4-signed (`execute-api`) with those credentials.
 *
 * @param accessToken The Cognito user-pool access token, or null for guest
 * @param credentials The Identity Pool AWS credentials for signing the guest route
 * @param identityId The Identity Pool identityId, if available
 */
data class ConnectSession(
    val accessToken: String? = null,
    val credentials: AwsCredentials? = null,
    val identityId: String? = null
) {
    /** Whether an authenticated access token is present. */
    val isAuthenticated: Boolean get() = accessToken != null
}

/**
 * Resolves a [ConnectSession] for the current caller.
 *
 * The Amplify Auth integration layer implements this against
 * `Amplify.Auth.fetchAuthSession()` to extract the access token and/or
 * guest credentials.
 */
fun interface ConnectCredentialsProvider {
    /** Resolves the auth material for the current caller. */
    suspend fun fetchSession(): ConnectSession
}
