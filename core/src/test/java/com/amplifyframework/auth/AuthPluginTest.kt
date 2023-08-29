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

package com.amplifyframework.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test that ensures new Auth category APIs have a default implementation in the [AuthPlugin] class. This allows
 * 3rd party Auth plugins to compile against newer versions of Amplify.
 */
class AuthPluginTest {

    @Test
    fun `test plugin compiles`() {
        // The purpose of this test is to ensure that TestPlugin compiles, the assertion is irrelevant
        val plugin = TestPlugin()
        assertEquals("testVersion", plugin.version)
    }

    /**
     * DO NOT add any implementations to this class. The purpose of this test is to ensure that any new methods added
     * to the Auth category have default implementations in AuthPlugin.
     */
    private class TestPlugin : AuthPlugin<Unit>() {
        override fun signUp(
            username: String,
            password: String,
            options: AuthSignUpOptions,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmSignUp(
            username: String,
            confirmationCode: String,
            options: AuthConfirmSignUpOptions,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmSignUp(
            username: String,
            confirmationCode: String,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun resendSignUpCode(
            username: String,
            options: AuthResendSignUpCodeOptions,
            onSuccess: Consumer<AuthCodeDeliveryDetails>,
            onError: Consumer<AuthException>
        ) {}
        override fun resendSignUpCode(
            username: String,
            onSuccess: Consumer<AuthCodeDeliveryDetails>,
            onError: Consumer<AuthException>
        ) {}
        override fun signIn(
            username: String?,
            password: String?,
            options: AuthSignInOptions,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun signIn(
            username: String?,
            password: String?,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmSignIn(
            challengeResponse: String,
            options: AuthConfirmSignInOptions,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmSignIn(
            challengeResponse: String,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun signInWithSocialWebUI(
            provider: AuthProvider,
            callingActivity: Activity,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun signInWithSocialWebUI(
            provider: AuthProvider,
            callingActivity: Activity,
            options: AuthWebUISignInOptions,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun signInWithWebUI(
            callingActivity: Activity,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun signInWithWebUI(
            callingActivity: Activity,
            options: AuthWebUISignInOptions,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun handleWebUISignInResponse(intent: Intent?) {}
        override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {}
        override fun fetchAuthSession(
            options: AuthFetchSessionOptions,
            onSuccess: Consumer<AuthSession>,
            onError: Consumer<AuthException>
        ) {}
        override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {}
        override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {}
        override fun forgetDevice(device: AuthDevice, onSuccess: Action, onError: Consumer<AuthException>) {}
        override fun fetchDevices(onSuccess: Consumer<MutableList<AuthDevice>>, onError: Consumer<AuthException>) {}
        override fun resetPassword(
            username: String,
            options: AuthResetPasswordOptions,
            onSuccess: Consumer<AuthResetPasswordResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun resetPassword(
            username: String,
            onSuccess: Consumer<AuthResetPasswordResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmResetPassword(
            username: String,
            newPassword: String,
            confirmationCode: String,
            options: AuthConfirmResetPasswordOptions,
            onSuccess: Action,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmResetPassword(
            username: String,
            newPassword: String,
            confirmationCode: String,
            onSuccess: Action,
            onError: Consumer<AuthException>
        ) {}
        override fun updatePassword(
            oldPassword: String,
            newPassword: String,
            onSuccess: Action,
            onError: Consumer<AuthException>
        ) {}
        override fun fetchUserAttributes(
            onSuccess: Consumer<MutableList<AuthUserAttribute>>,
            onError: Consumer<AuthException>
        ) {}
        override fun updateUserAttribute(
            attribute: AuthUserAttribute,
            options: AuthUpdateUserAttributeOptions,
            onSuccess: Consumer<AuthUpdateAttributeResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun updateUserAttribute(
            attribute: AuthUserAttribute,
            onSuccess: Consumer<AuthUpdateAttributeResult>,
            onError: Consumer<AuthException>
        ) {}
        override fun updateUserAttributes(
            attributes: MutableList<AuthUserAttribute>,
            options: AuthUpdateUserAttributesOptions,
            onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
            onError: Consumer<AuthException>
        ) {}
        override fun updateUserAttributes(
            attributes: MutableList<AuthUserAttribute>,
            onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
            onError: Consumer<AuthException>
        ) {}
        override fun resendUserAttributeConfirmationCode(
            attributeKey: AuthUserAttributeKey,
            options: AuthResendUserAttributeConfirmationCodeOptions,
            onSuccess: Consumer<AuthCodeDeliveryDetails>,
            onError: Consumer<AuthException>
        ) {}
        override fun resendUserAttributeConfirmationCode(
            attributeKey: AuthUserAttributeKey,
            onSuccess: Consumer<AuthCodeDeliveryDetails>,
            onError: Consumer<AuthException>
        ) {}
        override fun confirmUserAttribute(
            attributeKey: AuthUserAttributeKey,
            confirmationCode: String,
            onSuccess: Action,
            onError: Consumer<AuthException>
        ) {}
        override fun getCurrentUser(onSuccess: Consumer<AuthUser>, onError: Consumer<AuthException>) {}
        override fun signOut(onComplete: Consumer<AuthSignOutResult>) {}
        override fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) {}
        override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {}
        override fun getPluginKey() = ""
        override fun configure(pluginConfiguration: JSONObject?, context: Context) {}
        override fun getEscapeHatch() = Unit
        override fun getVersion() = "testVersion"

        // DO NOT add any additional overrides. New APIs must have a default implementation in the AuthPlugin base class.
    }
}
