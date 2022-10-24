package com.amplifyframework.auth.cognito

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AuthStressTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.VERBOSE))
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(ApplicationProvider.getApplicationContext())
                Log.i("MyAmplifyApp", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("MyAmplifyApp", "Could not initialize Amplify", error)
            }
            Thread.sleep(5000)
        }
    }

    private fun resetAuth() {
        Amplify.Auth.signOut { }
        Thread.sleep(5000)
    }

    @Test
    fun testMultipleSignIn() {
        resetAuth()
        val latch = CountDownLatch(50)

        (1..50).forEach { _ ->
            Amplify.Auth.signIn("username2", "User@123",
                {
                    if (it.isSignedIn) latch.countDown()
                }, {
                    fail()
                }
            )
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testMultipleSignOut() {
        val latch = CountDownLatch(50)

        (1..50).forEach { _ ->
            Amplify.Auth.signOut {
                if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown()
                println("signed out $it")
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testMultipleFAS_WhenSignedOut() {
        resetAuth()
        val latch = CountDownLatch(100)

        (1..100).forEach { _ ->
            Amplify.Auth.fetchAuthSession(
                {
                    if (!it.isSignedIn) latch.countDown()
                    println("session fetched $it")
                },
                {
                    println("session error $it")
                    fail()
                }
            )
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testMultipleFAS_AfterSignIn() {
        resetAuth()
        val latch = CountDownLatch(101)

        Amplify.Auth.signIn("username2", "User@123",
            {
                if (it.isSignedIn) latch.countDown()
                println("signed in $it")
            }, {
                println("sign in error $it")
                fail()
            }
        )

        (1..100).forEach { _ ->
            Amplify.Auth.fetchAuthSession(
                {
                    if (it.isSignedIn) latch.countDown()
                    println("session fetched $it")
                },
                {
                    println("session error $it")
                    fail()
                }
            )
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testSignOut_AfterSignIn() {
        resetAuth()
        val latch = CountDownLatch(2)

        Amplify.Auth.signIn("username2", "User@123",
            {
                println("signed in $it")
                fail()
            }, {
                println("sign in error $it")
                latch.countDown()
            }
        )

        Amplify.Auth.signOut {
            println("signed out $it")
            if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) latch.countDown()
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testSignIn_multipleFAS_SignOut() {
        resetAuth()
        val latch = CountDownLatch(100)
        val signInOutLatch = CountDownLatch(2)

        Amplify.Auth.signIn("username2", "User@123",
            {
                println("signed in $it")
                if (it.isSignedIn) signInOutLatch.countDown()
            }, {
                println("sign in error $it")
            }
        )

        (1..100).forEach { _ ->
            Amplify.Auth.fetchAuthSession(
                {
                    println("session fetched $it")
                    latch.countDown()
                },
                {
                    println("session error $it")
                    fail()
                }
            )
        }

        Amplify.Auth.signOut {
            println("signed out $it")
            if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) signInOutLatch.countDown()
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertTrue(signInOutLatch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testSignIn_multipleFAS_withSignOut() {
        resetAuth()
        val latch = CountDownLatch(100)
        val signInOutLatch = CountDownLatch(2)

        Amplify.Auth.signIn("username2", "User@123",
            {
                println("signed in $it")
                if (it.isSignedIn) signInOutLatch.countDown()
            }, {
                println("sign in error $it")
            }
        )

        val random = (Math.random() * 100).toInt()
        (1..100).forEach { idx ->
            if (idx == random) {
                println("send sign out @ $idx")
                Amplify.Auth.signOut {
                    println("signed out $it")
                    if ((it as AWSCognitoAuthSignOutResult).signedOutLocally) signInOutLatch.countDown()
                }
            }

            Amplify.Auth.fetchAuthSession(
                {
                    println("session fetched $it")
                    latch.countDown()
                },
                {
                    println("session error $it")
                    fail()
                }
            )
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertTrue(signInOutLatch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testSignIn_multipleFAS_withRefresh() {
        resetAuth()
        val latch = CountDownLatch(102)

        Amplify.Auth.signIn("username2", "User@123",
            {
                println("signed in $it")
                if (it.isSignedIn) latch.countDown()
            }, {
                println("sign in error $it")
                fail()
            }
        )

        val random = (Math.random() * 100).toInt()
        (1..100).forEach { idx ->
            if (idx == random) {
                println("session refresh @ $idx")
                val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
                Amplify.Auth.fetchAuthSession(options,
                    {
                        println("session refreshed $it")
                        latch.countDown()
                    },
                    {
                        println("session refresh error $it")
                        fail()
                    }
                )
            }

            Amplify.Auth.fetchAuthSession(
                {
                    println("session fetched $it")
                    latch.countDown()
                },
                {
                    println("session error $it")
                    fail()
                }
            )
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }
}