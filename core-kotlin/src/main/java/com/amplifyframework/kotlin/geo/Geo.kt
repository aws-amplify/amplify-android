/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.kotlin.geo

import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.geo.result.GeoSearchResult

interface Geo {
    /**
     * Gets a collection of maps and their corresponding styles.
     *
     * @return A collection of all available [MapStyle].
     */
    suspend fun getAvailableMaps(): Collection<MapStyle>

    /**
     * Gets the default map and style from available maps.
     *
     * @return The default [MapStyle].
     */
    suspend fun getDefaultMap(): MapStyle

    /**
     * Uses given options to get map style descriptor JSON.
     *
     * @param options  Options to specify for this operation.
     * @return The [MapStyleDescriptor] matching the given options.
     */
    suspend fun getMapStyleDescriptor(
        options: GetMapStyleDescriptorOptions = GetMapStyleDescriptorOptions.defaults()
    ): MapStyleDescriptor

    /**
     * Searches for locations that match text query.
     *
     * @param query    Search query text.
     * @param options  Search options to use.
     * @return The [GeoSearchResult] for the query and options.
     */
    suspend fun searchByText(
        query: String,
        options: GeoSearchByTextOptions = GeoSearchByTextOptions.defaults()
    ): GeoSearchResult

    /**
     * Searches for location with given set of coordinates.
     *
     * @param position Coordinates to look-up.
     * @param options  Search options to use.
     * @return The [GeoSearchResult] for the position and options.
     */
    suspend fun searchByCoordinates(
        position: Coordinates,
        options: GeoSearchByCoordinatesOptions = GeoSearchByCoordinatesOptions.defaults()
    ): GeoSearchResult
}
