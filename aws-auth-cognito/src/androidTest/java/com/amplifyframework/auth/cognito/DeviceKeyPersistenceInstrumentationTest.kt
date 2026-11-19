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
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.testutils.Credentials
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.assertAwait
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for device key persistence across different auth flows.
 *
 * Validates that once a device is remembered, the same device key is reused
 * across sign-out/sign-in cycles regardless of which auth flow is used
 * (USER_PASSWORD_AUTH, USER_SRP_AUTH). The device count should always remain 1.
 *
 * Prerequisites:
 * - Cognito User Pool with Device Tracking set to "Always Remember"
 * - USER_PASSWORD_AUTH and USER_SRP_AUTH both enabled on the app client
 * - Valid test credentials in credentials.json
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
     * Sign in with USER_PASSWORD_AUTH, remember device (1 device).
     * Sign out, sign in with USER_SRP_AUTH — device count should stay 1, same device key.
     * Sign out, sign in with USER_PASSWORD_AUTH again — still 1, same key.
     * Sign out, sign in with USER_SRP_AUTH again — still 1, same key.
     */
    @Test
    fun deviceKey_stays_consistent_across_alternating_auth_flows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)

        // Step 1: Sign in with USER_PASSWORD_AUTH and remember device
        signIn(username, password, AuthFlowType.USER_PASSWORD_AUTH)
        syncAuth.rememberDevice()

        val initialDevices = syncAuth.fetchDevices()
        assertEquals("Should have exactly 1 device after initial sign-in", 1, initialDevices.size)
        val originalDeviceId = initialDevices[0].id

        // Step 2: Sign out, sign in with USER_SRP_AUTH — same device
        signOut()
        signIn(username, password, AuthFlowType.USER_SRP_AUTH)

        var devices = syncAuth.fetchDevices()
        assertEquals("Should still have 1 device after SRP sign-in", 1, devices.size)
        assertEquals("Device ID should match after SRP sign-in", originalDeviceId, devices[0].id)

        // Step 3: Sign out, sign in with USER_PASSWORD_AUTH — same device
        signOut()
        signIn(username, password, AuthFlowType.USER_PASSWORD_AUTH)

        devices = syncAuth.fetchDevices()
        assertEquals("Should still have 1 device after second PASSWORD sign-in", 1, devices.size)
        assertEquals("Device ID should match after second PASSWORD sign-in", originalDeviceId, devices[0].id)

        // Step 4: Sign out, sign in with USER_SRP_AUTH — same device
        signOut()
        signIn(username, password, AuthFlowType.USER_SRP_AUTH)

        devices = syncAuth.fetchDevices()
        assertEquals("Should still have 1 device after second SRP sign-in", 1, devices.size)
        assertEquals("Device ID should match after second SRP sign-in", originalDeviceId, devices[0].id)

        // Clean up
        syncAuth.forgetDevice()
    }

    /**
     * Same-flow baseline: sign in with USER_SRP_AUTH, remember device,
     * sign out, sign in with USER_SRP_AUTH — device count stays 1.
     */
    @Test
    fun deviceKey_persists_across_same_flow_signIn_signOut() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)

        signIn(username, password, AuthFlowType.USER_SRP_AUTH)
        syncAuth.rememberDevice()

        val initialDevices = syncAuth.fetchDevices()
        assertEquals("Should have exactly 1 device", 1, initialDevices.size)
        val originalDeviceId = initialDevices[0].id

        signOut()
        signIn(username, password, AuthFlowType.USER_SRP_AUTH)

        val devices = syncAuth.fetchDevices()
        assertEquals("Should still have 1 device after re-sign-in", 1, devices.size)
        assertEquals("Device ID should be the same", originalDeviceId, devices[0].id)

        // Clean up
        syncAuth.forgetDevice()
    }

    private fun signIn(username: String, password: String, flowType: AuthFlowType) {
        val options: AuthSignInOptions = AWSCognitoAuthSignInOptions.builder()
            .authFlowType(flowType)
            .build()
        syncAuth.signIn(username, password, options)
    }

    private fun signOut() {
        try {
            syncAuth.signOut()
        } catch (e: Exception) {
            // Ignore errors during sign-out
        }
    }
}
