/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync.util

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.auth.CognitoCredentialsProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class AccessTokenProvider(
    private val credentialsProvider: AuthCredentialsProvider = CognitoCredentialsProvider()
) {
    suspend fun fetchLatestCognitoAuthToken() = credentialsProvider.getAccessToken()

    private suspend fun AuthCredentialsProvider.getAccessToken() = suspendCoroutine { continuation ->
        getAccessToken(
            { token -> continuation.resume(token) },
            { error -> continuation.resumeWithException(error) }
        )
    }
}
