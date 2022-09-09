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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoAuthPluginInstrumentationTests {
    var auth: SynchronousAuth? = null

    @Before
    fun setUp() {
        // Auth plugin uses default configuration
        auth = SynchronousAuth.delegatingToCognito(ApplicationProvider.getApplicationContext(), AWSCognitoAuthPlugin())
    }

    @After
    fun tearDown() {
        auth?.signOut()
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
        auth?.signOut()

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
        auth?.signOut()
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
        auth?.signOut()
        signInWithCognito()

        signInAccumulatorExtra.await(10, TimeUnit.SECONDS)
        // Execution should not reach here
    }

    private fun signInWithCognito() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        auth?.signIn(username, password)
    }
}
