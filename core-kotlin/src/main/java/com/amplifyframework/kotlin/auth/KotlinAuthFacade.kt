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
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.options.AuthAssociateWebAuthnCredentialsOptions
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthDeleteWebAuthnCredentialOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions
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
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KotlinAuthFacade(private val delegate: Delegate = Amplify.Auth) : Auth {
    override suspend fun signUp(username: String, password: String?, options: AuthSignUpOptions): AuthSignUpResult =
        suspendCoroutine { continuation ->
            delegate.signUp(
                username,
                password,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions
    ): AuthSignUpResult = suspendCoroutine { continuation ->
        delegate.confirmSignUp(
            username,
            confirmationCode,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions
    ): AuthCodeDeliveryDetails = suspendCoroutine { continuation ->
        delegate.resendSignUpCode(
            username,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun signIn(username: String?, password: String?, options: AuthSignInOptions): AuthSignInResult =
        suspendCoroutine { continuation ->
            delegate.signIn(
                username,
                password,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun confirmSignIn(challengeResponse: String, options: AuthConfirmSignInOptions): AuthSignInResult =
        suspendCoroutine { continuation ->
            delegate.confirmSignIn(
                challengeResponse,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun autoSignIn(): AuthSignInResult = suspendCoroutine { continuation ->
        delegate.autoSignIn(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun signInWithSocialWebUI(
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

    override suspend fun signInWithWebUI(callingActivity: Activity, options: AuthWebUISignInOptions): AuthSignInResult =
        suspendCoroutine { continuation ->
            delegate.signInWithWebUI(
                callingActivity,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override fun handleWebUISignInResponse(intent: Intent) {
        delegate.handleWebUISignInResponse(intent)
    }

    override suspend fun fetchAuthSession(options: AuthFetchSessionOptions): AuthSession =
        suspendCoroutine { continuation ->
            delegate.fetchAuthSession(
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun rememberDevice() = suspendCoroutine { continuation ->
        delegate.rememberDevice(
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun forgetDevice(device: AuthDevice?) = suspendCoroutine { continuation ->
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

    override suspend fun fetchDevices(): List<AuthDevice> = suspendCoroutine { continuation ->
        delegate.fetchDevices(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun resetPassword(username: String, options: AuthResetPasswordOptions): AuthResetPasswordResult =
        suspendCoroutine { continuation ->
            delegate.resetPassword(
                username,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions
    ) = suspendCoroutine { continuation ->
        delegate.confirmResetPassword(
            username,
            newPassword,
            confirmationCode,
            options,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun updatePassword(oldPassword: String, newPassword: String) = suspendCoroutine { continuation ->
        delegate.updatePassword(
            oldPassword,
            newPassword,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun fetchUserAttributes(): List<AuthUserAttribute> = suspendCoroutine { continuation ->
        delegate.fetchUserAttributes(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions
    ): AuthUpdateAttributeResult = suspendCoroutine { continuation ->
        delegate.updateUserAttribute(
            attribute,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> = suspendCoroutine { continuation ->
        delegate.updateUserAttributes(
            attributes,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions
    ): AuthCodeDeliveryDetails = suspendCoroutine { continuation ->
        delegate.resendUserAttributeConfirmationCode(
            attributeKey,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun confirmUserAttribute(attributeKey: AuthUserAttributeKey, confirmationCode: String) =
        suspendCoroutine { continuation ->
            delegate.confirmUserAttribute(
                attributeKey,
                confirmationCode,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun getCurrentUser(): AuthUser = suspendCoroutine { continuation ->
        delegate.getCurrentUser(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun signOut(options: AuthSignOutOptions): AuthSignOutResult = suspendCoroutine { continuation ->
        delegate.signOut(options) { continuation.resume(it) }
    }

    override suspend fun deleteUser() = suspendCoroutine { continuation ->
        delegate.deleteUser(
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun setUpTOTP(): TOTPSetupDetails = suspendCoroutine { continuation ->
        delegate.setUpTOTP({
            continuation.resume(it)
        }, {
            continuation.resumeWithException(it)
        })
    }

    override suspend fun verifyTOTPSetup(code: String, options: AuthVerifyTOTPSetupOptions) =
        suspendCoroutine { continuation ->
            delegate.verifyTOTPSetup(code, options, {
                continuation.resume(Unit)
            }, {
                continuation.resumeWithException(it)
            })
        }

    override suspend fun associateWebAuthnCredential(
        callingActivity: Activity,
        options: AuthAssociateWebAuthnCredentialsOptions
    ) = suspendCoroutine { continuation ->
        delegate.associateWebAuthnCredential(
            callingActivity,
            options,
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun listWebAuthnCredentials(options: AuthListWebAuthnCredentialsOptions) =
        suspendCoroutine { continuation ->
            delegate.listWebAuthnCredentials(
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun deleteWebAuthnCredential(credentialId: String, options: AuthDeleteWebAuthnCredentialOptions) =
        suspendCoroutine { continuation ->
            delegate.deleteWebAuthnCredential(
                credentialId,
                options,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
}
