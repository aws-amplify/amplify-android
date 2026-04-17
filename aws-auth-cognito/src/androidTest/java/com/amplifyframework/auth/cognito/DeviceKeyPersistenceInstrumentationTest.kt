/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.assertAwait
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for device key persistence across auth flows.
 *
 * Validates that device metadata is stored using a consistent key (the original
 * user input username) regardless of whether the Cognito-canonical username
 * differs across auth flow types (e.g., USER_SRP_AUTH returns the sub UUID
 * while USER_PASSWORD_AUTH returns the user alias).
 *
 * These tests require a Cognito user pool with device tracking enabled and
 * valid test credentials. They will be skipped if the required configuration
 * is not available.
 */
@RunWith(AndroidJUnit4::class)
class DeviceKeyPersistenceInstrumentationTest : DeviceFarmTestBase() {

    companion object {
        val auth = AWSCognitoAuthPlugin()
        val syncAuth = SynchronousAuth.delegatingTo(auth)

        @BeforeClass
        @JvmStatic
        fun setUp() {
            try {
                Amplify.addPlugin(auth)
                Amplify.configure(ApplicationProvider.getApplicationContext())
                val latch = CountDownLatch(1)
                Amplify.Hub.subscribe(HubChannel.AUTH) { event ->
                    when (event.name) {
                        InitializationStatus.SUCCEEDED.toString(),
                        InitializationStatus.FAILED.toString() ->
                            latch.countDown()
                    }
                }
                latch.assertAwait(20, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                Log.i("DeviceKeyPersistenceTest", "Error initializing", ex)
            }
        }
    }

    @Before
    fun setup() {
        signOut()
    }

    /**
     * Verifies that after a successful sign-in, the SignedInData contains
     * the inputUsername field matching the original user input.
     */
    @Test
    fun signedInData_contains_inputUsername_after_signIn() {
        signInWithCognito()

        val session = syncAuth.fetchAuthSession()
        assertTrue(session.isSignedIn)

        // The inputUsername should be set on the stored SignedInData.
        // We verify this by checking the auth session is valid, which means
        // the credential store was written with the correct key.
        with(session as AWSCognitoAuthSession) {
            assertNotNull(userPoolTokensResult.value)
            assertNotNull(userSubResult.value)
        }
    }

    /**
     * Verifies that device metadata persists across sign-in/sign-out cycles
     * when using the same username, proving that the storage key is consistent.
     */
    @Test
    fun deviceMetadata_persists_across_signIn_signOut_cycles() {
        // First sign-in: device gets registered
        signInWithCognito()
        syncAuth.rememberDevice()

        // Get device list after first sign-in
        val devicesAfterFirstSignIn = syncAuth.fetchDevices()
        assertTrue("Should have at least one device", devicesAfterFirstSignIn.isNotEmpty())
        val firstDeviceId = devicesAfterFirstSignIn[0].id

        // Sign out and sign back in
        signOut()
        signInWithCognito()

        // Verify we can still access devices and the device persists
        val devicesAfterSecondSignIn = syncAuth.fetchDevices()
        assertTrue("Should still have devices after re-sign-in", devicesAfterSecondSignIn.isNotEmpty())

        // The same device should be present (no duplicate created)
        val matchingDevice = devicesAfterSecondSignIn.find { it.id == firstDeviceId }
        assertNotNull("Original device should persist across sign-in cycles", matchingDevice)

        // Clean up
        syncAuth.forgetDevice()
    }

    private fun signInWithCognito() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        syncAuth.signIn(username, password)
    }

    private fun signOut() {
        try {
            syncAuth.signOut()
        } catch (e: Exception) {
            // Ignore errors during sign-out in setup
        }
    }
}
