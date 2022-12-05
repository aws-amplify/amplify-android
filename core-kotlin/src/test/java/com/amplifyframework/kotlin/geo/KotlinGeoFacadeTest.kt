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

import com.amplifyframework.core.Consumer
import com.amplifyframework.geo.GeoCategoryBehavior
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.result.GeoSearchResult
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertSame
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Unit tests for the [KotlinGeoFacade] class.
 */
internal class KotlinGeoFacadeTest {
    private val delegate = mockk<GeoCategoryBehavior>()
    private val geo = KotlinGeoFacade(delegate)

    @Test
    fun `gets available maps`() = runBlocking {
        val maps = listOf(
            MapStyle("a", "b"),
            MapStyle("c", "d")
        )
        every { delegate.getAvailableMaps(any(), any()) } answers {
            val callback = firstArg<Consumer<Collection<MapStyle>>>()
            callback.accept(maps)
        }
        val result = geo.getAvailableMaps()
        assertSame(maps, result)
    }

    @Test(expected = GeoException::class)
    fun `throws available map error`(): Unit = runBlocking {
        val error = GeoException("message", "suggestion")
        every { delegate.getAvailableMaps(any(), any()) } answers {
            val callback = secondArg<Consumer<GeoException>>()
            callback.accept(error)
        }
        geo.getAvailableMaps()
    }

    @Test
    fun `gets default map`() = runBlocking {
        val map = MapStyle("name", "style")
        every { delegate.getDefaultMap(any(), any()) } answers {
            val callback = firstArg<Consumer<MapStyle>>()
            callback.accept(map)
        }
        val result = geo.getDefaultMap()
        assertSame(map, result)
    }

    @Test(expected = GeoException::class)
    fun `throws default map error`(): Unit = runBlocking {
        val error = GeoException("message", "suggestion")
        every { delegate.getDefaultMap(any(), any()) } answers {
            val callback = secondArg<Consumer<GeoException>>()
            callback.accept(error)
        }
        geo.getDefaultMap()
    }

    @Test
    fun `returns map style descriptor`() = runBlocking {
        val descriptor = MapStyleDescriptor("")
        every { delegate.getMapStyleDescriptor(any(), any(), any()) } answers {
            val callback = secondArg<Consumer<MapStyleDescriptor>>()
            callback.accept(descriptor)
        }
        val result = geo.getMapStyleDescriptor()
        assertSame(descriptor, result)
    }

    @Test(expected = GeoException::class)
    fun `throws map style descriptor error`(): Unit = runBlocking {
        val error = GeoException("message", "suggestion")
        every { delegate.getMapStyleDescriptor(any(), any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }
        geo.getMapStyleDescriptor()
    }

    @Test
    fun `returns search by text result`() = runBlocking {
        val query = "query"
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByText(query, any(), any(), any()) } answers {
            val callback = thirdArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }
        val result = geo.searchByText(query)
        assertSame(searchResult, result)
    }

    @Test(expected = GeoException::class)
    fun `throws search by text error`(): Unit = runBlocking {
        val query = "query"
        val error = GeoException("message", "suggestion")
        every { delegate.searchByText(query, any(), any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }
        geo.searchByText(query)
    }

    @Test
    fun `returns search by coordinates result`() = runBlocking {
        val position = Coordinates()
        val searchResult = GeoSearchResult.withPlaces(emptyList())
        every { delegate.searchByCoordinates(position, any(), any(), any()) } answers {
            val callback = thirdArg<Consumer<GeoSearchResult>>()
            callback.accept(searchResult)
        }
        val result = geo.searchByCoordinates(position)
        assertSame(searchResult, result)
    }

    @Test(expected = GeoException::class)
    fun `throws search by coordinates error`(): Unit = runBlocking {
        val position = Coordinates()
        val error = GeoException("message", "suggestion")
        every { delegate.searchByCoordinates(position, any(), any(), any()) } answers {
            val callback = lastArg<Consumer<GeoException>>()
            callback.accept(error)
        }
        geo.searchByCoordinates(position)
    }
}
