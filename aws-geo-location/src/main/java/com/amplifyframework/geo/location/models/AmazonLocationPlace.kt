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

package com.amplifyframework.geo.location.models

import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.Place
import com.amazonaws.services.geo.model.Place as AmazonPlace

/**
 * Specialized [Place] instance that can hold metadata returned
 * by the Amazon Location Service Places API.
 *
 * @see [API Place](https://docs.aws.amazon.com/location-places/latest/APIReference/API_Place.html)
 */
data class AmazonLocationPlace(
    val coordinates: Coordinates,
    val label: String? = null,
    val addressNumber: String? = null,
    val street: String? = null,
    val country: String? = null,
    val region: String? = null,
    val subRegion: String? = null,
    val municipality: String? = null,
    val neighborhood: String? = null,
    val postalCode: String? = null
) : Place(coordinates) {
    internal constructor(place: AmazonPlace) : this(
        // Amazon Location Service represents 2d point as [long, lat]
        Coordinates(place.geometry.point[1],  // latitude
                    place.geometry.point[0]), // longitude
        place.label,
        place.addressNumber,
        place.street,
        place.country,
        place.region,
        place.subRegion,
        place.municipality,
        place.neighborhood,
        place.postalCode
    )
}