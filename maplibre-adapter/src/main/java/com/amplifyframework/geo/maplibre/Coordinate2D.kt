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

package com.amplifyframework.geo.maplibre

import android.annotation.SuppressLint
import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.maplibre.android.geometry.LatLng

@SuppressLint("ParcelCreator")
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
