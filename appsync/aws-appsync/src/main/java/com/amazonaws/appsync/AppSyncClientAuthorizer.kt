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
package com.amazonaws.appsync

import com.amplifyframework.foundation.credentials.AwsCredentialsProvider

/**
 * Sealed hierarchy of authorizer configurations for the AppSync GraphQL client.
 * Each subtype encodes its [AppSyncAuthMode] and holds the provider needed to
 * produce authorization credentials for that mode.
 *
 * These are configuration objects — the actual header generation and request signing
 * is handled internally by the plugin infrastructure via [AuthProviderBridge].
 */
sealed class AppSyncClientAuthorizer(
    /** The auth mode this authorizer provides. */
    val authMode: AppSyncAuthMode
) {
    /**
     * API Key authorization.
     * @param fetchApiKey Suspend function that provides the API key.
     */
    class ApiKey(
        internal val fetchApiKey: suspend () -> String
    ) : AppSyncClientAuthorizer(AppSyncAuthMode.API_KEY) {
        /** Convenience constructor for a static API key. */
        constructor(apiKey: String) : this({ apiKey })
    }

    /**
     * Amazon Cognito User Pools authorization.
     * @param fetchToken Suspend function that returns a valid access/ID token.
     */
    class UserPools(
        internal val fetchToken: suspend () -> String
    ) : AppSyncClientAuthorizer(AppSyncAuthMode.USER_POOLS)

    /**
     * OpenID Connect authorization.
     * @param fetchToken Suspend function that returns a valid OIDC token.
     */
    class Oidc(
        internal val fetchToken: suspend () -> String
    ) : AppSyncClientAuthorizer(AppSyncAuthMode.OIDC)

    /**
     * AWS Lambda custom authorization.
     * @param fetchToken Suspend function that returns a valid authorization token.
     */
    class Lambda(
        internal val fetchToken: suspend () -> String
    ) : AppSyncClientAuthorizer(AppSyncAuthMode.LAMBDA)

    /**
     * IAM (SigV4) authorization.
     * @param credentialsProvider An [AwsCredentialsProvider] that supplies IAM credentials
     *   for SigV4 signing.
     */
    class Iam(
        internal val credentialsProvider: AwsCredentialsProvider<*>
    ) : AppSyncClientAuthorizer(AppSyncAuthMode.IAM)
}
