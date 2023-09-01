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
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class KotlinAuthFacadeInternal(private val delegate: RealAWSCognitoAuthPlugin) {

    suspend fun signUp(
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

    suspend fun confirmSignUp(
        username: String,
        confirmationCode: String
    ): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            delegate.confirmSignUp(
                username,
                confirmationCode,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun confirmSignUp(
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

    suspend fun resendSignUpCode(
        username: String
    ): AuthCodeDeliveryDetails {
        return suspendCoroutine { continuation ->
            delegate.resendSignUpCode(
                username,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions
    ): AuthCodeDeliveryDetails {
        return suspendCoroutine { continuation ->
            delegate.resendSignUpCode(
                username,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signIn(
        username: String?,
        password: String?
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.signIn(
                username,
                password,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signIn(
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

    suspend fun confirmSignIn(
        challengeResponse: String
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.confirmSignIn(
                challengeResponse,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.confirmSignIn(
                challengeResponse,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.signInWithSocialWebUI(
                provider,
                callingActivity,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions
    ): AuthSignInResult {
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

    suspend fun signInWithWebUI(
        callingActivity: Activity
    ): AuthSignInResult {
        return suspendCoroutine { continuation ->
            delegate.signInWithWebUI(
                callingActivity,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signInWithWebUI(
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

    fun handleWebUISignInResponse(intent: Intent?) {
        delegate.handleWebUISignInResponse(intent)
    }

    suspend fun fetchAuthSession(): AuthSession {
        return suspendCoroutine { continuation ->
            delegate.fetchAuthSession(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun fetchAuthSession(options: AuthFetchSessionOptions): AuthSession {
        return suspendCoroutine { continuation ->
            delegate.fetchAuthSession(
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun rememberDevice() {
        return suspendCoroutine { continuation ->
            delegate.rememberDevice(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun forgetDevice() {
        return suspendCoroutine { continuation ->
            delegate.forgetDevice(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun forgetDevice(device: AuthDevice) {
        return suspendCoroutine { continuation ->
            delegate.forgetDevice(
                device,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun fetchDevices(): List<AuthDevice> {
        return suspendCoroutine { continuation ->
            delegate.fetchDevices(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun resetPassword(
        username: String
    ): AuthResetPasswordResult {
        return suspendCoroutine { continuation ->
            delegate.resetPassword(
                username,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun resetPassword(
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

    suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String
    ) {
        return suspendCoroutine { continuation ->
            delegate.confirmResetPassword(
                username,
                newPassword,
                confirmationCode,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions
    ) {
        return suspendCoroutine { continuation ->
            delegate.confirmResetPassword(
                username,
                newPassword,
                confirmationCode,
                options,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String) {
        return suspendCoroutine { continuation ->
            delegate.updatePassword(
                oldPassword,
                newPassword,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun fetchUserAttributes(): List<AuthUserAttribute> {
        return suspendCoroutine { continuation ->
            delegate.fetchUserAttributes(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun updateUserAttribute(
        attribute: AuthUserAttribute
    ): AuthUpdateAttributeResult {
        return suspendCoroutine { continuation ->
            delegate.updateUserAttribute(
                attribute,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun updateUserAttribute(
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

    suspend fun updateUserAttributes(
        attributes: List<AuthUserAttribute>
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> {
        return suspendCoroutine { continuation ->
            delegate.updateUserAttributes(
                attributes,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun updateUserAttributes(
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

    suspend fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey
    ): AuthCodeDeliveryDetails {
        return suspendCoroutine { continuation ->
            delegate.resendUserAttributeConfirmationCode(
                attributeKey,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun resendUserAttributeConfirmationCode(
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

    suspend fun confirmUserAttribute(
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

    suspend fun getCurrentUser(): AuthUser {
        return suspendCoroutine { continuation ->
            delegate.getCurrentUser(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun signOut(): AuthSignOutResult {
        return suspendCoroutine { continuation ->
            delegate.signOut { continuation.resume(it) }
        }
    }

    suspend fun signOut(options: AuthSignOutOptions): AuthSignOutResult {
        return suspendCoroutine { continuation ->
            delegate.signOut(options) { continuation.resume(it) }
        }
    }

    suspend fun deleteUser() {
        return suspendCoroutine { continuation ->
            delegate.deleteUser(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions?
    ): FederateToIdentityPoolResult {
        return suspendCoroutine { continuation ->
            delegate.federateToIdentityPool(
                providerToken,
                authProvider,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun clearFederationToIdentityPool() {
        return suspendCoroutine { continuation ->
            delegate.clearFederationToIdentityPool(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun setUpTOTP(): TOTPSetupDetails {
        return suspendCoroutine { continuation ->
            delegate.setUpTOTP(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }
    suspend fun verifyTOTPSetup(code: String, options: AuthVerifyTOTPSetupOptions) {
        return suspendCoroutine { continuation ->
            delegate.verifyTOTPSetup(
                code,
                options,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun fetchMFAPreference(): UserMFAPreference {
        return suspendCoroutine { continuation ->
            delegate.fetchMFAPreference(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    suspend fun updateMFAPreference(
        sms: MFAPreference?,
        totp: MFAPreference?
    ) {
        return suspendCoroutine { continuation ->
            delegate.updateMFAPreference(
                sms,
                totp,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
