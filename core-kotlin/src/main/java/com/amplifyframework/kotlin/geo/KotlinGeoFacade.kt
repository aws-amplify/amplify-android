/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.kotlin.geo

import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategoryBehavior
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KotlinGeoFacade(private val delegate: GeoCategoryBehavior = Amplify.Geo) : Geo {
    override suspend fun getAvailableMaps(): Collection<MapStyle> = suspendCoroutine { continuation ->
        delegate.getAvailableMaps(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun getDefaultMap() = suspendCoroutine { continuation ->
        delegate.getDefaultMap(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun getMapStyleDescriptor(options: GetMapStyleDescriptorOptions) =
        suspendCoroutine { continuation ->
            delegate.getMapStyleDescriptor(
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun searchByText(
        query: String,
        options: GeoSearchByTextOptions
    ) = suspendCoroutine { continuation ->
        delegate.searchByText(
            query,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    override suspend fun searchByCoordinates(
        position: Coordinates,
        options: GeoSearchByCoordinatesOptions
    ) = suspendCoroutine { continuation ->
        delegate.searchByCoordinates(
            position,
            options,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }
}
