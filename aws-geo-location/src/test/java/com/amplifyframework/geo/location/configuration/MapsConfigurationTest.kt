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

package com.amplifyframework.geo.location.configuration

import com.amplifyframework.geo.models.MapStyle
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests that [MapsConfiguration] configuration behaves as expected.
 */
class MapsConfigurationTest {
    /**
     * Tests that [MapsConfiguration] object can be built from a JSON.
     */
    @Test
    fun mapsConfigurationBuildsFromJson() {
        val json = JSONObject()
            .put("items", JSONObject()
                .put("map_A", JSONObject()
                    .put("style", "style_A"))
                .put("map_B", JSONObject()
                    .put("style", "style_B")))
            .put("default", "map_B")
        val config = MapsConfiguration.fromJson(json).build()
        val expected = MapsConfiguration.builder()
            .items(setOf(
                MapStyle("map_A", "style_A"),
                MapStyle("map_B", "style_B")
            ))
            .default(MapStyle("map_B", "style_B"))
            .build()
        assertEquals(expected, config)
    }

    /**
     * Tests that [MapsConfiguration] object can be built from a JSON where
     * "default" map is not explicit.
     */
    @Test
    fun defaultIsNotExplicitlyRequired() {
        val json = JSONObject()
            .put("items", JSONObject()
                .put("map_A", JSONObject()
                    .put("style", "style_A")))
        val config = MapsConfiguration.fromJson(json).build()
        val expected = MapsConfiguration.builder()
            .items(setOf(MapStyle("map_A", "style_A")))
            .default(MapStyle("map_A", "style_A"))
            .build()
        assertEquals(expected, config)
    }

    /**
     * Tests that [MapsConfiguration] defaults to first map in the collection
     * if not explicitly given.
     */
    @Test
    fun defaultsToFirstMap() {
        val maps = ArrayList<MapStyle>()
        for (map in 0..4) {
            maps.add(MapStyle("map_$map", "style_$map"))
        }
        val config = MapsConfiguration.builder()
            .items(maps)
            .build()
        assertEquals(maps.first(), config.default)
    }

    /**
     * Tests that [MapsConfiguration] fails to build if no map is given.
     * @throws NoSuchElementException when no map is given.
     */
    @Test(expected = NoSuchElementException::class)
    fun atLeastOneMapIsRequired() {
        val json = JSONObject()
            .put("items", JSONObject())
            .put("default", "map_A")
        MapsConfiguration.fromJson(json).build()
    }
}