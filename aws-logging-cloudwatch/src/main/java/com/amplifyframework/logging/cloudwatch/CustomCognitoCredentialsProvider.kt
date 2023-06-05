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
package com.amplifyframework.logging.cloudwatch

import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class CustomCognitoCredentialsProvider() : CognitoCredentialsProvider() {

    suspend fun getCurrentUser(): AuthUser {
        return suspendCoroutine { continuation ->
            Amplify.Auth.getCurrentUser({
                continuation.resume(it)
            }, {
                continuation.resumeWithException(it)
            })
        }
    }
}
