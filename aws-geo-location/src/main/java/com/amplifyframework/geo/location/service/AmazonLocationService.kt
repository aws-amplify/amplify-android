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

package com.amplifyframework.geo.location.service

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.GetMapStyleDescriptorRequest
import com.amazonaws.services.geo.model.SearchPlaceIndexForPositionRequest
import com.amazonaws.services.geo.model.SearchPlaceIndexForTextRequest
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.CountryCode
import com.amplifyframework.geo.models.Place
import com.amplifyframework.geo.models.SearchArea
import com.amplifyframework.util.UserAgent

import java.nio.ByteBuffer

/**
 * Implements the backend provider for the location plugin using
 * AWS Mobile SDK's [AmazonLocationClient].
 * @param credentialsProvider AWS credentials provider for authorizing API calls
 * @param region AWS region for the Amazon Location Service
 */
internal class AmazonLocationService(
    credentialsProvider: AWSCredentialsProvider,
    region: String
) : GeoService<AmazonLocationClient> {
    override val provider: AmazonLocationClient

    init {
        val configuration = ClientConfiguration()
        configuration.userAgent = UserAgent.string()
        provider = AmazonLocationClient(credentialsProvider, configuration)
        provider.setRegion(Region.getRegion(region))
    }

    override fun getStyleJson(mapName: String): String {
        val request = GetMapStyleDescriptorRequest()
            .withMapName(mapName)
        val response = provider.getMapStyleDescriptor(request)
        return readFromBuffer(response.blob)
    }

    override fun geocode(
        index: String,
        query: String,
        limit: Int,
        area: SearchArea?,
        countries: List<CountryCode>
    ): List<Place> {
        val request = SearchPlaceIndexForTextRequest()
            .withIndexName(index)
            .withText(query)
            .withMaxResults(limit)
            .withFilterCountries(countries.map { it.name })
        if (area?.biasPosition != null) {
            val position = listOf(
                area.biasPosition!!.longitude,
                area.biasPosition!!.latitude
            )
            request.setBiasPosition(position)
        } else if (area?.boundingBox != null) {
            val boundary = listOf(
                area.boundingBox!!.longitudeSW,
                area.boundingBox!!.latitudeSW,
                area.boundingBox!!.longitudeNE,
                area.boundingBox!!.latitudeNE
            )
            request.setFilterBBox(boundary)
        }
        val response = provider.searchPlaceIndexForText(request)
        return response.results.map { AmazonLocationPlace(it.place) }
    }

    override fun reverseGeocode(index: String,
                                position: Coordinates,
                                limit: Int): List<Place> {
        val request = SearchPlaceIndexForPositionRequest()
            .withIndexName(index)
            .withPosition(listOf(position.longitude, position.latitude))
            .withMaxResults(limit)
        val response = provider.searchPlaceIndexForPosition(request)
        return response.results.map { AmazonLocationPlace(it.place) }
    }

    private fun readFromBuffer(buffer: ByteBuffer): String {
        return if (buffer.hasArray()) {
            val startIndex = buffer.arrayOffset() + buffer.position()
            val endIndex = startIndex + buffer.remaining()
            buffer.array().decodeToString(startIndex, endIndex)
        } else {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes.decodeToString()
        }
    }
}