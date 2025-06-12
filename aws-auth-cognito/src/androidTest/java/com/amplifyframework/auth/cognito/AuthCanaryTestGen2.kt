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
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.cognito.testutils.callAmplify
import com.amplifyframework.auth.cognito.testutils.invokeAmplify
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class AuthCanaryTestGen2 {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = AuthCanaryTestGen2::class.simpleName
        private val mainThreadSurrogate = newSingleThreadContext("Main thread")
        private val attributes = listOf(
            (AuthUserAttribute(AuthUserAttributeKey.address(), "Sesame Street")),
            (AuthUserAttribute(AuthUserAttributeKey.name(), "Elmo")),
            (AuthUserAttribute(AuthUserAttributeKey.gender(), "Male")),
            (AuthUserAttribute(AuthUserAttributeKey.birthdate(), "2000-02-03")),
            (AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+16268319333")), // Elmo's phone #
            (AuthUserAttribute(AuthUserAttributeKey.updatedAt(), "${System.currentTimeMillis()}"))
        )

        private val api = AWSApiPlugin()
        private val auth = AWSCognitoAuthPlugin()

        @BeforeClass
        @JvmStatic
        fun setup() {
            // Gen2 doesn't support REST API as of v1 schema, so for now we will initialize an API plugin with the
            // Gen1 config json. We can then use this plugin directly instead of calling Amplify.API
            val amplifyConfiguration = AmplifyConfiguration.fromConfigFile(ApplicationProvider.getApplicationContext())
            val apiJson = amplifyConfiguration.forCategoryType(api.categoryType).getPluginConfig(api.pluginKey)
            api.configure(apiJson, ApplicationProvider.getApplicationContext())

            try {
                Amplify.addPlugin(auth)
                Amplify.configure(AmplifyOutputs(R.raw.amplify_outputs), ApplicationProvider.getApplicationContext())
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
    fun signUp() = runTest(timeout = TIMEOUT_S.seconds) {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        val signUpResult = callAmplify { onSuccess, onFailure ->
            signUp(tempUsername, tempPassword, options, onSuccess, onFailure)
        }
        signedUpNewUser = true
    }

    // Test requires confirmation code, testing onError call.
    @Test
    fun confirmSignUp() {
        assertFailsWith<UserNotFoundException> {
            runBlockingWithTimeout(TIMEOUT_S.seconds) {
                val result =
                    callAmplify { onSuccess, onFailure ->
                        confirmSignUp("username", "code", onSuccess, onFailure)
                    }
            }
        }
    }

    @Test
    fun signIn() = runTest(timeout = TIMEOUT_S.seconds) {
        val options = AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.USER_SRP_AUTH).build()
        val signInResult = callAmplify { onSuccess, onFailure ->
            signIn(username, password, options, onSuccess, onFailure)
        }
        assertTrue(signInResult.isSignedIn)
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmSignIn() {
        assertFailsWith<InvalidStateException> {
            runBlockingWithTimeout(TIMEOUT_S.seconds) {
                val result =
                    callAmplify { onSuccess, onFailure ->
                        confirmSignIn("code", onSuccess, onFailure)
                    }
            }
        }
    }

    @Test
    fun fetchAuthSession() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
    }

    @Test
    fun fetchAuthSessionWithRefresh() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val option = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(option, onSuccess, onFailure) }
    }

    @Test
    fun rememberDevice() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        invokeAmplify { onSuccess, onFailure -> rememberDevice(onSuccess, onFailure) }
    }

    @Test
    fun forgetDevice() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        invokeAmplify { onSuccess, onFailure -> forgetDevice(onSuccess, onFailure) }
    }

    @Test
    fun fetchDevices() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val devices = callAmplify { onSuccess, onFailure -> fetchDevices(onSuccess, onFailure) }
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmResetPassword() {
        assertFailsWith<UserNotFoundException> {
            runBlockingWithTimeout(TIMEOUT_S.seconds) {
                invokeAmplify { onSuccess, onFailure ->
                    confirmResetPassword(
                        "username",
                        "NewPassword123",
                        "code",
                        onSuccess,
                        onFailure
                    )
                }
            }
        }
    }

    @Test
    fun updatePassword() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        invokeAmplify { onSuccess, onFailure ->
            updatePassword(tempPassword, tempPassword + "1", onSuccess, onFailure)
        }
    }

    @Test
    fun fetchUserAttributes() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val attributes = callAmplify { onSuccess, onFailure -> fetchUserAttributes(onSuccess, onFailure) }
    }

    @Test
    fun updateUserAttribute() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val attributes = callAmplify { onSuccess, onFailure ->
            updateUserAttribute(AuthUserAttribute(AuthUserAttributeKey.name(), "apitest"), onSuccess, onFailure)
        }
    }

    @Test
    fun updateUserAttributes() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val attributes = callAmplify { onSuccess, onFailure ->
            updateUserAttributes(attributes, onSuccess, onFailure)
        }
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmUserAttribute() {
        assertFailsWith<SignedOutException> {
            runBlockingWithTimeout(TIMEOUT_S.seconds) {
                invokeAmplify { onSuccess, onFailure ->
                    confirmUserAttribute(
                        AuthUserAttributeKey.email(),
                        "344299",
                        onSuccess,
                        onFailure
                    )
                }
            }
        }
    }

    @Test
    fun getCurrentUser() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val user = callAmplify { onSuccess, onFailure ->
            getCurrentUser(onSuccess, onFailure)
        }
    }

    @Test
    fun signOut(): Unit = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val signOutResult = callAmplify { onSuccess, _ -> signOut(onSuccess) }
        when (signOutResult) {
            is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                // Sign Out completed fully and without errors.
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
            else -> fail("Unexpected sign out result occurred")
        }
    }

    @Test
    fun globalSignOut(): Unit = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signInUser(username, password)
        val options = AuthSignOutOptions.builder()
            .globalSignOut(true)
            .build()
        val signOutResult = callAmplify { onSuccess, _ -> signOut(options, onSuccess) }
        when (signOutResult) {
            is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                // Sign Out completed fully and without errors.
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
            else -> fail("Unexpected sign out result occurred")
        }
    }

    @Test
    fun deleteUser() = runBlockingWithTimeout(timeout = TIMEOUT_S.seconds) {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        invokeAmplify { onSuccess, onFailure -> deleteUser(onSuccess, onFailure) }
        signedUpNewUser = false
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
        api.post(
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
        api.post(
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
