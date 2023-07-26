/*
 *  Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.pinpoint.core.endpointProfile

import com.amplifyframework.annotations.InternalAmplifyApi
import kotlinx.serialization.Serializable

/**
 * Stores the location associated with the endpoint
 *
 * @param country endpoint location country
 * @param latitude endpoint location latitude
 * @param longitude endpoint location longitude
 * @param postalCode endpoint location postalCode
 * @param city endpoint location city
 * @param region endpoint location region
 */
@Serializable
@InternalAmplifyApi
data class EndpointProfileLocation(
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val postalCode: String = "",
    val city: String = "",
    val region: String = ""
)
