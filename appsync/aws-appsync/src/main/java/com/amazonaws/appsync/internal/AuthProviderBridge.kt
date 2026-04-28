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
package com.amazonaws.appsync.internal

import com.amazonaws.appsync.AppSyncAuthorization
import com.amazonaws.appsync.AppSyncClientAuthorizer
import com.amplifyframework.api.aws.ApiAuthProviders
import com.amplifyframework.foundation.credentials.toSmithyProvider
import kotlinx.coroutines.runBlocking

/**
 * Bridges [AppSyncClientAuthorizer] instances into [ApiAuthProviders] so that
 * [SubscriptionAuthorizer] uses the user-provided token fetchers instead of falling
 * back to default Cognito/OIDC providers.
 */
internal object AuthProviderBridge {

    /**
     * Build an [ApiAuthProviders] from the authorizers in an [AppSyncAuthorization].
     * Each authorizer subtype is mapped to the corresponding provider interface.
     */
    fun buildApiAuthProviders(authorization: AppSyncAuthorization): ApiAuthProviders {
        val authorizers = when (authorization) {
            is AppSyncAuthorization.Single -> listOf(authorization.authorizer)
            is AppSyncAuthorization.Multi -> authorization.authorizers
        }

        val builder = ApiAuthProviders.builder()

        for (authorizer in authorizers) {
            when (authorizer) {
                is AppSyncClientAuthorizer.ApiKey -> {
                    builder.apiKeyAuthProvider { runBlocking { authorizer.fetchApiKey() } }
                }
                is AppSyncClientAuthorizer.UserPools -> {
                    builder.cognitoUserPoolsAuthProvider(
                        object : com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider {
                            override fun getLatestAuthToken(): String =
                                runBlocking { authorizer.fetchToken() }
                            // getUsername() is never called in production code —
                            // SubscriptionAuthorizer and AuthRuleRequestDecorator only use
                            // getLatestAuthToken(). Throw to surface any unexpected usage.
                            override fun getUsername(): String =
                                throw NotImplementedError(
                                    "getUsername() is not supported by AmplifyAppSyncClient."
                                )
                        }
                    )
                }
                is AppSyncClientAuthorizer.Oidc -> {
                    builder.oidcAuthProvider { runBlocking { authorizer.fetchToken() } }
                }
                is AppSyncClientAuthorizer.Lambda -> {
                    builder.functionAuthProvider { runBlocking { authorizer.fetchToken() } }
                }
                is AppSyncClientAuthorizer.Iam -> {
                    builder.awsCredentialsProvider(authorizer.credentialsProvider.toSmithyProvider())
                }
            }
        }

        return builder.build()
    }
}
