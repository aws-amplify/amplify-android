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
 * @param name Display name (maps to FirstName in Customer Profiles)
 * @param email Email address (maps to EmailAddress)
 * @param phoneNumber Phone number (maps to PhoneNumber)
 * @param plan Subscription plan or tier (stored as custom attribute)
 * @param customAttributes Additional key-value attributes to store on the profile
 */
data class UserProfile(
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val plan: String? = null,
    val customAttributes: Map<String, String>? = null
)
