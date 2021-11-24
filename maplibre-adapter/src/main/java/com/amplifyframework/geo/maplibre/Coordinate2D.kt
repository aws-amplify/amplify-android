package com.amplifyframework.geo.maplibre

import android.location.Location
import android.os.Parcelable
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coordinate2D(
    val longitude: Double = 0.0,
    val latitude: Double = 0.0
) : Parcelable {
    val location: Location
        get() = Location("Amplify.Geo").apply {
            longitude = this@Coordinate2D.longitude
            latitude = this@Coordinate2D.latitude
        }

    val latlng: LatLng
        get() = LatLng(
            this@Coordinate2D.latitude,
            this@Coordinate2D.longitude
        )
}

val Location.coordinates: Coordinate2D
    get() = Coordinate2D(longitude = this.longitude, latitude = this.latitude)
