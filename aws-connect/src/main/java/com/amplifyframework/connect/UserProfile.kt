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

/**
 * User profile attributes to associate with the Customer Profiles record.
 *
 * @param email Email address
 * @param name Display name
 * @param phone Phone number
 * @param customAttributes Additional key-value string attributes
 * @param location Geographic location attributes
 */
data class UserProfile(
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val customAttributes: Map<String, String>? = null,
    val location: UserProfileLocation? = null
)

/**
 * Geographic location attributes for a user profile.
 *
 * @param city City name
 * @param country Country name or code
 * @param postalCode Postal or zip code
 * @param region State, province, or region
 */
data class UserProfileLocation(
    val city: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val region: String? = null
)
