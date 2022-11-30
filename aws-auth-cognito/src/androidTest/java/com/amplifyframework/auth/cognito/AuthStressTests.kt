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
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.core.Amplify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
            (AuthUserAttribute(AuthUserAttributeKey.birthdate(), "February 3")),
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
            Thread.sleep(5000)
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
    fun testMultipleSignIn() {
        val successLatch = CountDownLatch(1)
        val errorLatch = CountDownLatch(49)

        repeat(50) {
            Amplify.Auth.signIn(
                username,
                password,
                { if (it.isSignedIn) successLatch.countDown() else fail() },
                { errorLatch.countDown() }
            )
        }

        assertTrue(successLatch.await(TIMEOUT_S, TimeUnit.SECONDS))
        assertTrue(errorLatch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signOut 50 times
     */
    @Test
    fun testMultipleSignOut() {
        val latch = CountDownLatch(50)

        repeat(50) {
            Amplify.Auth.signOut {
                if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown() else fail()
            }
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.fetchAuthSession 100 times when signed out
     */
    @Test
    fun testMultipleFAS_WhenSignedOut() {
        val latch = CountDownLatch(100)

        repeat(100) {
            Amplify.Auth.fetchAuthSession({ if (!it.isSignedIn) latch.countDown() else fail() }, { fail() })
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times
     */
    @Test
    fun testMultipleFAS_AfterSignIn() {
        val latch = CountDownLatch(101)

        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        repeat(100) {
            Amplify.Auth.fetchAuthSession({ if (it.isSignedIn) latch.countDown() else fail() }, { fail() })
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signIn, then calls Auth.signOut
     */
    @Test
    fun testSignOut_AfterSignIn() {
        val latch = CountDownLatch(2)

        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        Amplify.Auth.signOut { if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown() else fail() }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signIn, calls Auth.fetchAuthSession 100 times, then calls Auth.signOut
     */
    @Test
    fun testSignIn_multipleFAS_SignOut() {
        val latch = CountDownLatch(102)

        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        repeat(100) {
            Amplify.Auth.fetchAuthSession({ latch.countDown() }, { fail() })
        }

        Amplify.Auth.signOut { if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown() else fail() }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.signOut within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withSignOut() {
        val latch = CountDownLatch(102)

        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        val random = (Math.random() * 100).toInt()
        repeat(100) { idx ->
            if (idx == random) {
                Amplify.Auth.signOut {
                    if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown() else fail()
                }
            }

            Amplify.Auth.fetchAuthSession({ latch.countDown() }, { fail() })
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.fetchAuthSession with
     * forceRefresh() within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withRefresh() {
        val latch = CountDownLatch(101)

        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() },
            { fail() }
        )

        val random = List(2) { (Math.random() * 100).toInt() }
        repeat(100) { idx ->
            val options = AuthFetchSessionOptions.builder().forceRefresh(random.contains(idx)).build()

            Amplify.Auth.fetchAuthSession(
                options,
                { if (it.isSignedIn) latch.countDown() else fail() },
                { fail() }
            )
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.MINUTES))
    }

    /**
     * Randomly calls Auth.fetchAuthSession, Auth.signIn, Auth.fetchAuthSession with forceRefresh(), and Auth.signOut 20 times.
     */
    @Test
    fun testRandomMultipleAPIs() {
        val latch = CountDownLatch(20)

        val random = List(20) { (1..4).random() }
        random.forEach { idx ->
            when (idx) {
                1 -> Amplify.Auth.fetchAuthSession({ latch.countDown() }, { fail() })
                2 -> {
                    Amplify.Auth.signIn(
                        username,
                        password,
                        { if (it.isSignedIn) latch.countDown() },
                        { latch.countDown() }
                    )
                }
                3 -> {
                    val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
                    Amplify.Auth.fetchAuthSession(options, { latch.countDown() }, { fail() })
                }
                4 -> Amplify.Auth.signOut {
                    if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown() else fail()
                }
            }
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.MINUTES))
    }

    /**
     * Calls Auth.getCurrentUser 100 times
     */
    @Test
    fun testSignIn_GetCurrentUser() {
        val latch = CountDownLatch(101)
        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        repeat(100) {
            Amplify.Auth.getCurrentUser(
                { if (it.username == username) latch.countDown() else fail() },
                { fail() }
            )
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.MINUTES))
    }

    /**
     * Calls Auth.fetchUserAttributes 100 times
     */
    @Test
    fun testSignIn_FetchAttributes() {
        val latch = CountDownLatch(101)
        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        repeat(100) {
            Amplify.Auth.fetchUserAttributes(
                { latch.countDown() },
                { fail() }
            )
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.MINUTES))
    }

    /**
     * Calls Auth.updateUserAttributes 100 times
     */
    @Test
    fun testSignIn_UpdateAttributes() {
        val latch = CountDownLatch(101)
        Amplify.Auth.signIn(
            username,
            password,
            { if (it.isSignedIn) latch.countDown() else fail() },
            { fail() }
        )

        repeat(100) {
            Amplify.Auth.updateUserAttributes(
                attributes,
                { latch.countDown() },
                { fail() }
            )
        }

        assertTrue(latch.await(TIMEOUT_S, TimeUnit.MINUTES))
    }
}
