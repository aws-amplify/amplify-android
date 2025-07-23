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

package com.amplifyframework.geo.maplibre.util

import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.models.Coordinates
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlin.reflect.KProperty1
import org.maplibre.android.plugins.annotation.Symbol

private fun <T> JsonObject.addStringProperty(ref: T, property: KProperty1<T, String?>) {
    val value = property.get(ref)
    if (value != null) {
        addProperty(property.name, value)
    } else {
        add(property.name, JsonNull.INSTANCE)
    }
}

private fun JsonElement.asOptionalString(): String? = if (isJsonNull) null else asString

fun JsonElement.toPlace(): AmazonLocationPlace {
    val obj = asJsonObject
    val coordinates = obj.getAsJsonObject(AmazonLocationPlace::coordinates.name)
    return AmazonLocationPlace(
        coordinates = Coordinates(
            coordinates["latitude"].asDouble,
            coordinates["longitude"].asDouble
        ),
        addressNumber = obj[AmazonLocationPlace::addressNumber.name].asOptionalString(),
        country = obj[AmazonLocationPlace::country.name].asOptionalString(),
        label = obj[AmazonLocationPlace::label.name].asOptionalString(),
        municipality = obj[AmazonLocationPlace::municipality.name].asOptionalString(),
        neighborhood = obj[AmazonLocationPlace::neighborhood.name].asOptionalString(),
        postalCode = obj[AmazonLocationPlace::postalCode.name].asOptionalString(),
        region = obj[AmazonLocationPlace::region.name].asOptionalString(),
        street = obj[AmazonLocationPlace::street.name].asOptionalString(),
        subRegion = obj[AmazonLocationPlace::subRegion.name].asOptionalString()
    )
}

fun AmazonLocationPlace.toJsonElement(): JsonElement {
    val place = JsonObject()
    place.addStringProperty(this, AmazonLocationPlace::addressNumber)
    place.addStringProperty(this, AmazonLocationPlace::country)
    place.addStringProperty(this, AmazonLocationPlace::label)
    place.addStringProperty(this, AmazonLocationPlace::municipality)
    place.addStringProperty(this, AmazonLocationPlace::neighborhood)
    place.addStringProperty(this, AmazonLocationPlace::postalCode)
    place.addStringProperty(this, AmazonLocationPlace::region)
    place.addStringProperty(this, AmazonLocationPlace::street)
    place.addStringProperty(this, AmazonLocationPlace::subRegion)
    place.add(
        AmazonLocationPlace::coordinates.name,
        JsonObject().apply {
            addProperty("latitude", coordinates.latitude)
            addProperty("longitude", coordinates.longitude)
        }
    )
    return place
}

fun Symbol.getPlaceData(): AmazonLocationPlace = data?.let {
    it.toPlace()
} ?: throw IllegalStateException("Symbol place data missing")
