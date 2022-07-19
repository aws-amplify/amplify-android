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
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class AWSCognitoAuthPluginTest {

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

        verify {
            realPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignUp(expectedUsername, expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                expectedOnSuccess,
                expectedOnError
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

        verify {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResendSignUpCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifySignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signIn(expectedUsername, expectedPassword, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.signIn(
                expectedUsername,
                expectedPassword,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyConfirmSignIn() {
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        verify { realPlugin.confirmSignIn(expectedConfirmationCode, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedConfirmSignIn() {
        val expectedConfirmationCode = "aaab"
        val expectedOptions = AuthConfirmSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifySignInWithSocialWebUI() {
        val expectedProvider = AuthProvider.amazon()
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithSocialWebUI(expectedProvider, expectedActivity, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                expectedOnSuccess,
                expectedOnError
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

        verify {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifySignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signInWithWebUI(expectedActivity, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOptions = AuthWebUISignInOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signInWithWebUI(expectedActivity, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyHandleWebUISignInResponse() {
        val expectedIntent: Intent = mockk()

        authPlugin.handleWebUISignInResponse(expectedIntent)

        verify { realPlugin.handleWebUISignInResponse(expectedIntent) }
    }

    @Test
    fun verifyFetchAuthSession() {
        val expectedOnSuccess = Consumer<AuthSession> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchAuthSession(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchAuthSession(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyRememberDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.rememberDevice(expectedOnSuccess, expectedOnError)

        verify { realPlugin.rememberDevice(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyForgetDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedOnSuccess, expectedOnError)

        verify { realPlugin.forgetDevice(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedForgetDevice() {
        val expectedDevice = AuthDevice.fromId("id2")
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError)

        verify { realPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyFetchDevices() {
        val expectedOnSuccess = Consumer<MutableList<AuthDevice>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchDevices(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchDevices(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyResetPassword() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedResetPassword() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResetPasswordOptions.defaults()
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyConfirmResetPassword() {
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(expectedPassword, expectedCode, expectedOnSuccess, expectedOnError)

        verify { realPlugin.confirmResetPassword(expectedPassword, expectedCode, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedConfirmResetPassword() {
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOptions = AuthConfirmResetPasswordOptions.defaults()
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(
            expectedPassword,
            expectedCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.confirmResetPassword(
                expectedPassword,
                expectedCode,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
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

        verify {
            realPlugin.updatePassword(expectedOldPassword, expectedNewPassword, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyFetchUserAttributes() {
        val expectedOnSuccess = Consumer<MutableList<AuthUserAttribute>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError)

        verify { realPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOptions = AuthUpdateUserAttributeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOnSuccess = Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError)

        verify { realPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOptions = AuthUpdateUserAttributesOptions.defaults()
        val expectedOnSuccess = Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.updateUserAttributes(
                expectedAttributes,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyEscapeHatch() {
        val expectedEscapeHatch = mockk<AWSCognitoAuthServiceBehavior>()
        every { realPlugin.escapeHatch() } returns expectedEscapeHatch

        assertEquals(expectedEscapeHatch, authPlugin.escapeHatch)
    }

    @Test
    fun verifyResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendUserAttributeConfirmationCode(expectedAttributeKey, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                expectedOnSuccess,
                expectedOnError
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

        verify {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
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

        verify {
            realPlugin.confirmUserAttribute(
                expectedAttributeKey,
                expectedConfirmationCode,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyGetCurrentUser() {
        val expectedOnSuccess = Consumer<AuthUser> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.getCurrentUser(expectedOnSuccess, expectedOnError)

        verify { realPlugin.getCurrentUser(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifySignOut() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signOut(expectedOnSuccess, expectedOnError)

        verify { realPlugin.signOut(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignOut() {
        val expectedOnSuccess = Action { }
        val expectedOptions = AuthSignOutOptions.builder().build()
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signOut(expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signOut(expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyDeleteUser() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.deleteUser(expectedOnSuccess, expectedOnError)

        verify { realPlugin.deleteUser(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyPluginKey() {
        assertEquals("awsCognitoAuthPlugin", authPlugin.pluginKey)
    }
}
