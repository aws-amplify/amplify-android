/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

import com.amplifyframework.connect.internal.DeviceIdStore
import com.amplifyframework.connect.internal.IdentifyUserService
import com.amplifyframework.foundation.credentials.AwsCredentials
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(RobolectricTestRunner::class)
class AmplifyConnectClientTest {

    private val mockService = mockk<IdentifyUserService>(relaxed = true)
    private val mockDeviceIdStore = mockk<DeviceIdStore>(relaxed = true)
    private val mockCredentialsProvider = mockk<ConnectCredentialsProvider>()

    private fun createClient(platform: String? = "Android", appVersion: String? = "1.0.0") = AmplifyConnectClient(
        configuration = ConnectClientConfiguration(
            endpoint = "https://test.execute-api.us-east-1.amazonaws.com",
            region = "us-east-1"
        ),
        credentialsProvider = mockCredentialsProvider,
        deviceIdStore = mockDeviceIdStore,
        platform = platform,
        appVersion = appVersion,
        service = mockService
    )

    @Test
    fun `identifyUser sends authenticated request with bearer token`() = runTest {
        coEvery { mockCredentialsProvider.fetchSession() } returns ConnectSession(
            accessToken = "test-access-token"
        )

        val client = createClient()
        client.identifyUser(userProfile = UserProfile(name = "Alice"), userId = "user-1")

        val sessionSlot = slot<ConnectSession>()
        val bodySlot = slot<String>()
        coVerify { mockService.identify(capture(sessionSlot), capture(bodySlot)) }
        sessionSlot.captured.isAuthenticated shouldBe true
        bodySlot.captured shouldContain "\"userId\":\"user-1\""
        bodySlot.captured shouldContain "\"name\":\"Alice\""
    }

    @Test
    fun `identifyUser sends guest request with credentials`() = runTest {
        val guestCreds = AwsCredentials.Temporary(
            accessKeyId = "AKIA_GUEST",
            secretAccessKey = "secret",
            sessionToken = "session-token",
            expiration = kotlin.time.Instant.DISTANT_FUTURE
        )
        coEvery { mockCredentialsProvider.fetchSession() } returns ConnectSession(
            credentials = guestCreds,
            identityId = "us-east-1:guest-id"
        )

        val client = createClient()
        client.identifyUser(userProfile = UserProfile(email = "guest@test.com"))

        val sessionSlot = slot<ConnectSession>()
        coVerify { mockService.identify(capture(sessionSlot), any()) }
        sessionSlot.captured.isAuthenticated shouldBe false
        sessionSlot.captured.credentials shouldBe guestCreds
    }

    @Test
    fun `identifyUser includes options when non-empty`() = runTest {
        coEvery { mockCredentialsProvider.fetchSession() } returns ConnectSession(
            accessToken = "token"
        )

        val client = createClient()
        client.identifyUser(
            userProfile = UserProfile(),
            options = IdentifyUserOptions(
                previousGuestIdentityId = "us-east-1:old-guest",
                deviceId = "device-123"
            )
        )

        val bodySlot = slot<String>()
        coVerify { mockService.identify(any(), capture(bodySlot)) }
        bodySlot.captured shouldContain "\"previousGuestIdentityId\":\"us-east-1:old-guest\""
        bodySlot.captured shouldContain "\"deviceId\":\"device-123\""
    }

    @Test
    fun `registerDevice folds into identifyUser with device options`() = runTest {
        coEvery { mockCredentialsProvider.fetchSession() } returns ConnectSession(
            accessToken = "token"
        )
        every { mockDeviceIdStore.getOrCreate() } returns "stable-device-uuid"

        val client = createClient()
        client.registerDevice(
            deviceToken = "fcm-token-abc",
            channelType = ChannelType.GCM,
            userId = "user-1"
        )

        val bodySlot = slot<String>()
        coVerify { mockService.identify(any(), capture(bodySlot)) }
        bodySlot.captured shouldContain "\"address\":\"fcm-token-abc\""
        bodySlot.captured shouldContain "\"deviceId\":\"stable-device-uuid\""
        bodySlot.captured shouldContain "\"channelType\":\"GCM\""
        bodySlot.captured shouldContain "\"platform\":\"Android\""
        bodySlot.captured shouldContain "\"appVersion\":\"1.0.0\""
    }

    @Test
    fun `registerDevice uses previousGuestIdentityId for merge on sign-in`() = runTest {
        coEvery { mockCredentialsProvider.fetchSession() } returns ConnectSession(
            accessToken = "token"
        )
        every { mockDeviceIdStore.getOrCreate() } returns "device-id"

        val client = createClient()
        client.registerDevice(
            deviceToken = "token",
            channelType = ChannelType.GCM,
            previousGuestIdentityId = "us-east-1:old-guest-id"
        )

        val bodySlot = slot<String>()
        coVerify { mockService.identify(any(), capture(bodySlot)) }
        bodySlot.captured shouldContain "\"previousGuestIdentityId\":\"us-east-1:old-guest-id\""
    }

    @Test
    fun `removeDevice throws unsupported`() = runTest {
        val client = createClient()
        shouldThrow<ConnectUnsupportedOperationException> {
            client.removeDevice()
        }
    }

    @Test
    fun `reset does not throw`() {
        val client = createClient()
        client.reset() // no-op, should not throw
    }
}
