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

package com.amplifyframework.auth.cognito

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

class AuthCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = AuthCanaryTest::class.simpleName
        private val mainThreadSurrogate = newSingleThreadContext("Main thread")
        private val attributes = listOf(
            (AuthUserAttribute(AuthUserAttributeKey.address(), "Sesame Street")),
            (AuthUserAttribute(AuthUserAttributeKey.name(), "Elmo")),
            (AuthUserAttribute(AuthUserAttributeKey.gender(), "Male")),
            (AuthUserAttribute(AuthUserAttributeKey.birthdate(), "February 3")),
            (AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+16268319333")), // Elmo's phone #
            (AuthUserAttribute(AuthUserAttributeKey.updatedAt(), "${System.currentTimeMillis()}"))
        )
        private val auth = AWSCognitoAuthPlugin()

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSApiPlugin())
                Amplify.addPlugin(auth)
                Amplify.configure(ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var tempUsername: String
    private lateinit var tempPassword: String
    private var signedUpNewUser = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun resetAuth() {
        signOutUser()
        val context = ApplicationProvider.getApplicationContext<Context>()
        Dispatchers.setMain(mainThreadSurrogate)
        Credentials.load(context).let {
            username = it.first
            password = it.second
        }
        tempUsername = UUID.randomUUID().toString()
        tempPassword = UUID.randomUUID().toString()
        signedUpNewUser = false
    }

    @After
    fun teardown() {
        if (signedUpNewUser) {
            signedUpNewUser = false
            signOutUser()
            deleteTemporaryUser(tempUsername)
        }
    }

    @Test
    fun signUp() {
        val latch = CountDownLatch(1)
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        Amplify.Auth.signUp(
            tempUsername,
            tempPassword,
            options,
            {
                signedUpNewUser = true
                latch.countDown()
            },
            { fail("Sign up failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call.
    @Test
    fun confirmSignUp() {
        val latch = CountDownLatch(1)
        Amplify.Auth.confirmSignUp(
            "username",
            "the code you received via email",
            { fail("Confirm sign up completed successfully, expected confirm sign up to fail") },
            { latch.countDown() }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Sending sign up confirmation code is disabled in the user pool.")
    fun resendSignUpCode() {
        signUpUser(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.resendSignUpCode(
            tempUsername,
            { latch.countDown() },
            { fail("Failed to confirm sign up: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun signIn() {
        val latch = CountDownLatch(1)
        val options = AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.USER_SRP_AUTH).build()
        Amplify.Auth.signIn(
            username,
            password,
            options,
            { result ->
                if (result.isSignedIn) {
                    latch.countDown()
                } else {
                    fail("Sign in not complete")
                }
            },
            { fail("Failed to sign in: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test will require UI. Implementation is TODO.")
    fun signInWithWebUI() { }

    @Test
    @Ignore("Test will require UI. Implementation is TODO.")
    fun signInWithSocialWebUi() { }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmSignIn() {
        val latch = CountDownLatch(1)
        Amplify.Auth.confirmSignIn(
            "confirmation code",
            { fail("Confirm sign in completed successfully, expected confirm sign in to fail") },
            { latch.countDown() }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchAuthSession() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.fetchAuthSession(
            { latch.countDown() },
            { fail("Failed to fetch session: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchAuthSessionWithRefresh() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        val option = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        Amplify.Auth.fetchAuthSession(
            option,
            { latch.countDown() },
            { fail("Failed to fetch session: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test fails with missing device key error. Ignoring test pending investigation.")
    fun rememberDevice() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.rememberDevice(
            { latch.countDown() },
            { fail("Remember device failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test fails with missing device key error. Ignoring test pending investigation.")
    fun forgetDevice() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.forgetDevice(
            { latch.countDown() },
            { fail("Forget device failed with error: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchDevices() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.fetchDevices(
            { latch.countDown() },
            { fail("Fetch devices failed with error: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test requires a temporary user with a confirmed email.")
    fun resetPassword() {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.resetPassword(
            tempUsername,
            { latch.countDown() },
            { fail("Reset password failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmResetPassword() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.confirmResetPassword(
                "username",
                "NewPassword123",
                "confirmation code",
                { fail("New password confirmed, expected confirm reset password to fail") },
                { latch.countDown() }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun updatePassword() {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.updatePassword(
            tempPassword,
            tempPassword + "1",
            { latch.countDown() },
            { fail("Password update failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchUserAttributes() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.fetchUserAttributes(
            { latch.countDown() },
            { fail("Failed to fetch user attributes: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun updateUserAttribute() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.name(), "apitest"),
            { latch.countDown() },
            { fail("Failed to update user attribute: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun updateUserAttributes() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.updateUserAttributes(
            attributes, // attributes is a list of AuthUserAttribute
            { latch.countDown() },
            { fail("Failed to update user attributes: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmUserAttribute() {
        val latch = CountDownLatch(1)
        Amplify.Auth.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "344299",
            { fail("Confirmed user attribute with incorrect code, expected confirm user attribute to fail.") },
            { latch.countDown() }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test fails when run too frequently due to resend confirmation code limit exceeded.")
    fun resendUserAttributeConfirmationCode() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.resendUserAttributeConfirmationCode(
            AuthUserAttributeKey.email(),
            { latch.countDown() },
            { fail("Failed to resend code: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getCurrentUser() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.getCurrentUser(
            { latch.countDown() },
            { fail("Get current user failed with an exception: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun signOut() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.signOut { signOutResult ->
            when (signOutResult) {
                is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                    // Sign Out completed fully and without errors.
                    latch.countDown()
                }
                is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                    // Sign Out completed with some errors. User is signed out of the device.
                    signOutResult.hostedUIError?.let { fail("HostedUIError while signing out: $it") }
                    signOutResult.globalSignOutError?.let { fail("GlobalSignOutError while signing out: $it") }
                    signOutResult.revokeTokenError?.let { fail("RevokeTokenError: $it") }
                }
                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    // Sign Out failed with an exception, leaving the user signed in.
                    fail("Sign out failed: ${signOutResult.exception}")
                }
            }
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun globalSignOut() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        val options = AuthSignOutOptions.builder()
            .globalSignOut(true)
            .build()
        Amplify.Auth.signOut(options) { signOutResult ->
            when (signOutResult) {
                is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                    // Sign Out completed fully and without errors.
                    latch.countDown()
                }
                is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                    // Sign Out completed with some errors. User is signed out of the device.
                    signOutResult.hostedUIError?.let { fail("HostedUIError while signing out: $it") }
                    signOutResult.globalSignOutError?.let { fail("GlobalSignOutError while signing out: $it") }
                    signOutResult.revokeTokenError?.let { fail("RevokeTokenError: $it") }
                }
                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    // Sign Out failed with an exception, leaving the user signed in.
                    fail("Sign out failed: ${signOutResult.exception}")
                }
            }
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun deleteUser() {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.deleteUser(
            {
                signedUpNewUser = false
                latch.countDown()
            },
            { fail("Delete user failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("OAuth flows not set up.")
    fun testFederateToIdentityPool() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        auth.federateToIdentityPool(
            "YOUR_TOKEN",
            AuthProvider.facebook(),
            { latch.countDown() },
            { fail("Failed to federate to Identity Pool: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("OAuth flows not set up.")
    fun testClearFederateToIdentityPool() {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        auth.clearFederationToIdentityPool(
            { latch.countDown() },
            { fail("Failed to clear federation: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    private fun signUpUser(user: String, pass: String) {
        val latch = CountDownLatch(1)
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        auth.signUp(
            user,
            pass,
            options,
            {
                signedUpNewUser = true
                latch.countDown()
            },
            {
                Log.e(TAG, "Sign up failed", it)
                latch.countDown()
            }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }

    private fun signInUser(user: String, pass: String) {
        val latch = CountDownLatch(1)
        auth.signIn(user, pass, { latch.countDown() }, { latch.countDown() })
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }

    private fun signOutUser() {
        val latch = CountDownLatch(1)
        auth.signOut { latch.countDown() }
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }

    private fun deleteTemporaryUser(user: String) {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        val request = RestOptions.builder()
            .addPath("/deleteUser")
            .addBody("{\"username\":\"$user\"}".toByteArray())
            .addHeader("Content-Type", "application/json")
            .build()
        Amplify.API.post(
            request,
            { latch.countDown() },
            {
                Log.e(TAG, "Error deleting user", it)
                latch.countDown()
            }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
        signOutUser()
    }

    private fun confirmTemporaryUserSignUp(user: String) {
        signInUser(username, password)
        val latch = CountDownLatch(1)
        val request = RestOptions.builder()
            .addPath("/confirmUserSignUp")
            .addBody("{\"username\":\"$user\"}".toByteArray())
            .addHeader("Content-Type", "application/json")
            .build()
        Amplify.API.post(
            request,
            { latch.countDown() },
            {
                Log.e(TAG, "Error confirming user", it)
                latch.countDown()
            }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
        signOutUser()
    }
}
