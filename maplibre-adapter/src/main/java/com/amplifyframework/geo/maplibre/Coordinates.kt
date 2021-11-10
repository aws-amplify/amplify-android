package com.amplifyframework.geo.maplibre

import android.location.Location
import android.os.Parcelable
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coordinates(
    val longitude: Double = 0.0,
    val latitude: Double = 0.0
): Parcelable {
    val location: Location
        get() = Location("Amplify.Geo").apply {
            longitude = this@Coordinates.longitude
            latitude = this@Coordinates.latitude
        }

    val latlng: LatLng
        get() = LatLng(
            this@Coordinates.latitude,
            this@Coordinates.longitude
        )
}

val Location.coordinates: Coordinates
    get() = Coordinates(longitude = this.longitude, latitude = this.latitude)
