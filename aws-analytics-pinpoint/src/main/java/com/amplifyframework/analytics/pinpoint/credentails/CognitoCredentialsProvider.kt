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
package com.amplifyframework.analytics.pinpoint.credentails

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
/**
 * Internal implementation of cognito credentials provider.
 * This will be ported to core once it seems feasible to do so.
 */
internal class CognitoCredentialsProvider : AuthCredentialsProvider {
    /**
     * Request identityId from the provider.
     */
    override suspend fun getIdentityId(): String {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    authSession.toAWSCognitoAuthSession()?.identityIdResult?.value?.let {
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

    /**
     * Request [Credentials] from the provider.
     */
    override suspend fun getCredentials(): Credentials {
        return suspendCoroutine { continuation ->
            Amplify.Auth.fetchAuthSession(
                { authSession ->
                    authSession.toAWSCognitoAuthSession()?.awsCredentialsResult?.value?.let {
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
}

private fun AWSCredentials.toCredentials(): Credentials {
    return Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey,
        sessionToken = (this as? AWSTemporaryCredentials)?.sessionToken,
        expiration = (this as? AWSTemporaryCredentials)?.expiration
    )
}

private fun AuthSession.toAWSCognitoAuthSession(): AWSCognitoAuthSession? {
    if (this is AWSCognitoAuthSession) {
        return this
    }

    return null
}
