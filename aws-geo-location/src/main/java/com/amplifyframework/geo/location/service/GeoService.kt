/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.location.service

import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.CountryCode
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.models.Place
import com.amplifyframework.geo.models.SearchArea
import com.amplifyframework.geo.options.GeoUpdateLocationOptions

/**
 * Backend provider for Geo Amazon Location Geo plugin.
 */
internal interface GeoService<T> {
    /**
     * Backend provider for Geo data.
     */
    val provider: T

    /**
     * Gets map's style JSON in string format.
     *
     * @param mapName map name
     */
    suspend fun getStyleJson(mapName: String): String

    /**
     * Searches index for the location details given a string query.
     */
    suspend fun geocode(
        index: String,
        query: String,
        limit: Int,
        area: SearchArea? = null,
        countries: List<CountryCode> = emptyList()
    ): List<Place>

    /**
     * Searches index for the location details given a set of coordinates.
     */
    suspend fun reverseGeocode(
        index: String,
        position: Coordinates,
        limit: Int
    ): List<Place>

    /**
     * Sends an update that the device with this ID is in this location.
     */
    suspend fun updateLocation(
        deviceId: String,
        position: GeoPosition,
        options: GeoUpdateLocationOptions,
    )

    /**
     * Sends an update that the device with this ID has been in these locations (at these times).
     */
    suspend fun updateLocations(
        deviceId: String,
        positions: List<GeoPosition>,
        options: GeoUpdateLocationOptions,
    )

    /**
     * Deletes any stored location history for the given device.
     */
    suspend fun deleteLocationHistory(
        deviceId: String,
        tracker: String
    )
}
