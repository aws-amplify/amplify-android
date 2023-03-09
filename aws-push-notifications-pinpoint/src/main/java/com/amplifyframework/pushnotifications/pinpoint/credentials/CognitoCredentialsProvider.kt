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

package com.amplifyframework.pushnotifications.pinpoint.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
/**
 * Wrapper to provide credentials from Auth synchronously and asynchronously
 */
open class CognitoCredentialsProvider : AuthCredentialsProvider {

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
                        AuthException(
                            "Failed to get credentials. " +
                                "Check if you are signed in and configured identity pools correctly.",
                            AmplifyException.TODO_RECOVERY_SUGGESTION
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
                        AuthException(
                            "Failed to get identity ID. " +
                                "Check if you are signed in and configured identity pools correctly.",
                            AmplifyException.TODO_RECOVERY_SUGGESTION
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
