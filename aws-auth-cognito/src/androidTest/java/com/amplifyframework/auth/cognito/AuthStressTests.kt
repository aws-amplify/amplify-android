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

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.cognito.testutils.callAmplify
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class AuthStressTests {
    companion object {
        private const val TIMEOUT_S = 20L
        val attributes = listOf(
            (AuthUserAttribute(AuthUserAttributeKey.address(), "Sesame Street")),
            (AuthUserAttribute(AuthUserAttributeKey.name(), "Elmo")),
            (AuthUserAttribute(AuthUserAttributeKey.gender(), "Male")),
            (AuthUserAttribute(AuthUserAttributeKey.birthdate(), "2000-02-03")),
            (AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+16268319333")),
            (AuthUserAttribute(AuthUserAttributeKey.updatedAt(), "${System.currentTimeMillis()}"))
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(ApplicationProvider.getApplicationContext())
                Log.i("MyAmplifyApp", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("MyAmplifyApp", "Could not initialize Amplify", error)
            }
        }
    }

    lateinit var username: String
    lateinit var password: String

    @Before
    fun resetAuth() {
        val latch = CountDownLatch(1)
        Amplify.Auth.signOut {
            latch.countDown()
        }
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
        val context = ApplicationProvider.getApplicationContext<Context>()
        Credentials.load(context).let {
            username = it.first
            password = it.second
        }
    }

    /**
     * Calls Auth.signIn 50 times
     */
    @Test
    fun testMultipleSignIn() = runStressTest(
        times = 50,
        timeout = 20.seconds,
        setup = { signInUser(username, password) }
    ) {
        assertFailsWith<SignedInException> {
            val result = callAmplify { onSuccess, onFailure -> signIn(username, password, onSuccess, onFailure) }
        }
    }

    /**
     * Calls Auth.signOut 50 times
     */
    @Test
    fun testMultipleSignOut() = runStressTest(
        times = 50
    ) {
        signOutUser()
    }

    /**
     * Calls Auth.fetchAuthSession 100 times when signed out
     */
    @Test
    fun testMultipleFAS_WhenSignedOut() = runStressTest(
        times = 100,
        timeout = 20.seconds
    ) {
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times
     */
    @Test
    fun testMultipleFAS_AfterSignIn() = runStressTest(
        times = 100,
        timeout = 1.minutes,
        setup = { signInUser(username, password) }
    ) {
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
    }

    /**
     * Calls Auth.signIn, then calls Auth.signOut
     */
    @Test
    fun testSignOut_AfterSignIn() = runStressTest(
        times = 0,
        timeout = 1.minutes,
        setup = { signInUser(username, password) },
        finally = { signOutUser() }
    ) { }

    /**
     * Calls Auth.signIn, calls Auth.fetchAuthSession 100 times, then calls Auth.signOut
     */
    @Test
    fun testSignIn_multipleFAS_SignOut() = runStressTest(
        times = 100,
        timeout = 1.minutes,
        setup = { signInUser(username, password) },
        finally = { signOutUser() }
    ) {
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.signOut within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withSignOut() = runStressTest(
        times = 100,
        timeout = 2.minutes,
        setup = { signInUser(username, password) }
    ) {
        val random = (Math.random() * 100).toInt()
        repeat(100) { idx ->
            if (idx == random) {
                signOutUser()
            }

            val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
        }
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.fetchAuthSession with
     * forceRefresh() within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withRefresh() = runStressTest(
        times = 100,
        timeout = 2.minutes,
        setup = { signInUser(username, password) }
    ) {
        val options = AuthFetchSessionOptions.builder().forceRefresh(Random.nextInt(2) == 0).build()
        val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(options, onSuccess, onFailure) }
        assertTrue(session.isSignedIn)
    }

    /**
     * Randomly calls Auth.fetchAuthSession, Auth.signIn, Auth.fetchAuthSession with forceRefresh(), and Auth.signOut 20 times.
     */
    @Test
    fun testRandomMultipleAPIs() = runBlockingWithTimeout(timeout = 1.minutes) {
        val random = List(20) { (1..4).random() }
        random.forEach { idx ->
            when (idx) {
                1 -> {
                    val session = callAmplify { onSuccess, onFailure -> fetchAuthSession(onSuccess, onFailure) }
                }
                2 -> {
                    try {
                        val result =
                            callAmplify { onSuccess, onFailure -> signIn(username, password, onSuccess, onFailure) }
                    } catch (exception: SignedInException) {
                        // catch and ignore this exception as it's expected if sign in is called twice without a sign out
                    }
                }
                3 -> {
                    val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
                    val session = callAmplify { onSuccess, onFailure ->
                        fetchAuthSession(options, onSuccess, onFailure)
                    }
                }
                4 -> {
                    signOutUser()
                }
            }
        }
    }

    /**
     * Calls Auth.getCurrentUser 100 times
     */
    @Test
    fun testSignIn_GetCurrentUser() = runStressTest(
        times = 100,
        timeout = 1.minutes,
        setup = { signInUser(username, password) }
    ) {
        val user = callAmplify { onSuccess, onFailure -> getCurrentUser(onSuccess, onFailure) }
    }

    /**
     * Calls Auth.fetchUserAttributes 100 times
     */
    @Test
    fun testSignIn_FetchAttributes() = runStressTest(
        times = 100,
        timeout = 1.minutes,
        setup = { signInUser(username, password) }
    ) {
        val attributes = callAmplify { onSuccess, onFailure -> fetchUserAttributes(onSuccess, onFailure) }
    }

    /**
     * Calls Auth.updateUserAttributes 100 times
     */
    @Test
    fun testSignIn_UpdateAttributes() = runStressTest(
        times = 100,
        timeout = 2.minutes,
        setup = { signInUser(username, password) }
    ) {
        val updated = callAmplify { onSuccess, onFailure -> updateUserAttributes(attributes, onSuccess, onFailure) }
    }

    private fun runStressTest(
        times: Int = 50,
        timeout: Duration = 10.seconds,
        setup: suspend () -> Unit = {},
        finally: suspend () -> Unit = {},
        body: suspend (Int) -> Unit
    ) {
        try {
            runBlockingWithTimeout(timeout) {
                setup()
                repeat(times) {
                    // This delay is to try and slow the amount of requests per second we make without inflating the
                    // test execution time by too much. If we still find that these tests are flaky, remove this
                    // and investigate further
                    delay(20)
                    body(it)
                }
                finally()
            }
        } catch (e: Exception) {
            fail("Test failed with exception: $e")
        }
    }
}

private suspend fun signInUser(username: String, password: String) {
    val result = callAmplify { onSuccess, onFailure -> signIn(username, password, onSuccess, onFailure) }
    assertTrue(result.isSignedIn)
}

private suspend fun signOutUser() {
    val signedOut = callAmplify { onSuccess, _ -> signOut(onSuccess) }
    assertTrue((signedOut as AWSCognitoAuthSignOutResult).signedOutLocally)
}
