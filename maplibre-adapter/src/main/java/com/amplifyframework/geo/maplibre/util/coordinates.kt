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
