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
import com.mapbox.mapboxsdk.geometry.LatLng

/**
 * @return
 */
fun Coordinates.toLatLng(): LatLng = LatLng(
    this.latitude, this.longitude
)

/**
 * @return
 */
fun LatLng.toCoordinates(): Coordinates = Coordinates(
    this.latitude, this.longitude
)

/**
 *
 */
fun parseCoordinates(latlong: String): Coordinates? {
    val values = latlong.split(",")
    if (values.count() != 2) return null

    val (latitude, longitude) = values.map { it.toDouble() }
    return Coordinates(latitude, longitude)
}
