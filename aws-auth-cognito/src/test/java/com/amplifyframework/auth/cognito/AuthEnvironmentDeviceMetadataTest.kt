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

package com.amplifyframework.auth.cognito

import android.content.Context
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthEnvironmentDeviceMetadataTest {

    private val credentialStoreClient = mockk<StoreClientBehavior>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private val username = "0f9a6b1c-uuid"
    private val email = "user@example.com"
    private val metadata = DeviceMetadata.Metadata("device-key", "device-group-key", "device-secret")

    private val environment = AuthEnvironment(
        mockk<Context>(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        credentialStoreClient,
        null,
        null,
        logger
    )

    private fun storeContains(vararg entries: Pair<String, DeviceMetadata>) {
        val byUsername = entries.toMap()
        coEvery { credentialStoreClient.loadCredentials(any()) } answers {
            val requested = (firstArg<CredentialType>() as CredentialType.Device).username
            AmplifyCredential.DeviceData(byUsername[requested] ?: DeviceMetadata.Empty)
        }
    }

    @Test
    fun `returns metadata stored under the current username`() = runTest {
        storeContains(username to metadata)

        environment.getDeviceMetadata(username, listOf(email)) shouldBe metadata
    }

    @Test
    fun `returns null when no metadata is stored under any key`() = runTest {
        storeContains()

        environment.getDeviceMetadata(username, listOf(email)) shouldBe null
    }

    @Test
    fun `falls back to metadata stored under a legacy alias key`() = runTest {
        storeContains(email to metadata)

        environment.getDeviceMetadata(username, listOf(email)) shouldBe metadata
    }

    @Test
    fun `migrates legacy metadata to the current username and clears the legacy entry`() = runTest {
        storeContains(email to metadata)

        environment.getDeviceMetadata(username, listOf(email))

        coVerify {
            credentialStoreClient.storeCredentials(
                CredentialType.Device(username),
                AmplifyCredential.DeviceData(metadata)
            )
            credentialStoreClient.clearCredentials(CredentialType.Device(email))
        }
    }

    @Test
    fun `does not migrate when metadata already exists under the current username`() = runTest {
        storeContains(username to metadata, email to metadata)

        environment.getDeviceMetadata(username, listOf(email))

        coVerify(exactly = 0) { credentialStoreClient.storeCredentials(any(), any()) }
        coVerify(exactly = 0) { credentialStoreClient.clearCredentials(any()) }
    }

    @Test
    fun `does not consult the store twice when the alias equals the current username`() = runTest {
        storeContains()

        environment.getDeviceMetadata(username, listOf(username)) shouldBe null

        coVerify(exactly = 1) { credentialStoreClient.loadCredentials(CredentialType.Device(username)) }
    }

    @Test
    fun `no legacy candidates behaves like a plain lookup`() = runTest {
        storeContains(email to metadata)

        environment.getDeviceMetadata(username) shouldBe null
    }
}
