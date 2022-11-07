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

package com.amplifyframework.api.aws.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.api.ApiException
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Wrapper to provide credentials from Auth synchronously and asynchronously
 */
internal open class CognitoCredentialsProvider : CredentialsProvider {
    /**
     * Request [Credentials] synchronously by blocking current suspend execution.
     */
    fun getCredentialsBlocking(): Credentials {
        return runBlocking {
            withContext(Dispatchers.IO) {
                getCredentials()
            }
        }
    }

    /**
     * Request [Credentials] from the provider.
     */
    override suspend fun getCredentials(): Credentials {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    (authSession as? AWSCognitoAuthSession)?.awsCredentialsResult?.value?.let {
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

    fun getAccessToken(onResult: Consumer<String>, onFailure: Consumer<Exception>) {
        Amplify.Auth.fetchAuthSession(
            { session ->
                val tokens = (session as? AWSCognitoAuthSession)?.userPoolTokensResult?.value?.accessToken
                tokens?.let { onResult.accept(it) }
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

private fun AWSCredentials.toCredentials(): Credentials {
    return Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey,
        sessionToken = (this as? AWSTemporaryCredentials)?.sessionToken,
        expiration = (this as? AWSTemporaryCredentials)?.expiration
    )
}
