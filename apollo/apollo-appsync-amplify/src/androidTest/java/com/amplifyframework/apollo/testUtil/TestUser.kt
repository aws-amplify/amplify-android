/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.testUtil

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TestUser(
    private val email: String = "apolloTestUser2@test.com",
    private val password: String = "ApolloTestPassword"
) {
    suspend fun create() = suspendCoroutine { continuation ->
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), email)
            .build()

        Amplify.Auth.signUp(
            email,
            password,
            options,
            { continuation.resume(Unit) },
            { error ->
                when (error) {
                    is UsernameExistsException -> continuation.resume(Unit) //
                    else -> continuation.resumeWithException(error)
                }
            }
        )
    }

    suspend fun signIn() = suspendCoroutine { continuation ->
        Amplify.Auth.signIn(
            email,
            password,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun signOut() = suspendCoroutine { continuation -> Amplify.Auth.signOut { continuation.resume(Unit) } }
}
