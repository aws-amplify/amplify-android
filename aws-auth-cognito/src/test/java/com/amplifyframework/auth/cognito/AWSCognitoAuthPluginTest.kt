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
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthListWebAuthnCredentialsOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
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
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.coVerify
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
        authPlugin.useCaseFactory = mockk(relaxed = true)
    }

    @Test
    fun verifySignUp() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignUpOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.signUp()

        authPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(expectedUsername, expectedPassword, expectedOptions)
        }
    }

    @Test
    fun verifyConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.confirmSignUp()

        authPlugin.confirmSignUp(expectedUsername, expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(
                expectedUsername,
                expectedConfirmationCode
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

        val useCase = authPlugin.useCaseFactory.confirmSignUp()

        authPlugin.confirmSignUp(
            expectedUsername,
            expectedConfirmationCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(
                expectedUsername,
                expectedConfirmationCode,
                expectedOptions
            )
        }
    }

    @Test
    fun verifyResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.resendSignupCode()

        authPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedUsername, any()) }
    }

    @Test
    fun verifyOverloadedResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResendSignUpCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.resendSignupCode()

        authPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(expectedUsername, expectedOptions)
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

        val useCase = authPlugin.useCaseFactory.rememberDevice()

        authPlugin.rememberDevice(expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
    }

    @Test
    fun verifyForgetDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.forgetDevice()

        authPlugin.forgetDevice(expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
    }

    @Test
    fun verifyOverloadedForgetDevice() {
        val expectedDevice = AuthDevice.fromId("id2")
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.forgetDevice()

        authPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedDevice) }
    }

    @Test
    fun verifyFetchDevices() {
        val expectedOnSuccess = Consumer<List<AuthDevice>> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.fetchDevices()

        authPlugin.fetchDevices(expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
    }

    @Test
    fun verifyResetPassword() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.resetPassword()

        authPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedUsername, any()) }
    }

    @Test
    fun verifyOverloadedResetPassword() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResetPasswordOptions.defaults()
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.resetPassword()

        authPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedUsername, expectedOptions) }
    }

    @Test
    fun verifyConfirmResetPassword() {
        val expectedUsername = "user1"
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.confirmResetPassword()

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOnSuccess,
            expectedOnError
        )

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(
                expectedUsername,
                expectedPassword,
                expectedCode
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

        val useCase = authPlugin.useCaseFactory.confirmResetPassword()

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(
                expectedUsername,
                expectedPassword,
                expectedCode,
                expectedOptions
            )
        }
    }

    @Test
    fun verifyUpdatePassword() {
        val expectedOldPassword = "aldfkj1"
        val expectedNewPassword = "34w3ed"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.updatePassword()

        authPlugin.updatePassword(expectedOldPassword, expectedNewPassword, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(expectedOldPassword, expectedNewPassword)
        }
    }

    @Test
    fun verifyFetchUserAttributes() {
        val expectedOnSuccess = Consumer<List<AuthUserAttribute>> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.fetchUserAttributes()

        authPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
    }

    @Test
    fun verifyUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.updateUserAttributes()

        authPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttribute) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOptions = AuthUpdateUserAttributeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.updateUserAttributes()

        authPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(expectedAttribute, expectedOptions)
        }
    }

    @Test
    fun verifyUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOnSuccess = Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.updateUserAttributes()

        authPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttributes) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOptions = AuthUpdateUserAttributesOptions.defaults()
        val expectedOnSuccess = Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.updateUserAttributes()

        authPlugin.updateUserAttributes(expectedAttributes, expectedOptions, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttributes, expectedOptions) }
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

        val useCase = authPlugin.useCaseFactory.resendUserAttributeConfirmation()

        authPlugin.resendUserAttributeConfirmationCode(expectedAttributeKey, expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttributeKey) }
    }

    @Test
    fun verifyOverloadedResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOptions = AuthResendUserAttributeConfirmationCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.resendUserAttributeConfirmation()

        authPlugin.resendUserAttributeConfirmationCode(
            expectedAttributeKey,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttributeKey, expectedOptions) }
    }

    @Test
    fun verifyConfirmUserAttribute() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedConfirmationCode = "akj34"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.confirmUserAttribute()

        authPlugin.confirmUserAttribute(
            expectedAttributeKey,
            expectedConfirmationCode,
            expectedOnSuccess,
            expectedOnError
        )

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(expectedAttributeKey, expectedConfirmationCode) }
    }

    @Test
    fun verifyGetCurrentUser() {
        val expectedOnSuccess = Consumer<AuthUser> { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.getCurrentUser()

        authPlugin.getCurrentUser(expectedOnSuccess, expectedOnError)

        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
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

        val useCase = authPlugin.useCaseFactory.setupTotp()

        authPlugin.setUpTOTP(expectedOnSuccess, expectedOnError)
        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute() }
    }

    @Test
    fun verifyTOTPSetup() {
        val code = "123456"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.verifyTotpSetup()

        authPlugin.verifyTOTPSetup(code, expectedOnSuccess, expectedOnError)
        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(code, any()) }
    }

    @Test
    fun verifyTOTPSetupWithOptions() {
        val code = "123456"
        val options = AWSCognitoAuthVerifyTOTPSetupOptions.CognitoBuilder().friendlyDeviceName("DEVICE_NAME").build()
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        val useCase = authPlugin.useCaseFactory.verifyTotpSetup()

        authPlugin.verifyTOTPSetup(code, options, expectedOnSuccess, expectedOnError)
        coVerify(timeout = CHANNEL_TIMEOUT) { useCase.execute(code, options) }
    }

    @Test
    fun fetchMFAPreferences() {
        val expectedOnSuccess = Consumer<UserMFAPreference> { }
        val expectedOnError = Consumer<AuthException> { }
        authPlugin.fetchMFAPreference(expectedOnSuccess, expectedOnError)
        verify(timeout = CHANNEL_TIMEOUT) { realPlugin.fetchMFAPreference(any(), any()) }
    }

    @Test
    fun updateMFAPreferencesDeprecatedApi() {
        val smsPreference = MFAPreference.ENABLED
        val totpPreference = MFAPreference.PREFERRED
        val onSuccess = Action { }
        val onError = Consumer<AuthException> { }
        authPlugin.updateMFAPreference(smsPreference, totpPreference, onSuccess, onError)
        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updateMFAPreference(smsPreference, totpPreference, null, any(), any())
        }
    }

    @Test
    fun updateMFAPreferences() {
        val smsPreference = MFAPreference.ENABLED
        val totpPreference = MFAPreference.PREFERRED
        val emailPreference = MFAPreference.NOT_PREFERRED
        val onSuccess = Action { }
        val onError = Consumer<AuthException> { }
        authPlugin.updateMFAPreference(smsPreference, totpPreference, emailPreference, onSuccess, onError)
        verify(timeout = CHANNEL_TIMEOUT) {
            realPlugin.updateMFAPreference(smsPreference, totpPreference, emailPreference, any(), any())
        }
    }

    @Test
    fun associateWebAuthnCredential() {
        val useCase = authPlugin.useCaseFactory.associateWebAuthnCredential()

        val activity: Activity = mockk()
        authPlugin.associateWebAuthnCredential(activity, {}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(activity, AuthAssociateWebAuthnCredentialsOptions.defaults())
        }
    }

    @Test
    fun associateWebAuthnCredentialWithOptions() {
        val useCase = authPlugin.useCaseFactory.associateWebAuthnCredential()

        val activity: Activity = mockk()
        val options: AuthAssociateWebAuthnCredentialsOptions = mockk()
        authPlugin.associateWebAuthnCredential(activity, options, {}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(activity, options)
        }
    }

    @Test
    fun listWebAuthnCredentials() {
        val useCase = authPlugin.useCaseFactory.listWebAuthnCredentials()
        authPlugin.listWebAuthnCredentials({}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(AuthListWebAuthnCredentialsOptions.defaults())
        }
    }

    @Test
    fun listWebAuthnCredentialsWithOptions() {
        val useCase = authPlugin.useCaseFactory.listWebAuthnCredentials()
        val options = AWSCognitoAuthListWebAuthnCredentialsOptions.builder().build()
        authPlugin.listWebAuthnCredentials(options, {}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(options)
        }
    }

    @Test
    fun deleteWebAuthnCredential() {
        val useCase = authPlugin.useCaseFactory.deleteWebAuthnCredential()
        val credentialId = "someId"
        authPlugin.deleteWebAuthnCredential(credentialId, {}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(credentialId, AuthDeleteWebAuthnCredentialOptions.defaults())
        }
    }

    @Test
    fun deleteWebAuthnCredentialWithOptions() {
        val useCase = authPlugin.useCaseFactory.deleteWebAuthnCredential()
        val options: AuthDeleteWebAuthnCredentialOptions = mockk()
        val credentialId = "someId"
        authPlugin.deleteWebAuthnCredential(credentialId, options, {}, {})
        coVerify(timeout = CHANNEL_TIMEOUT) {
            useCase.execute(credentialId, options)
        }
    }

    @Test
    fun verifyPluginKey() {
        assertEquals("awsCognitoAuthPlugin", authPlugin.pluginKey)
    }
}
