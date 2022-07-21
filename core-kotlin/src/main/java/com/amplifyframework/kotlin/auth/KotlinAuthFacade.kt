/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.auth

import android.app.Activity
import android.content.Intent
import com.amplifyframework.auth.AuthCategoryBehavior as Delegate
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KotlinAuthFacade(private val delegate: Delegate = Amplify.Auth) : Auth {
    override suspend fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions
    ): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            delegate.signUp(
                username,
                password,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions
    ): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            delegate.confirmSignUp(
                username,
                confirmationCode,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions
    ): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            delegate.resendSignUpCode(
                username,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.signIn(
                username,
                password,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun confirmSignIn(
        confirmationCode: String,
        options: AuthConfirmSignInOptions
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.confirmSignIn(
                confirmationCode,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions
    ):
        AuthSignInResult {
            return suspendCoroutine { continuation ->
                delegate.signInWithSocialWebUI(
                    provider,
                    callingActivity,
                    options,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
        }

    override suspend fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.signInWithWebUI(
                callingActivity,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override fun handleWebUISignInResponse(intent: Intent) {
        delegate.handleWebUISignInResponse(intent)
    }

    override suspend fun fetchAuthSession(): AuthSession {
        return suspendCoroutine { continuation ->
            delegate.fetchAuthSession(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun rememberDevice() {
        return suspendCoroutine { continuation ->
            delegate.rememberDevice(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun forgetDevice(device: AuthDevice?) {
        return suspendCoroutine { continuation ->
            if (device == null) {
                delegate.forgetDevice(
                    { continuation.resume(Unit) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.forgetDevice(
                    device,
                    { continuation.resume(Unit) },
                    { continuation.resumeWithException(it) }
                )
            }
        }
    }

    override suspend fun fetchDevices(): List<AuthDevice> {
        return suspendCoroutine { continuation ->
            delegate.fetchDevices(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions
    ): AuthResetPasswordResult {
        return suspendCoroutine { continuation ->
            delegate.resetPassword(
                username,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun confirmResetPassword(
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions
    ) {
        return suspendCoroutine { continuation ->
            delegate.confirmResetPassword(
                newPassword,
                confirmationCode,
                options,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun updatePassword(oldPassword: String, newPassword: String) {
        return suspendCoroutine { continuation ->
            delegate.updatePassword(
                oldPassword,
                newPassword,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun fetchUserAttributes(): List<AuthUserAttribute> {
        return suspendCoroutine { continuation ->
            delegate.fetchUserAttributes(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions
    ): AuthUpdateAttributeResult {
        return suspendCoroutine { continuation ->
            delegate.updateUserAttribute(
                attribute,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> {
        return suspendCoroutine { continuation ->
            delegate.updateUserAttributes(
                attributes,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions
    ): AuthCodeDeliveryDetails {
        return suspendCoroutine { continuation ->
            delegate.resendUserAttributeConfirmationCode(
                attributeKey,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String
    ) {
        return suspendCoroutine { continuation ->
            delegate.confirmUserAttribute(
                attributeKey,
                confirmationCode,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun getCurrentUser(): AuthUser {
        return suspendCoroutine { continuation ->
            delegate.getCurrentUser(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun signOut(options: AuthSignOutOptions) {
        return suspendCoroutine { continuation ->
            delegate.signOut(
                options,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun deleteUser() {
        return suspendCoroutine { continuation ->
            delegate.deleteUser(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
