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

import com.amplifyframework.connect.ConnectValidationException
import com.amplifyframework.connect.UserProfile
import com.amplifyframework.connect.UserProfileLocation

/**
 * Client-side input length validation mirroring backend limits (construct head
 * aeffa206). MAX_ATTRIBUTE_LENGTH = 255 on all string fields.
 */
internal object InputValidation {
    const val MAX_ATTRIBUTE_LENGTH = 255

    fun validateUserProfile(profile: UserProfile) {
        profile.email?.let { checkLength("email", it) }
        profile.name?.let { checkLength("name", it) }
        profile.phone?.let { checkLength("phone", it) }
        profile.customAttributes?.forEach { (key, value) ->
            checkLength("customAttributes key '$key'", key)
            checkLength("customAttributes value for '$key'", value)
        }
        profile.location?.let { validateLocation(it) }
    }

    fun validateToken(token: String) {
        checkLength("token", token)
    }

    private fun validateLocation(location: UserProfileLocation) {
        location.city?.let { checkLength("location.city", it) }
        location.country?.let { checkLength("location.country", it) }
        location.postalCode?.let { checkLength("location.postalCode", it) }
        location.region?.let { checkLength("location.region", it) }
    }

    private fun checkLength(fieldName: String, value: String) {
        if (value.length > MAX_ATTRIBUTE_LENGTH) {
            throw ConnectValidationException(
                detail = "$fieldName exceeds maximum length of $MAX_ATTRIBUTE_LENGTH characters."
            )
        }
    }
}
