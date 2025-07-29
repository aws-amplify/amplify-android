/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

@file:JvmName("CoordinateUtils")

package com.amplifyframework.geo.maplibre.util

import com.amplifyframework.geo.models.Coordinates
import org.maplibre.android.geometry.LatLng

/**
 * Converts the Amplify `Coordinates` to MapLibre's `LatLng` object.
 * @return the latitude/longitude object
 * @see toCoordinates
 */
fun Coordinates.toLatLng(): LatLng = LatLng(
    this.latitude,
    this.longitude
)

/**
 * Converts the MapLibre's `LatLng` object to Amplify `Coordinates` model.
 * @return the coordinates model
 * @see toLatLng
 */
fun LatLng.toCoordinates(): Coordinates = Coordinates(
    this.latitude,
    this.longitude
)

/**
 * Parse a string in the format of "latitude,longitude" to a `Coordinates`
 * @return the coordinates model in case the string can be parsed correctly or `null` otherwise.
 */
fun parseCoordinates(latlong: String): Coordinates? {
    val values = latlong.split(",")
    if (values.count() != 2) return null

    val (latitude, longitude) = values.map { it.toDouble() }
    return Coordinates(latitude, longitude)
}
