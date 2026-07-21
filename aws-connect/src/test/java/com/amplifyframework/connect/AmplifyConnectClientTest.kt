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

import com.amplifyframework.connect.internal.ConnectService
import com.amplifyframework.connect.internal.DeviceIdStore
import com.amplifyframework.foundation.credentials.AwsCredentials
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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

    private val mockService = mockk<ConnectService>(relaxed = true)
    private val mockDeviceIdStore = mockk<DeviceIdStore>()
    private val mockCredentialsProvider = mockk<ConnectCredentialsProvider>()
    private val testCredentials = AwsCredentials.Temporary(
        accessKeyId = "AKID",
        secretAccessKey = "secret",
        sessionToken = "session",
        expiration = kotlin.time.Instant.DISTANT_FUTURE
    )

    private fun createClient(
        platform: String? = "Android",
        appVersion: String? = "1.0.0",
        channelType: ChannelType = ChannelType.GCM
    ) = AmplifyConnectClient(
        configuration = ConnectClientConfiguration(
            endpoint = "https://test.execute-api.us-east-1.amazonaws.com",
            region = "us-east-1"
        ),
        credentialsProvider = mockCredentialsProvider,
        deviceIdStore = mockDeviceIdStore,
        platform = platform,
        appVersion = appVersion,
        channelType = channelType,
        service = mockService
    )

    @Test
    fun `identifyUser sends userProfile with all fields`() = runTest {
        coEvery { mockCredentialsProvider.resolve() } returns testCredentials

        val client = createClient()
        client.identifyUser(
            UserProfile(
                email = "alice@test.com",
                name = "Alice",
                phone = "+1234567890",
                customAttributes = mapOf("tier" to "premium"),
                location = UserProfileLocation(city = "Seattle", country = "US")
            )
        )

        val bodySlot = slot<String>()
        coVerify { mockService.identifyUser(testCredentials, capture(bodySlot)) }
        bodySlot.captured shouldContain "\"email\":\"alice@test.com\""
        bodySlot.captured shouldContain "\"name\":\"Alice\""
        bodySlot.captured shouldContain "\"phone\":\"+1234567890\""
        bodySlot.captured shouldContain "\"tier\":\"premium\""
        bodySlot.captured shouldContain "\"city\":\"Seattle\""
        bodySlot.captured shouldContain "\"country\":\"US\""
    }

    @Test
    fun `identifyUser does not send userId`() = runTest {
        coEvery { mockCredentialsProvider.resolve() } returns testCredentials

        val client = createClient()
        client.identifyUser(UserProfile(name = "Bob"))

        val bodySlot = slot<String>()
        coVerify { mockService.identifyUser(testCredentials, capture(bodySlot)) }
        bodySlot.captured shouldNotContain "userId"
    }

    @Test
    fun `registerDevice sends device object with token and deviceId`() = runTest {
        coEvery { mockCredentialsProvider.resolve() } returns testCredentials
        every { mockDeviceIdStore.getOrCreate() } returns "stable-device-uuid"

        val client = createClient()
        client.registerDevice("fcm-token-abc")

        val bodySlot = slot<String>()
        coVerify { mockService.registerDevice(testCredentials, capture(bodySlot)) }
        bodySlot.captured shouldContain "\"token\":\"fcm-token-abc\""
        bodySlot.captured shouldContain "\"deviceId\":\"stable-device-uuid\""
        bodySlot.captured shouldContain "\"platform\":\"Android\""
        bodySlot.captured shouldContain "\"appVersion\":\"1.0.0\""
        bodySlot.captured shouldContain "\"channelType\":\"GCM\""
    }

    @Test
    fun `registerDevice omits null platform and appVersion`() = runTest {
        coEvery { mockCredentialsProvider.resolve() } returns testCredentials
        every { mockDeviceIdStore.getOrCreate() } returns "device-id"

        val client = createClient(platform = null, appVersion = null)
        client.registerDevice("token")

        val bodySlot = slot<String>()
        coVerify { mockService.registerDevice(testCredentials, capture(bodySlot)) }
        bodySlot.captured shouldNotContain "\"platform\""
        bodySlot.captured shouldNotContain "\"appVersion\""
    }

    @Test
    fun `removeDevice sends deviceId from store`() = runTest {
        coEvery { mockCredentialsProvider.resolve() } returns testCredentials
        every { mockDeviceIdStore.getOrCreate() } returns "my-device-id"

        val client = createClient()
        client.removeDevice()

        val bodySlot = slot<String>()
        coVerify { mockService.removeDevice(testCredentials, capture(bodySlot)) }
        bodySlot.captured shouldContain "\"deviceId\":\"my-device-id\""
    }
}
