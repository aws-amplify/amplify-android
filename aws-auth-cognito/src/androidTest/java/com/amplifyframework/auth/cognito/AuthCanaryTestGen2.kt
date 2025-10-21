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
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.testutils.sync.SynchronousApi
import com.amplifyframework.testutils.sync.SynchronousAuth
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.booleans.shouldBeTrue
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class AuthCanaryTestGen2 {
    companion object {
        private const val TIMEOUT_MS = 20L * 1000
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
        private val syncAuth = SynchronousAuth.delegatingToAmplify(TIMEOUT_MS)
        private val syncApi = SynchronousApi.delegatingTo(api, TIMEOUT_MS)

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
    fun signUp() {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        syncAuth.signUp(tempUsername, tempPassword, options)
    }

    // Test requires confirmation code, testing onError call.
    @Test
    fun confirmSignUp() {
        shouldThrowAny {
            syncAuth.confirmSignUp("username", "the code you received via email")
        }
    }

    @Test
    fun signIn() {
        val options = AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.USER_SRP_AUTH).build()
        val result = syncAuth.signIn(username, password, options)
        result.isSignedIn.shouldBeTrue()
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmSignIn() {
        shouldThrowAny {
            syncAuth.confirmSignIn("confirmation code")
        }
    }

    @Test
    fun fetchAuthSession() {
        signInUser(username, password)
        syncAuth.fetchAuthSession()
    }

    @Test
    fun fetchAuthSessionWithRefresh() {
        signInUser(username, password)
        val option = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        syncAuth.fetchAuthSession(option)
    }

    @Test
    fun rememberDevice() {
        signInUser(username, password)
        syncAuth.rememberDevice()
    }

    @Test
    fun forgetDevice() {
        signInUser(username, password)
        syncAuth.forgetDevice()
    }

    @Test
    fun fetchDevices() {
        signInUser(username, password)
        syncAuth.fetchDevices()
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmResetPassword() {
        shouldThrowAny {
            syncAuth.confirmResetPassword("username", "NewPassword123", "confirmation code")
        }
    }

    @Test
    fun updatePassword() {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        syncAuth.updatePassword(tempPassword, tempPassword + "1")
    }

    @Test
    fun fetchUserAttributes() {
        signInUser(username, password)
        syncAuth.fetchUserAttributes()
    }

    @Test
    fun updateUserAttribute() {
        signInUser(username, password)
        syncAuth.updateUserAttribute(AuthUserAttribute(AuthUserAttributeKey.name(), "apitest"))
    }

    @Test
    fun updateUserAttributes() {
        signInUser(username, password)
        syncAuth.updateUserAttributes(attributes)
    }

    // Test requires confirmation code, testing onError call
    @Test
    fun confirmUserAttribute() {
        shouldThrowAny {
            syncAuth.confirmUserAttribute(AuthUserAttributeKey.email(), "344299")
        }
    }

    @Test
    fun getCurrentUser() {
        signInUser(username, password)
        syncAuth.currentUser
    }

    @Test
    fun signOut() {
        signInUser(username, password)
        val signOutResult = syncAuth.signOut()
        when (signOutResult) {
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

    @Test
    fun globalSignOut() {
        signInUser(username, password)
        val options = AuthSignOutOptions.builder()
            .globalSignOut(true)
            .build()
        val signOutResult = syncAuth.signOut(options)
        when (signOutResult) {
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

    @Test
    fun deleteUser() {
        signUpUser(tempUsername, tempPassword)
        confirmTemporaryUserSignUp(tempUsername)
        signInUser(tempUsername, tempPassword)
        syncAuth.deleteUser()
        signedUpNewUser = false
    }

    private fun signUpUser(user: String, pass: String) {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), "my@email.com")
            .build()
        syncAuth.signUp(user, pass, options)
    }

    private fun signInUser(user: String, pass: String) {
        syncAuth.signIn(user, pass)
    }

    private fun signOutUser() {
        syncAuth.signOut()
    }

    private fun deleteTemporaryUser(user: String) {
        signInUser(username, password)
        val request = RestOptions.builder()
            .addPath("/deleteUser")
            .addBody("{\"username\":\"$user\"}".toByteArray())
            .addHeader("Content-Type", "application/json")
            .build()
        syncApi.post(request)
        signOutUser()
    }

    private fun confirmTemporaryUserSignUp(user: String) {
        signInUser(username, password)
        val request = RestOptions.builder()
            .addPath("/confirmUserSignUp")
            .addBody("{\"username\":\"$user\"}".toByteArray())
            .addHeader("Content-Type", "application/json")
            .build()
        syncApi.post(request)
        signOutUser()
    }
}
