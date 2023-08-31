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
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
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
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class AWSCognitoAuthPluginTest {

    companion object {
        const val CHANNEL_TIMEOUT = 1000L
    }

    private lateinit var authPlugin: AWSCognitoAuthPlugin
    private val realPlugin: RealAWSCognitoAuthPlugin = mockk(relaxed = true)

    @Before
    fun setup() {
        authPlugin = AWSCognitoAuthPlugin()
        authPlugin.realPlugin = realPlugin
    }

    @Test
    fun verifySignUp() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignUpOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, any(), any())
        }
    }

    @Test
    fun verifyConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignUp(expectedUsername, expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyOverloadedConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOptions = AuthConfirmSignUpOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignUp(
            expectedUsername,
            expectedConfirmationCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.resendSignUpCode(expectedUsername, any(), any()) }
    }

    @Test
    fun verifyOverloadedResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResendSignUpCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.resendSignUpCode(expectedUsername, expectedOptions, any(), any())
        }
    }

    @Test
    fun verifySignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.signIn(expectedUsername, expectedPassword, any(), any()) }
    }

    @Test
    fun verifyOverloadedSignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.signIn(
                expectedUsername,
                expectedPassword,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyConfirmSignIn() {
        val expectedChallengeResponse = "aaab"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedChallengeResponse, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.confirmSignIn(expectedChallengeResponse, any(), any()) }
    }

    @Test
    fun verifyOverloadedConfirmSignIn() {
        val expectedConfirmationCode = "aaab"
        val expectedOptions = AuthConfirmSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, any(), any())
        }
    }

    @Test
    fun verifySignInWithSocialWebUI() {
        val expectedProvider = AuthProvider.amazon()
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithSocialWebUI(expectedProvider, expectedActivity, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyOverloadedSignInWithSocialWebUI() {
        val expectedProvider = AuthProvider.amazon()
        val expectedActivity: Activity = mockk()
        val expectedOptions = AuthWebUISignInOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithSocialWebUI(
            expectedProvider,
            expectedActivity,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifySignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.signInWithWebUI(expectedActivity, any(), any()) }
    }

    @Test
    fun verifyOverloadedSignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOptions = AuthWebUISignInOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.signInWithWebUI(expectedActivity, expectedOptions, any(), any())
        }
    }

    @Test
    fun verifyHandleWebUISignInResponse() {
        val expectedIntent: Intent = mockk()

        authPlugin.handleWebUISignInResponse(expectedIntent)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.handleWebUISignInResponse(expectedIntent) }
    }

    @Test
    fun verifyOverloadedFetchAuthSession() {
        val expectedOptions = AuthFetchSessionOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSession> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchAuthSession(expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchAuthSession(expectedOptions, any(), any()) }
    }

    @Test
    fun verifyFetchAuthSession() {
        val expectedOnSuccess = Consumer<AuthSession> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchAuthSession(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchAuthSession(any(), any()) }
    }

    @Test
    fun verifyRememberDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.rememberDevice(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.rememberDevice(any(), any()) }
    }

    @Test
    fun verifyForgetDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.forgetDevice(any(), any()) }
    }

    @Test
    fun verifyOverloadedForgetDevice() {
        val expectedDevice = AuthDevice.fromId("id2")
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.forgetDevice(expectedDevice, any(), any()) }
    }

    @Test
    fun verifyFetchDevices() {
        val expectedOnSuccess = Consumer<List<AuthDevice>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchDevices(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchDevices(any(), any()) }
    }

    @Test
    fun verifyResetPassword() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.resetPassword(expectedUsername, any(), any()) }
    }

    @Test
    fun verifyOverloadedResetPassword() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResetPasswordOptions.defaults()
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.resetPassword(expectedUsername, expectedOptions, any(), any()) }
    }

    @Test
    fun verifyConfirmResetPassword() {
        val expectedUsername = "user1"
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmResetPassword(
                expectedUsername,
                expectedPassword,
                expectedCode,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyOverloadedConfirmResetPassword() {
        val expectedUsername = "user1"
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOptions = AuthConfirmResetPasswordOptions.defaults()
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmResetPassword(
                expectedUsername,
                expectedPassword,
                expectedCode,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyUpdatePassword() {
        val expectedOldPassword = "aldfkj1"
        val expectedNewPassword = "34w3ed"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updatePassword(expectedOldPassword, expectedNewPassword, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updatePassword(expectedOldPassword, expectedNewPassword, any(), any())
        }
    }

    @Test
    fun verifyFetchUserAttributes() {
        val expectedOnSuccess = Consumer<List<AuthUserAttribute>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchUserAttributes(any(), any()) }
    }

    @Test
    fun verifyUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.updateUserAttribute(expectedAttribute, any(), any()) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOptions = AuthUpdateUserAttributeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updateUserAttribute(expectedAttribute, expectedOptions, any(), any())
        }
    }

    @Test
    fun verifyUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOnSuccess = Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.updateUserAttributes(expectedAttributes, any(), any()) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOptions = AuthUpdateUserAttributesOptions.defaults()
        val expectedOnSuccess = Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOptions, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updateUserAttributes(
                expectedAttributes,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyEscapeHatch() {
        val expectedEscapeHatch = mockk<AWSCognitoAuthService>()
        every { realPlugin.escapeHatch() } returns expectedEscapeHatch

        assertEquals(expectedEscapeHatch, authPlugin.escapeHatch)
    }

    @Test
    fun verifyResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendUserAttributeConfirmationCode(expectedAttributeKey, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyOverloadedResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOptions = AuthResendUserAttributeConfirmationCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendUserAttributeConfirmationCode(
            expectedAttributeKey,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                expectedOptions,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyConfirmUserAttribute() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedConfirmationCode = "akj34"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmUserAttribute(
            expectedAttributeKey,
            expectedConfirmationCode,
            expectedOnSuccess,
            expectedOnError
        )

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.confirmUserAttribute(
                expectedAttributeKey,
                expectedConfirmationCode,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyGetCurrentUser() {
        val expectedOnSuccess = Consumer<AuthUser> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.getCurrentUser(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.getCurrentUser(any(), any()) }
    }

    @Test
    fun verifySignOut() {
        val expectedOnComplete = Consumer<AuthSignOutResult> { }

        authPlugin.signOut(expectedOnComplete)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.signOut(any()) }
    }

    @Test
    fun verifyOverloadedSignOut() {
        val expectedOptions = AuthSignOutOptions.builder().build()
        val expectedOnComplete = Consumer<AuthSignOutResult> { }

        authPlugin.signOut(expectedOptions, expectedOnComplete)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.signOut(expectedOptions, any()) }
    }

    @Test
    fun verifyDeleteUser() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.deleteUser(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.deleteUser(any(), any()) }
    }

    @Test
    fun verifyFederateToIdentityPool() {
        val expectedToken = "adsfkjadlfjk4"
        val expectedProvider = AuthProvider.amazon()
        val expectedOnSuccess = Consumer<FederateToIdentityPoolResult> { }
        val expectedOnError = Consumer<AuthException> { }
        val options = FederateToIdentityPoolOptions.builder().build()

        authPlugin.federateToIdentityPool(expectedToken, expectedProvider, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.federateToIdentityPool(
                expectedToken,
                expectedProvider,
                options,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyFederateToIdentityPoolWithOptions() {
        val expectedToken = "adsfkjadlfjk4"
        val expectedProvider = AuthProvider.amazon()
        val expectedOnSuccess = Consumer<FederateToIdentityPoolResult> { }
        val expectedOnError = Consumer<AuthException> { }
        val options = FederateToIdentityPoolOptions
            .builder()
            .developerProvidedIdentityId("devid")
            .build()

        authPlugin.federateToIdentityPool(expectedToken, expectedProvider, options, expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.federateToIdentityPool(
                expectedToken,
                expectedProvider,
                options,
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyClearFederationToIdentityPool() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.clearFederationToIdentityPool(expectedOnSuccess, expectedOnError)

        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.clearFederationToIdentityPool(any(), any()) }
    }

    @Test
    fun setUpTOTP() {
        val expectedOnSuccess = Consumer<TOTPSetupDetails> { }
        val expectedOnError = Consumer<AuthException> { }
        authPlugin.setUpTOTP(expectedOnSuccess, expectedOnError)
        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.setUpTOTP(any(), any()) }
    }

    @Test
    fun verifyTOTPSetup() {
        val code = "123456"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }
        authPlugin.verifyTOTPSetup(code, expectedOnSuccess, expectedOnError)
        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.verifyTOTPSetup(code, any(), any(), any()) }
    }

    @Test
    fun verifyTOTPSetupWithOptions() {
        val code = "123456"
        val options = AWSCognitoAuthVerifyTOTPSetupOptions.CognitoBuilder().friendlyDeviceName("DEVICE_NAME").build()
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }
        authPlugin.verifyTOTPSetup(code, options, expectedOnSuccess, expectedOnError)
        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.verifyTOTPSetup(code, options, any(), any()) }
    }

    @Test
    fun fetchMFAPreferences() {
        val expectedOnSuccess = Consumer<UserMFAPreference> { }
        val expectedOnError = Consumer<AuthException> { }
        authPlugin.fetchMFAPreference(expectedOnSuccess, expectedOnError)
        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchMFAPreference(any(), any()) }
    }

    @Test
    fun updateMFAPreferences() {
        val smsPreference = MFAPreference.ENABLED
        val totpPreference = MFAPreference.PREFERRED
        val onSuccess = Action { }
        val onError = Consumer<AuthException> { }
        authPlugin.updateMFAPreference(smsPreference, totpPreference, onSuccess, onError)
        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updateMFAPreference(smsPreference, totpPreference, any(), any())
        }
    }

    @Test
    fun verifyPluginKey() {
        assertEquals("awsCognitoAuthPlugin", authPlugin.pluginKey)
    }
}
