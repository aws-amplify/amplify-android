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
package com.amplifyframework.connect.internal

import aws.sdk.kotlin.services.customerprofiles.CustomerProfilesClient
import aws.sdk.kotlin.services.customerprofiles.model.AddProfileKeyResponse
import aws.sdk.kotlin.services.customerprofiles.model.CreateProfileResponse
import aws.sdk.kotlin.services.customerprofiles.model.GetProfileObjectTypeResponse
import aws.sdk.kotlin.services.customerprofiles.model.Profile
import aws.sdk.kotlin.services.customerprofiles.model.ResourceNotFoundException
import aws.sdk.kotlin.services.customerprofiles.model.SearchProfilesResponse
import aws.sdk.kotlin.services.customerprofiles.model.UpdateProfileResponse
import com.amplifyframework.connect.ConnectObjectTypeNotConfiguredException
import com.amplifyframework.connect.UserProfile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityResolverTest {

    private val mockClient = mockk<CustomerProfilesClient>(relaxed = true)
    private val domainName = "test-domain"
    private val resolver = IdentityResolver(mockClient, domainName)

    @Test
    fun `validateObjectType succeeds when type exists`() = runTest {
        coEvery { mockClient.getProfileObjectType(any()) } returns GetProfileObjectTypeResponse {
            objectTypeName = "AmplifyDevice"
            description = "test"
        }

        resolver.validateObjectType()

        coVerify(exactly = 1) { mockClient.getProfileObjectType(any()) }
    }

    @Test
    fun `validateObjectType caches result after first success`() = runTest {
        coEvery { mockClient.getProfileObjectType(any()) } returns GetProfileObjectTypeResponse {
            objectTypeName = "AmplifyDevice"
            description = "test"
        }

        resolver.validateObjectType()
        resolver.validateObjectType()

        coVerify(exactly = 1) { mockClient.getProfileObjectType(any()) }
    }

    @Test
    fun `validateObjectType throws when type not found`() = runTest {
        coEvery { mockClient.getProfileObjectType(any()) } throws ResourceNotFoundException {
            message = "Object type not found"
        }

        val exception = shouldThrow<ConnectObjectTypeNotConfiguredException> {
            resolver.validateObjectType()
        }
        exception.message shouldContain "AmplifyDevice"
        exception.message shouldContain "not configured"
    }

    @Test
    fun `resolveProfile returns existing profile when found`() = runTest {
        val existingProfileId = "profile-123"
        coEvery { mockClient.searchProfiles(any()) } returns SearchProfilesResponse {
            items = listOf(
                Profile { profileId = existingProfileId }
            )
        }

        val result = resolver.resolveProfile("cognito-sub-1", null)

        result shouldBe existingProfileId
        coVerify(exactly = 0) { mockClient.createProfile(any()) }
    }

    @Test
    fun `resolveProfile creates and links new profile when not found`() = runTest {
        val newProfileId = "new-profile-456"
        coEvery { mockClient.searchProfiles(any()) } returns SearchProfilesResponse {
            items = emptyList()
        }
        coEvery { mockClient.createProfile(any()) } returns CreateProfileResponse {
            profileId = newProfileId
        }
        coEvery { mockClient.addProfileKey(any()) } returns AddProfileKeyResponse {
            keyName = "cognitoUserKey"
            values = listOf("cognito-sub-1")
        }

        val result = resolver.resolveProfile("cognito-sub-1", null)

        result shouldBe newProfileId
        coVerify(exactly = 1) { mockClient.createProfile(any()) }
        coVerify(exactly = 1) { mockClient.addProfileKey(any()) }
    }

    @Test
    fun `resolveProfile updates attributes when userProfile is provided`() = runTest {
        val profileId = "profile-789"
        coEvery { mockClient.searchProfiles(any()) } returns SearchProfilesResponse {
            items = listOf(Profile { this.profileId = profileId })
        }
        coEvery { mockClient.updateProfile(any()) } returns UpdateProfileResponse {
            this.profileId = profileId
        }

        val userProfile = UserProfile(
            name = "Test User",
            email = "test@example.com",
            plan = "premium"
        )
        resolver.resolveProfile("cognito-sub-1", userProfile)

        coVerify(exactly = 1) { mockClient.updateProfile(any()) }
    }

    @Test
    fun `resolveProfile skips update when userProfile is null`() = runTest {
        coEvery { mockClient.searchProfiles(any()) } returns SearchProfilesResponse {
            items = listOf(Profile { profileId = "profile-1" })
        }

        resolver.resolveProfile("cognito-sub-1", null)

        coVerify(exactly = 0) { mockClient.updateProfile(any()) }
    }
}
