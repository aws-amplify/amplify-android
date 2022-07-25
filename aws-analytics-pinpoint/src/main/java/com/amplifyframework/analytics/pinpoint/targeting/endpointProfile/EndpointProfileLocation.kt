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

import android.content.Context
import com.amplifyframework.analytics.pinpoint.internal.core.util.JSONSerializable
import com.amplifyframework.analytics.pinpoint.internal.core.util.JSONBuilder
import com.amplifyframework.core.Amplify
import org.json.JSONObject
import java.util.*

class EndpointProfileLocation(context: Context) : JSONSerializable {
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

    internal var country = ""
    fun getCountry() = country
    fun setCountry(country: String) = country.also { this.country = it }


    override fun toJSONObject(): JSONObject {
        val builder = JSONBuilder(null)
        builder.withAttribute("Latitude", latitude)
        builder.withAttribute("Longitude", longitude)
        builder.withAttribute("PostalCode", postalCode)
        builder.withAttribute("City", city)
        builder.withAttribute("Region", region)
        builder.withAttribute("Country", country)
        return builder.toJSONObject()
    }

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
    }

    /**
     * Default constructor
     *
     * @param context the context
     */
    init {
        val localeCountry = try {
            context.resources.configuration.locales[0].isO3Country
        } catch (exception: MissingResourceException) {
            LOG.debug("Locale getISO3Country failed, falling back to getCountry.")
            context.resources.configuration.locales[0].country
        }
        country = localeCountry
    }
}