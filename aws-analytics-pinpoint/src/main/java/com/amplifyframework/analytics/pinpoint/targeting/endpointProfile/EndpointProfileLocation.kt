/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.analytics.pinpoint.targeting.endpointProfile

import kotlinx.serialization.Serializable

/**
 * Stores the location associated with the endpoint
 *
 * @param context the Android context, which we use to get the locale
 */
@Serializable
data class EndpointProfileLocation internal constructor(internal var country: String) {
    internal var latitude: Double? = null
    fun getLatitude() = latitude
    fun setLatitude(latitude: Double) = latitude.also { this.latitude = it }

    internal var longitude: Double? = null
    fun getLongitude() = longitude
    fun setLongitude(longitude: Double) = longitude.also { this.longitude = it }

    internal var postalCode = ""
    fun getPostalCode() = postalCode
    fun setPostalCode(postalCode: String) = postalCode.also { this.postalCode = it }

    internal var city = ""
    fun getCity() = city
    fun setCity(city: String) = city.also { this.city = it }

    internal var region = ""
    fun getRegion() = region
    fun setRegion(region: String) = region.also { this.region = it }

    fun getCountry() = country
    fun setCountry(country: String) = country.also { this.country = it }
}
