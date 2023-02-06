/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.api.ApiException
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wrapper to provide credentials from Auth synchronously and asynchronously
 */
open class CognitoCredentialsProvider : CredentialsProvider, AuthCredentialsProvider {

    /**
     * Request [Credentials] from the provider.
     */
    override suspend fun getCredentials(): Credentials {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    authSession.toAWSAuthSession()?.awsCredentialsResult?.value?.let {
                        continuation.resume(it.toCredentials())
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

    /**
     * Request identityId from the provider.
     */
    override suspend fun getIdentityId(): String {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    authSession.toAWSAuthSession()?.identityIdResult?.value?.let {
                        continuation.resume(it)
                    } ?: continuation.resumeWithException(
                        Exception(
                            "Failed to get identity ID. " +
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

    fun getAccessToken(onResult: Consumer<String>, onFailure: Consumer<Exception>) {
        Amplify.Auth.fetchAuthSession(
            { session ->
                val tokens = session.toAWSAuthSession()?.userPoolTokensResult?.value?.accessToken
                tokens?.let { onResult.accept(tokens) }
                    ?: onFailure.accept(
                        ApiException.ApiAuthException(
                            "Token is null",
                            "Token received but is null. Check if you are signed in"
                        )
                    )
            },
            {
                onFailure.accept(it)
            }
        )
    }
}

private fun AuthSession.toAWSAuthSession(): AWSAuthSessionInternal? {
    if (this is AWSAuthSessionInternal) {
        return this
    }

    return null
}

private fun AWSCredentials.toCredentials(): Credentials {
    return Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey,
        sessionToken = (this as? AWSTemporaryCredentials)?.sessionToken,
        expiration = (this as? AWSTemporaryCredentials)?.expiration
    )
}
