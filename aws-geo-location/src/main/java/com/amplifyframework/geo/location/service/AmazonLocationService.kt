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

import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.BatchDeleteDevicePositionHistoryRequest
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionRequest
import aws.sdk.kotlin.services.location.model.DevicePositionUpdate
import aws.sdk.kotlin.services.location.model.GetMapStyleDescriptorRequest
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForPositionRequest
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForTextRequest
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.CountryCode
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.models.Place
import com.amplifyframework.geo.models.SearchArea
import com.amplifyframework.geo.options.GeoUpdateLocationOptions

/**
 * Implements the backend provider for the location plugin using
 * AWS Kotlin SDK's [LocationClient].
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

    override suspend fun updateLocation(
        deviceId: String,
        position: GeoPosition,
        options: GeoUpdateLocationOptions,
    ) {
        updateLocations(deviceId, listOf(position), options)
    }

    override suspend fun updateLocations(
        deviceId: String,
        positions: List<GeoPosition>,
        options: GeoUpdateLocationOptions
    ) {
        val updateList: MutableList<DevicePositionUpdate> = mutableListOf()
        for (position in positions) {
            val devicePositionUpdate = DevicePositionUpdate.invoke {
                this.deviceId = deviceId
                // Amazon Location Service uses [longitude, latitude]
                this.position = listOf(position.location.longitude, position.location.latitude)
                this.sampleTime = Instant.fromEpochSeconds(position.timeStamp.time)
                this.positionProperties = options.positionProperties.properties
            }
            updateList.add(devicePositionUpdate)
        }

        val request = BatchUpdateDevicePositionRequest.invoke {
            trackerName = options.tracker
            updates = updateList
        }
        val response = provider.batchUpdateDevicePosition(request)
        if (!response.errors.isNullOrEmpty()) {
            response.errors?.first()?.error?.let {
                if (it.message != null) {
                    throw GeoException(it.message!!, "Please ensure that you have a stable internet connection.")
                } else {
                    throw ClientException()
                }
            }
        }
    }

    override suspend fun deleteLocationHistory(
        deviceId: String,
        tracker: String
    ) {
        val request = BatchDeleteDevicePositionHistoryRequest.invoke {
            trackerName = tracker
            deviceIds = listOf(deviceId)
        }
        val response = provider.batchDeleteDevicePositionHistory(request)
        if (!response.errors.isNullOrEmpty()) {
            response.errors?.first()?.error?.let {
                throw ServiceException(message = it.message)
            }
        }
    }
}
