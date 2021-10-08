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

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that [SearchIndicesConfiguration] configuration behaves as expected.
 */
@RunWith(RobolectricTestRunner::class)
class SearchIndicesConfigurationTest {
    /**
     * Tests that [SearchIndicesConfiguration] object can be built from a JSON.
     */
    @Test
    fun searchIndicesConfigurationBuildsFromJson() {
        val json = JSONObject()
            .put("items", JSONArray()
                .put("search_index_A")
                .put("search_index_B"))
            .put("default", "search_index_B")
        val config = SearchIndicesConfiguration.fromJson(json).build()
        val expected = SearchIndicesConfiguration.builder()
            .items(setOf("search_index_A", "search_index_B"))
            .default("search_index_B")
            .build()
        assertEquals(expected, config)
    }

    /**
     * Tests that [SearchIndicesConfiguration] object can be built from a JSON where
     * "default" search index is not explicit.
     */
    @Test
    fun defaultIsNotExplicitlyRequired() {
        val json = JSONObject()
            .put("items", JSONArray()
                .put("search_index_A"))
        val config = SearchIndicesConfiguration.fromJson(json).build()
        val expected = SearchIndicesConfiguration.builder()
            .items(setOf("search_index_A"))
            .default("search_index_A")
            .build()
        assertEquals(expected, config)
    }

    /**
     * Tests that [SearchIndicesConfiguration] defaults to first search index in the collection
     * if not explicitly given.
     */
    @Test
    fun defaultsToFirstSearchIndex() {
        val searchIndices = ArrayList<String>()
        for (searchIndex in 0..4) {
            searchIndices.add("search_index_$searchIndex")
        }
        val config = SearchIndicesConfiguration.builder()
            .items(searchIndices)
            .build()
        assertEquals(searchIndices.first(), config.default)
    }

    /**
     * Tests that [SearchIndicesConfiguration] fails to build if no search index is given.
     * @throws NoSuchElementException when no search index is given.
     */
    @Test(expected = NoSuchElementException::class)
    fun atLeastOneSearchIndexIsRequired() {
        val json = JSONObject()
            .put("items", JSONArray())
            .put("default", "search_index_A")
        SearchIndicesConfiguration.fromJson(json).build()
    }
}