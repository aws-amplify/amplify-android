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

package com.amplifyframework.auth.cognito

import android.app.Activity
import android.content.Intent
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class KotlinAuthFacadeInternal(private val delegate: RealAWSCognitoAuthPlugin) {

    suspend fun signInWithSocialWebUI(provider: AuthProvider, callingActivity: Activity): AuthSignInResult =
        suspendCoroutine { continuation ->
            delegate.signInWithSocialWebUI(
                provider,
                callingActivity,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    suspend fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions
    ): AuthSignInResult = suspendCoroutine { continuation ->
        delegate.signInWithSocialWebUI(
            provider,
            callingActivity,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun signInWithWebUI(callingActivity: Activity): AuthSignInResult = suspendCoroutine { continuation ->
        delegate.signInWithWebUI(
            callingActivity,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun signInWithWebUI(callingActivity: Activity, options: AuthWebUISignInOptions): AuthSignInResult =
        suspendCoroutine { continuation ->
            delegate.signInWithWebUI(
                callingActivity,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    fun handleWebUISignInResponse(intent: Intent?) {
        delegate.handleWebUISignInResponse(intent)
    }

    suspend fun fetchAuthSession(): AuthSession = suspendCoroutine { continuation ->
        delegate.fetchAuthSession(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun fetchAuthSession(options: AuthFetchSessionOptions): AuthSession = suspendCoroutine { continuation ->
        delegate.fetchAuthSession(
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun signOut(): AuthSignOutResult = suspendCoroutine { continuation ->
        delegate.signOut { continuation.resume(it) }
    }

    suspend fun signOut(options: AuthSignOutOptions): AuthSignOutResult = suspendCoroutine { continuation ->
        delegate.signOut(options) { continuation.resume(it) }
    }

    suspend fun federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions?
    ): FederateToIdentityPoolResult = suspendCoroutine { continuation ->
        delegate.federateToIdentityPool(
            providerToken,
            authProvider,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun clearFederationToIdentityPool() = suspendCoroutine { continuation ->
        delegate.clearFederationToIdentityPool(
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }
}
