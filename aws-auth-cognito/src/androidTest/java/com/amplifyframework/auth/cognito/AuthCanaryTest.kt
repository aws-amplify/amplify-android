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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.testutils.Resources
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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
        val attributes = listOf(
            (AuthUserAttribute(AuthUserAttributeKey.address(), "Sesame Street")),
            (AuthUserAttribute(AuthUserAttributeKey.name(), "Elmo")),
            (AuthUserAttribute(AuthUserAttributeKey.gender(), "Male")),
            (AuthUserAttribute(AuthUserAttributeKey.birthdate(), "February 3")),
            (AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+16268319333")), // Elmo's phone #
            (AuthUserAttribute(AuthUserAttributeKey.updatedAt(), "${System.currentTimeMillis()}"))
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(ApplicationProvider.getApplicationContext())
                Log.i(TAG, "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var tempUsername: String
    private lateinit var tempPassword: String
    private var createdNewUser = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun resetAuth() {
        val latch = CountDownLatch(1)
        Amplify.Auth.signOut {
            latch.countDown()
        }
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
        val context = ApplicationProvider.getApplicationContext<Context>()
        Dispatchers.setMain(mainThreadSurrogate)
        Credentials.load(context).let {
            username = it.first
            password = it.second
        }
        tempUsername = UUID.randomUUID().toString()
        tempPassword = UUID.randomUUID().toString()
        createdNewUser = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun cleanUp() {
        if (createdNewUser) {
            val configJson = Resources.readAsJson(
                ApplicationProvider.getApplicationContext(),
                R.raw.amplifyconfiguration
            )
                .getJSONObject("auth")
                .getJSONObject("plugins")
                .getJSONObject("awsCognitoAuthPlugin")
            val userPoolId = AuthConfiguration.fromJson(configJson).userPool!!.poolId!!
            // Delete the temporary user that was created
            val deleteUserRequest = AdminDeleteUserRequest {
                this.username = tempUsername
                this.userPoolId = userPoolId
            }
            val cognitoAuthPlugin = Amplify.Auth.getPlugin("awsCognitoAuthPlugin")
            val cognitoAuthService = cognitoAuthPlugin.escapeHatch as AWSCognitoAuthService
            val cognitoIdentityProviderClient = cognitoAuthService.cognitoIdentityProviderClient
            TestScope().runTest {
                cognitoIdentityProviderClient?.adminDeleteUser(deleteUserRequest)
            }
        }
    }

    @Test
    fun signUp() {
        val latch = CountDownLatch(1)
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        try {
            Amplify.Auth.signUp(
                tempUsername,
                tempPassword,
                options,
                {
                    createdNewUser = true
                    Log.i(TAG, "Sign up succeeded: $it")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Sign up failed", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call.
    @Test
    fun confirmSignUp() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.confirmSignUp(
                "username",
                "the code you received via email",
                { result ->
                    Log.i(TAG, "Confirm signUp result completed: ${result.isSignUpComplete}")
                    fail()
                },
                {
                    Log.e(TAG, "Failed to confirm sign up", it)
                    latch.countDown()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun resendSignUpCode() {
        signUpBeforeTest(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.resendSignUpCode(
                tempUsername,
                {
                    Log.i(TAG, "Resend sign up code succeeded")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to confirm sign up", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun signIn() {
        val latch = CountDownLatch(1)
        val options = AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.USER_SRP_AUTH).build()
        try {
            Amplify.Auth.signIn(
                username,
                password,
                options,
                { result ->
                    if (result.isSignedIn) {
                        Log.i(TAG, "Sign in succeeded")
                        latch.countDown()
                    } else {
                        Log.i(TAG, "Sign in not complete")
                        fail()
                    }
                },
                {
                    Log.e(TAG, "Failed to sign in", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
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
        try {
            Amplify.Auth.confirmSignIn(
                "confirmation code",
                { result ->
                    Log.i(TAG, "Confirm signIn result completed: ${result.isSignedIn}")
                    fail()
                },
                {
                    Log.e(TAG, "Confirm sign in failed", it)
                    latch.countDown()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchAuthSession() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.fetchAuthSession(
                {
                    val session = it as AWSCognitoAuthSession
                    when (session.identityIdResult.type) {
                        AuthSessionResult.Type.SUCCESS ->
                            Log.i(TAG, "IdentityId = ${session.identityIdResult.value}")
                        AuthSessionResult.Type.FAILURE ->
                            Log.w(TAG, "IdentityId not found", session.identityIdResult.error)
                    }
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to fetch session", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchAuthSessionWithRefresh() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        val option = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        try {
            Amplify.Auth.fetchAuthSession(
                option,
                {
                    val session = it as AWSCognitoAuthSession
                    when (session.identityIdResult.type) {
                        AuthSessionResult.Type.SUCCESS ->
                            Log.i(TAG, "IdentityId = ${session.identityIdResult.value}")
                        AuthSessionResult.Type.FAILURE ->
                            Log.w(TAG, "IdentityId not found", session.identityIdResult.error)
                    }
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to fetch session", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test fails with missing device key error. Ignoring test pending investigation.")
    fun rememberDevice() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.rememberDevice(
                {
                    Log.i(TAG, "Remember device succeeded")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Remember device failed with error", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test fails with missing device key error. Ignoring test pending investigation.")
    fun forgetDevice() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.forgetDevice(
                {
                    Log.i(TAG, "Forget device succeeded")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Forget device failed with error", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchDevices() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.fetchDevices(
                { devices ->
                    devices.forEach { Log.i(TAG, "Device: $it") }
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Fetch devices failed with error", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Test requires a tempUser with a confirmed email.")
    fun resetPassword() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        Amplify.Auth.resetPassword(
            username,
            {
                Log.i(TAG, "Reset password succeeded")
                latch.countDown()
            },
            {
                Log.e(TAG, "Reset password failed", it)
                fail()
            }
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
                {
                    Log.i(TAG, "New password confirmed")
                    fail()
                },
                {
                    Log.e(TAG, "Failed to confirm password reset", it)
                    latch.countDown()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Need to sign up a tempUser and signIn, which requires confirmation.")
    fun updatePassword() {
        signUpBeforeTest(tempUsername, tempPassword)
        signInBeforeTest(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.updatePassword(
            tempPassword,
            tempPassword + "1",
            {
                Log.i(TAG, "Updated password successfully")
                latch.countDown()
            },
            {
                Log.e(TAG, "Password update failed", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun fetchUserAttributes() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.fetchUserAttributes(
                {
                    Log.i(TAG, "User attributes = $attributes")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to fetch user attributes", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun updateUserAttribute() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.updateUserAttribute(
                AuthUserAttribute(AuthUserAttributeKey.name(), "apitest"),
                {
                    Log.i(TAG, "Updated user attribute = $it")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to update user attribute.", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun updateUserAttributeWithConfirmationCode() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.confirmUserAttribute(
                AuthUserAttributeKey.email(),
                "344299",
                {
                    Log.i(TAG, "Confirmed user attribute with correct code.")
                    fail()
                },
                {
                    Log.e(TAG, "Failed to confirm user attribute. Bad code?", it)
                    latch.countDown()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun updateUserAttributes() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.updateUserAttributes(
                attributes, // attributes is a list of AuthUserAttribute
                {
                    Log.i(TAG, "Updated user attributes = $it")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to update user attributes", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmUserAttribute() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.confirmUserAttribute(
                AuthUserAttributeKey.email(),
                "344299",
                {
                    Log.i(TAG, "Confirmed user attribute with correct code.")
                    fail()
                },
                {
                    Log.e(TAG, "Failed to confirm user attribute. Bad code?", it)
                    latch.countDown()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun resendUserAttributeConfirmationCode() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.resendUserAttributeConfirmationCode(
                AuthUserAttributeKey.email(),
                {
                    Log.i(TAG, "Code was sent again: $it")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to resend code", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getCurrentUser() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.getCurrentUser(
                {
                    Log.i(TAG, "Current user details are: $it")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "getCurrentUser failed with an exception: $it")
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun signOut() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        try {
            Amplify.Auth.signOut {
                Log.i(TAG, "Signed out successfully")
                latch.countDown()
            }
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun globalSignOut() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        val options = AuthSignOutOptions.builder()
            .globalSignOut(true)
            .build()
        try {
            Amplify.Auth.signOut(options) {
                Log.i(TAG, "Signed out successfully")
                latch.countDown()
            }
        } catch (e: Exception) {
            fail(e.toString())
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("Need to sign up a tempUser and signIn, which requires confirmation.")
    fun deleteUser() {
        signUpBeforeTest(tempUsername, tempPassword)
        Thread.sleep(1000)
        signInBeforeTest(tempUsername, tempPassword)
        val latch = CountDownLatch(1)
        Amplify.Auth.deleteUser(
            {
                Log.i(TAG, "Delete user succeeded")
                latch.countDown()
            },
            {
                Log.e(TAG, "Delete user failed with error", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("OAuth flows not set up.")
    fun testFederateToIdentityPool() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        (Amplify.Auth.getPlugin("awsCognitoAuthPlugin") as? AWSCognitoAuthPlugin)?.let { plugin ->
            plugin.federateToIdentityPool(
                "YOUR_TOKEN",
                AuthProvider.facebook(),
                {
                    Log.i(TAG, "Successful federation to Identity Pool.")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to federate to Identity Pool.", it)
                    fail()
                }
            )
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    @Ignore("OAuth flows not set up.")
    fun testClearFederateToIdentityPool() {
        signInBeforeTest(username, password)
        val latch = CountDownLatch(1)
        (Amplify.Auth.getPlugin("awsCognitoAuthPlugin") as? AWSCognitoAuthPlugin)?.let { plugin ->
            plugin.clearFederationToIdentityPool(
                {
                    Log.i(TAG, "Federation cleared successfully.")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to clear federation.", it)
                    fail()
                }
            )
        }
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    private fun signUpBeforeTest(user: String, pass: String) {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        Amplify.Auth.signUp(
            user,
            pass,
            options,
            {
                createdNewUser = true
                Log.i(TAG, "Sign up succeeded: $it")
            },
            { Log.e(TAG, "Sign up failed", it) }
        )
    }

    private fun signInBeforeTest(user: String, pass: String) {
        Amplify.Auth.signIn(
            user,
            pass,
            { result ->
                if (result.isSignedIn) {
                    Log.i("AuthQuickstart", "Sign in succeeded")
                } else {
                    Log.i("AuthQuickstart", "Sign in not complete")
                }
            },
            { Log.e("AuthQuickstart", "Failed to sign in", it) }
        )
    }
}
