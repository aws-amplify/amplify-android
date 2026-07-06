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
import aws.sdk.kotlin.services.customerprofiles.model.AddProfileKeyRequest
import aws.sdk.kotlin.services.customerprofiles.model.CreateProfileRequest
import aws.sdk.kotlin.services.customerprofiles.model.GetProfileObjectTypeRequest
import aws.sdk.kotlin.services.customerprofiles.model.ResourceNotFoundException
import aws.sdk.kotlin.services.customerprofiles.model.SearchProfilesRequest
import aws.sdk.kotlin.services.customerprofiles.model.UpdateProfileRequest
import com.amplifyframework.connect.ConnectObjectTypeNotConfiguredException
import com.amplifyframework.connect.UserProfile

/**
 * Handles Customer Profiles identity resolution:
 * - Validates the AmplifyDevice ObjectType exists
 * - Creates or finds a profile linked to the Cognito identity
 * - Updates profile attributes from [UserProfile]
 */
internal class IdentityResolver(
    private val client: CustomerProfilesClient,
    private val domainName: String
) {
    @Volatile
    private var objectTypeValidated = false

    /**
     * Validates that the AmplifyDevice ProfileObjectType is provisioned.
     * Caches the result for the session lifetime.
     *
     * @throws ConnectObjectTypeNotConfiguredException if the type is not found
     */
    suspend fun validateObjectType() {
        if (objectTypeValidated) return
        try {
            val request = GetProfileObjectTypeRequest {
                this.domainName = this@IdentityResolver.domainName
                this.objectTypeName = OBJECT_TYPE_NAME
            }
            client.getProfileObjectType(request)
            objectTypeValidated = true
        } catch (e: ResourceNotFoundException) {
            throw ConnectObjectTypeNotConfiguredException(
                message = "The '$OBJECT_TYPE_NAME' ProfileObjectType is not configured in domain '$domainName'.",
                recoverySuggestion = "Run the backend setup to create the AmplifyDevice ProfileObjectType " +
                    "via PutProfileObjectType in your Customer Profiles domain.",
                cause = e
            )
        }
    }

    /**
     * Resolves the profileId for a given Cognito sub. Searches for an existing profile
     * by the cognitoUserKey. If not found, creates a new profile and links the key.
     *
     * @param cognitoSub The Cognito identity sub
     * @param userProfile Optional user attributes to set
     * @return The resolved profileId
     */
    suspend fun resolveProfile(cognitoSub: String, userProfile: UserProfile?): String {
        // Search for existing profile by Cognito key
        val searchRequest = SearchProfilesRequest {
            this.domainName = this@IdentityResolver.domainName
            this.keyName = COGNITO_USER_KEY
            this.values = listOf(cognitoSub)
        }
        val searchResponse = client.searchProfiles(searchRequest)

        val profileId = searchResponse.items?.firstOrNull()?.profileId
            ?: createAndLinkProfile(cognitoSub)

        // Update profile with user-provided attributes
        if (userProfile != null) {
            updateProfileAttributes(profileId, cognitoSub, userProfile)
        }

        return profileId
    }

    private suspend fun createAndLinkProfile(cognitoSub: String): String {
        val createRequest = CreateProfileRequest {
            this.domainName = this@IdentityResolver.domainName
            this.attributes = mapOf("CognitoUserId" to cognitoSub)
        }
        val createResponse = client.createProfile(createRequest)
        val profileId = createResponse.profileId

        // Link the cognitoUserKey for future SearchProfiles lookups
        val addKeyRequest = AddProfileKeyRequest {
            this.domainName = this@IdentityResolver.domainName
            this.profileId = profileId
            this.keyName = COGNITO_USER_KEY
            this.values = listOf(cognitoSub)
        }
        client.addProfileKey(addKeyRequest)

        return profileId
    }

    private suspend fun updateProfileAttributes(profileId: String, cognitoSub: String, userProfile: UserProfile) {
        val attrs = mutableMapOf("CognitoUserId" to cognitoSub)
        userProfile.plan?.let { attrs["Plan"] = it }
        userProfile.customAttributes?.let { attrs.putAll(it) }

        val updateRequest = UpdateProfileRequest {
            this.domainName = this@IdentityResolver.domainName
            this.profileId = profileId
            userProfile.name?.let { this.firstName = it }
            userProfile.email?.let { this.emailAddress = it }
            userProfile.phoneNumber?.let { this.phoneNumber = it }
            this.attributes = attrs
        }
        client.updateProfile(updateRequest)
    }

    internal companion object {
        const val OBJECT_TYPE_NAME = "AmplifyDevice"
        const val COGNITO_USER_KEY = "cognitoUserKey"
    }
}
