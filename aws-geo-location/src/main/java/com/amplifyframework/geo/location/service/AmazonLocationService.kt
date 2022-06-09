/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.GetMapStyleDescriptorRequest
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForPositionRequest
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForTextRequest
import aws.smithy.kotlin.runtime.ServiceException
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.CountryCode
import com.amplifyframework.geo.models.Place
import com.amplifyframework.geo.models.SearchArea

/**
 * Implements the backend provider for the location plugin using
 * AWS Mobile SDK's [LocationClient].
 * @param credentialsProvider AWS credentials provider for authorizing API calls
 * @param region AWS region for the Amazon Location Service
 */
internal class AmazonLocationService(
    credentialsProvider: CredentialsProvider,
    region: String
) : GeoService<LocationClient> {
    override val provider: LocationClient

    init {
        provider = LocationClient.invoke {
            this.credentialsProvider = credentialsProvider
            this.region = region
        }
    }

    override suspend fun getStyleJson(mapName: String): String {
        val request = GetMapStyleDescriptorRequest.invoke {
            this.mapName = mapName
        }
        val response = provider.getMapStyleDescriptor(request)
        return response.blob?.decodeToString() ?: throw ServiceException()
    }

    override suspend fun geocode(
        index: String,
        query: String,
        limit: Int,
        area: SearchArea?,
        countries: List<CountryCode>
    ): List<Place> {
        val request = SearchPlaceIndexForTextRequest.invoke {
            indexName = index
            text = query
            maxResults = limit
            filterCountries = countries.map { it.name }
            filterBBox = area?.boundingBox?.let {
                listOf(it.longitudeSW, it.latitudeSW, it.longitudeNE, it.latitudeNE)
            }
            biasPosition = area?.biasPosition?.let { listOf(it.longitude, it.latitude) }
        }

        val response = provider.searchPlaceIndexForText(request)

        return response.results
            ?.mapNotNull { it.place }
            ?.map {
                AmazonLocationPlace(it)
            } ?: listOf()
    }

    override suspend fun reverseGeocode(
        index: String,
        position: Coordinates,
        limit: Int
    ): List<Place> {
        val request = SearchPlaceIndexForPositionRequest.invoke {
            this.position = listOf(position.longitude, position.latitude)
            indexName = index
            maxResults = limit
        }
        val response = provider.searchPlaceIndexForPosition(request)

        return response.results
            ?.mapNotNull { it.place }
            ?.map {
                AmazonLocationPlace(it)
            } ?: listOf()
    }

}
