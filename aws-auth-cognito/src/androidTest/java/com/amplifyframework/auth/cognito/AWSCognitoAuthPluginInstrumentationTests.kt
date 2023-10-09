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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.HubAccumulator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoAuthPluginInstrumentationTests {

    companion object {
        var auth = AWSCognitoAuthPlugin()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            try {
                Amplify.addPlugin(auth)
                Amplify.configure(ApplicationProvider.getApplicationContext())
                // Wait for auth to be initialized
                val latch = CountDownLatch(1)
                Amplify.Hub.subscribe(HubChannel.AUTH) { event ->
                    when (event.name) {
                        InitializationStatus.SUCCEEDED.toString(),
                        InitializationStatus.FAILED.toString() ->
                            latch.countDown()
                    }
                }
                latch.await(20, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                Log.i("AWSCognitoAuthPluginInstrumentationTests", "Error initializing", ex)
            }
        }
    }

    @After
    fun tearDown() {
        signOut()
    }

    @Test
    fun signed_In_Hub_Event_Is_Published_When_Signed_In() {
        val hubAccumulator = HubAccumulator.create(HubChannel.AUTH, AuthChannelEventName.SIGNED_IN, 1).start()

        signInWithCognito()

        hubAccumulator.await(10, TimeUnit.SECONDS)
        // if we made it this far without timeout, it means hub event was received
    }

    @Test
    fun signed_Out_Hub_Event_Is_Published_When_Signed_Out() {
        val hubAccumulator = HubAccumulator.create(HubChannel.AUTH, AuthChannelEventName.SIGNED_OUT, 1).start()

        signInWithCognito()
        signOut()

        hubAccumulator.await(10, TimeUnit.SECONDS)
        // if we made it this far without timeout, it means hub event was received
    }

    @Test
    fun hub_Events_Are_Received_Only_Once_Per_Change() {
        val signInAccumulator = HubAccumulator
            .create(HubChannel.AUTH, AuthChannelEventName.SIGNED_IN, 2)
            .start()
        val signOutAccumulator = HubAccumulator
            .create(HubChannel.AUTH, AuthChannelEventName.SIGNED_OUT, 1)
            .start()

        signInWithCognito()
        signOut()
        signInWithCognito()

        signInAccumulator.await(10, TimeUnit.SECONDS)
        signOutAccumulator.await(10, TimeUnit.SECONDS)
        // if we made it this far without timeout, it means hub event was received
    }

    // This compliments the hub_Events_Are_Received_Only_Once_Per_Change test
    @Test(expected = RuntimeException::class)
    fun hub_Events_Are_Received_Only_Once_Per_Change_2() {
        val signInAccumulatorExtra = HubAccumulator
            .create(HubChannel.AUTH, AuthChannelEventName.SIGNED_IN, 3)
            .start()

        signInWithCognito()
        signOut()
        signInWithCognito()

        signInAccumulatorExtra.await(10, TimeUnit.SECONDS)
        // Execution should not reach here
    }

    @Test
    fun fetchAuthSession_can_pull_session_when_signed_in() {
        signInWithCognito()

        lateinit var session: AuthSession
        val latch = CountDownLatch(1)

        auth.fetchAuthSession(
            {
                session = it
                latch.countDown()
            },
            {
                latch.countDown()
            }
        )
        latch.await(10, TimeUnit.SECONDS)

        assertTrue(session.isSignedIn)
        with(session as AWSCognitoAuthSession) {
            assertNotNull(identityIdResult.value)
            assertNotNull(userPoolTokensResult.value)
            assertNotNull(awsCredentialsResult.value)
            assertNotNull(userSubResult.value)
        }
    }

    @Test
    fun fetchAuthSession_does_not_throw_error_even_when_signed_out() {
        signOut()

        lateinit var session: AuthSession

        val latch = CountDownLatch(1)

        auth.fetchAuthSession(
            {
                session = it
                latch.countDown()
            },
            {
                latch.countDown()
            }
        )
        latch.await(10, TimeUnit.SECONDS)

        assertFalse(session.isSignedIn)
        with(session as AWSCognitoAuthSession) {
            assertNull(identityIdResult.value)
            assertNull(userPoolTokensResult.value)
            assertNull(awsCredentialsResult.value)
            assertNull(userSubResult.value)
        }
    }

    @Test
    fun rememberDevice_succeeds_after_signIn_and_signOut() {
        signInWithCognito()

        val rememberLatch = CountDownLatch(1)

        auth.rememberDevice(
            {
                rememberLatch.countDown()
            },
            {
                rememberLatch.countDown()
                assertTrue(false)
            }
        )

        rememberLatch.await(10, TimeUnit.SECONDS)

        val forgetLatch = CountDownLatch(1)

        auth.forgetDevice(
            {
                forgetLatch.countDown()
            },
            {
                forgetLatch.countDown()
                assertTrue(false)
            }
        )

        forgetLatch.await(10, TimeUnit.SECONDS)

        signOut()
        signInWithCognito()

        val rememberLatch2 = CountDownLatch(1)

        auth.rememberDevice(
            {
                rememberLatch2.countDown()
            },
            {
                assertTrue(false)
                rememberLatch2.countDown()
            }
        )

        rememberLatch2.await(10, TimeUnit.SECONDS)

        val forgetLatch2 = CountDownLatch(1)

        auth.forgetDevice(
            {
                forgetLatch2.countDown()
            },
            {
                assertTrue(false)
                forgetLatch2.countDown()
            }
        )

        forgetLatch2.await(10, TimeUnit.SECONDS)
    }

    private fun signInWithCognito(synchronous: Boolean = true) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)

        val latch = CountDownLatch(1)
        auth.signIn(username, password, { latch.countDown() }, { latch.countDown() })

        if (synchronous) latch.await()
    }

    private fun signOut(synchronous: Boolean = true) {
        val latch = CountDownLatch(1)
        auth.signOut { latch.countDown() }
        if (synchronous) latch.await()
    }
}
