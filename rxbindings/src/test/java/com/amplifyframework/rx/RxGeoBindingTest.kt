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

package com.amplifyframework.rx

import com.amplifyframework.core.Consumer
import com.amplifyframework.geo.GeoCategoryBehavior
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.geo.result.GeoSearchResult
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import org.junit.Test

private const val TIMEOUT_SECONDS = 2L

/**
 * Unit tests for the [RxGeoBinding] class
 */
class RxGeoBindingTest {
    private val delegate = mockk<GeoCategoryBehavior>()
    private val geo = RxGeoBinding(delegate)

    @Test
    fun `returns available maps`() {
        val maps = listOf(MapStyle("a", "b"), MapStyle("c", "d"))
        every { delegate.getAvailableMaps(any(), any()) } answers {
            val callback = firstArg<Consumer<Collection<MapStyle>>>()
            callback.accept(maps)
        }

        val observer = geo.availableMaps.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(maps)
    }

    @Test
    fun `returns error for available maps`() {
        val error = GeoException("message", "suggestion")
        every { delegate.getAvailableMaps(any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.availableMaps.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns default map`() {
        val map = MapStyle("map", "style")
        every { delegate.getDefaultMap(any(), any()) } answers {
            val callback = firstArg<Consumer<MapStyle>>()
            callback.accept(map)
        }

        val observer = geo.defaultMap.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(map)
    }

    @Test
    fun `returns error for default map`() {
        val error = GeoException("message", "suggestion")
        every { delegate.getDefaultMap(any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.defaultMap.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns map style descriptor`() {
        val descriptor = MapStyleDescriptor("")
        every { delegate.getMapStyleDescriptor(any(), any()) } answers {
            val callback = firstArg<Consumer<MapStyleDescriptor>>()
            callback.accept(descriptor)
        }

        val observer = geo.mapStyleDescriptor.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(descriptor)
    }

    @Test
    fun `returns map style descriptor with options`() {
        val options = GetMapStyleDescriptorOptions.builder().mapName("map").build()
        val descriptor = MapStyleDescriptor("")
        every { delegate.getMapStyleDescriptor(options, any(), any()) } answers {
            val callback = secondArg<Consumer<MapStyleDescriptor>>()
            callback.accept(descriptor)
        }

        val observer = geo.getMapStyleDescriptor(options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(descriptor)
    }

    @Test
    fun `returns error for map style descriptor`() {
        val error = GeoException("message", "suggestion")
        every { delegate.getMapStyleDescriptor(any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.mapStyleDescriptor.test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns error for map style descriptor with options`() {
        val error = GeoException("message", "suggestion")
        val options = GetMapStyleDescriptorOptions.builder().mapName("map").build()
        every { delegate.getMapStyleDescriptor(options, any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.getMapStyleDescriptor(options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns search by text`() {
        val query = "query"
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByText(query, any(), any()) } answers {
            val callback = secondArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }

        val observer = geo.searchByText(query).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(searchResult)
    }

    @Test
    fun `returns search by text with options`() {
        val query = "query"
        val options = GeoSearchByTextOptions.builder().maxResults(2).build()
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByText(query, options, any(), any()) } answers {
            val callback = thirdArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }

        val observer = geo.searchByText(query, options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(searchResult)
    }

    @Test
    fun `returns error for search by text`() {
        val query = "query"
        val error = GeoException("message", "suggestion")
        every { delegate.searchByText(query, any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.searchByText(query).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns error for search by text with options`() {
        val query = "query"
        val options = GeoSearchByTextOptions.builder().maxResults(5).build()
        val error = GeoException("message", "suggestion")
        every { delegate.searchByText(query, options, any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.searchByText(query, options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns search by coordinates`() {
        val coordinates = Coordinates(2.0, 5.0)
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByCoordinates(coordinates, any(), any()) } answers {
            val callback = secondArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }

        val observer = geo.searchByCoordinates(coordinates).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(searchResult)
    }

    @Test
    fun `returns search by coordinates with options`() {
        val coordinates = Coordinates(2.0, 5.0)
        val options = GeoSearchByCoordinatesOptions.builder().maxResults(6).build()
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByCoordinates(coordinates, options, any(), any()) } answers {
            val callback = thirdArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }

        val observer = geo.searchByCoordinates(coordinates, options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoErrors().assertValue(searchResult)
    }

    @Test
    fun `returns error for search by coordinates`() {
        val coordinates = Coordinates(3.0, 4.0)
        val error = GeoException("message", "suggestion")
        every { delegate.searchByCoordinates(coordinates, any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.searchByCoordinates(coordinates).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }

    @Test
    fun `returns error for search by coordinates with options`() {
        val coordinates = Coordinates(3.0, 4.0)
        val options = GeoSearchByCoordinatesOptions.builder().maxResults(6).build()
        val error = GeoException("message", "suggestion")
        every { delegate.searchByCoordinates(coordinates, options, any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }

        val observer = geo.searchByCoordinates(coordinates, options).test()
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        observer.assertNoValues().assertError(error)
    }
}
