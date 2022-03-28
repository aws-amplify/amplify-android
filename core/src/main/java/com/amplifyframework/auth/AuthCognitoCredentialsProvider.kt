/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Internal utility class to interface with Auth.
 */
class AuthCognitoCredentialsProvider : AuthCredentialsProvider {
    /**
     * Get the identity ID of the currently logged in user if they are registered in identity pools.
     */
    override suspend fun getIdentityId(): String {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { session ->
                    val identityId = (session as? AmplifySession)?.identityId
                    identityId?.value?.let {
                        continuation.resume(it)
                    } ?: continuation.resumeWithException(
                        AuthException(
                            "Failed to get user's identity ID",
                            "Please check that you are logged in and " +
                                "that Auth is setup to support identity pools."
                        )
                    )
                },
                { continuation.resumeWithException(it) }
            )
        }
    }

    /**
     * Get AWS Credentials object needed by other categories.
     * @return AWS Credentials
     */
    override suspend fun getCredentials(): Credentials {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { session ->
                    val credentials = (session as? AmplifySession)?.awsCredentials
                    credentials?.value?.let {
                        continuation.resume(it)
                    } ?: continuation.resumeWithException(
                        AuthException(
                            "Failed to get AWS credentials",
                            "Please check that you are logged in or " +
                                "that Auth is setup to support identity pools."
                        )
                    )
                },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
