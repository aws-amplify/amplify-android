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

package com.amplifyframework.geo.location.auth

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper to provide credentials from Auth Cognito
 */
internal class CognitoCredentialsProvider(private val authCategory: AuthCategory) : CredentialsProvider {
    override suspend fun getCredentials(): Credentials {
        return suspendCancellableCoroutine { continuation ->
            authCategory.fetchAuthSession(
                { authSession ->
                    (authSession as? AWSCognitoAuthSession)?.awsCredentials?.value?.let {
                        continuation.resume(it)
                    } ?: continuation.resumeWithException(
                        Exception(
                            "Failed to get credentials. " +
                                "Check if you are signed in and configured identity pools correctly."
                        )
                    )
                },
                {
                    continuation.resumeWithException(it)
                }
            )
        }
    }
}
