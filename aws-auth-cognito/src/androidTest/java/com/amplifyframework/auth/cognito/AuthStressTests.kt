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
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.cognito.testutils.ResultFunction
import com.amplifyframework.auth.cognito.testutils.ResultFunctionWithArg
import com.amplifyframework.auth.cognito.testutils.blockForResult
import com.amplifyframework.auth.cognito.testutils.deferredResult
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Amplify
import io.kotest.inspectors.forAll
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        blockForResult(TIMEOUT_S.seconds) { onSuccess, _ -> Amplify.Auth.signOut(onSuccess) }
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
    fun testMultipleSignIn() = runStressTest {
        val results = callConcurrently(50) { onSuccess, _ ->
            Amplify.Auth.signIn(username, password, onSuccess, onSuccess) // errors are expected
        }.awaitAll()

        results.filterIsInstance<AuthSignInResult>().shouldHaveSingleElement { it.isSignedIn }
        results.filterIsInstance<SignedInException>().shouldHaveSize(49)
    }

    /**
     * Calls Auth.signOut 50 times
     */
    @Test
    fun testMultipleSignOut() = runStressTest {
        val results = callConcurrently(50) { onSuccess, _ ->
            Amplify.Auth.signOut(onSuccess)
        }.awaitAll()

        results.filterIsInstance<AWSCognitoAuthSignOutResult>()
            .shouldHaveSize(50).forAll { it.signedOutLocally.shouldBeTrue() }
    }

    /**
     * Calls Auth.fetchAuthSession 100 times when signed out
     */
    @Test
    fun testMultipleFAS_WhenSignedOut() = runStressTest {
        val results = callConcurrently(100) { onSuccess, onError ->
            Amplify.Auth.fetchAuthSession(onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(100).forAll { it.isSignedIn.shouldBeFalse() }
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times
     */
    @Test
    fun testMultipleFAS_AfterSignIn() = runStressTest {
        signIn()

        val results = callConcurrently(100) { onSuccess, onError ->
            Amplify.Auth.fetchAuthSession(onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(100).forAll { it.isSignedIn.shouldBeTrue() }
    }

    /**
     * Calls Auth.signIn, then calls Auth.signOut
     */
    @Test
    fun testSignOut_AfterSignIn() = runStressTest {
        signIn()
        signOut()
    }

    /**
     * Calls Auth.signIn, calls Auth.fetchAuthSession 100 times, then calls Auth.signOut
     */
    @Test
    fun testSignIn_multipleFAS_SignOut() = runStressTest {
        signIn()

        val results = callConcurrently(100) { onSuccess, onError ->
            Amplify.Auth.fetchAuthSession(onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(100)

        signOut()
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.signOut within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withSignOut() = runStressTest {
        signIn()

        val deferred = mutableListOf<Deferred<Any>>()
        val random = (Math.random() * 100).toInt()
        repeat(100) { idx ->
            if (idx == random) {
                deferred += deferredResult { onSuccess, _ -> Amplify.Auth.signOut(onSuccess) }
            }

            deferred += deferredResult { onSuccess, onError -> Amplify.Auth.fetchAuthSession(onSuccess, onError) }
        }

        val results = deferred.awaitAll()
        results.filterIsInstance<AuthSession>().shouldHaveSize(100)
        results.filterIsInstance<AWSCognitoAuthSignOutResult>().shouldHaveSingleElement { it.signedOutLocally }
    }

    /**
     * Calls Auth.signIn, then calls Auth.fetchAuthSession 100 times. Randomly calls Auth.fetchAuthSession with
     * forceRefresh() within those 100 calls.
     */
    @Test
    fun testSignIn_multipleFAS_withRefresh() = runStressTest {
        signIn()

        val random = List(2) { (Math.random() * 100).toInt() }

        val results = callConcurrentlyIndexed(100) { idx, onSuccess, onError ->
            val options = AuthFetchSessionOptions.builder().forceRefresh(random.contains(idx)).build()
            Amplify.Auth.fetchAuthSession(options, onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(100)
    }

    /**
     * Randomly calls Auth.fetchAuthSession, Auth.signIn, Auth.fetchAuthSession with forceRefresh(), and Auth.signOut 20 times.
     */
    @Test
    fun testRandomMultipleAPIs() = runStressTest(timeout = 30.seconds) {
        val results = callConcurrently(20) { onSuccess, onError ->
            val randomizer = (1..4).random()
            when (randomizer) {
                1 -> Amplify.Auth.fetchAuthSession(onSuccess, onError)
                2 -> Amplify.Auth.signIn(username, password, onSuccess, onSuccess) // SignedInException is expected
                3 -> {
                    val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
                    Amplify.Auth.fetchAuthSession(options, onSuccess, onError)
                }
                4 -> Amplify.Auth.signOut(onSuccess)
            }
        }.awaitAll()

        results.shouldHaveSize(20)
        results.filterIsInstance<AWSCognitoAuthSignOutResult>().forAll { it.signedOutLocally.shouldBeTrue() }
        results.filterIsInstance<AuthSignInResult>().forAll { it.isSignedIn.shouldBeTrue() }
    }

    /**
     * Calls Auth.getCurrentUser 100 times
     */
    @Test
    fun testSignIn_GetCurrentUser() = runStressTest {
        signIn()

        val results = callConcurrently(100) { onSuccess, onError ->
            Amplify.Auth.getCurrentUser(onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(100).forAll { it.username shouldBe username }
    }

    /**
     * Calls Auth.fetchUserAttributes 25 times
     * Each of these calls  always makes a network request so they take much longer
     */
    @Test
    fun testSignIn_FetchAttributes() = runStressTest(timeout = 2.minutes) {
        signIn()

        val results = callConcurrently(25) { onSuccess, onError ->
            Amplify.Auth.fetchUserAttributes(onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(25)
    }

    /**
     * Calls Auth.updateUserAttributes 25 times
     * Each of these calls always makes a network request so they take much longer
     */
    @Test
    fun testSignIn_UpdateAttributes() = runStressTest(timeout = 2.minutes) {
        signIn()

        val results = callConcurrently(25) { onSuccess, onError ->
            Amplify.Auth.updateUserAttributes(attributes, onSuccess, onError)
        }.awaitAll()

        results.shouldHaveSize(25)
    }

    private fun runStressTest(timeout: Duration = TIMEOUT_S.seconds, body: suspend () -> Unit) = runBlocking {
        withTimeout(timeout) {
            body()
        }
    }

    private fun <T> callConcurrently(times: Int, func: ResultFunction<T>): List<Deferred<T>> = buildList {
        repeat(times) {
            add(deferredResult(func))
        }
    }

    private fun <T> callConcurrentlyIndexed(times: Int, func: ResultFunctionWithArg<Int, T>): List<Deferred<T>> =
        buildList {
            repeat(times) { i ->
                add(deferredResult(i, func))
            }
        }

    private suspend fun signIn() {
        val result = deferredResult { onSuccess, onError ->
            Amplify.Auth.signIn(username, password, onSuccess, onError)
        }.await()
        result.isSignedIn.shouldBeTrue()
    }

    private suspend fun signOut() {
        val result = deferredResult { onSuccess, onError ->
            Amplify.Auth.signOut(onSuccess)
        }.await()
        (result as AWSCognitoAuthSignOutResult).signedOutLocally.shouldBeTrue()
    }
}
