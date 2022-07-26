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

package com.amplifyframework.predictions.aws.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wrapper to provide credentials from Auth synchronously and asynchronously
 */
internal class CognitoCredentialsProvider : CredentialsProvider {
    /**
     * Request [Credentials] from the provider.
     */
    override suspend fun getCredentials(): Credentials {
        return suspendCancellableCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
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
