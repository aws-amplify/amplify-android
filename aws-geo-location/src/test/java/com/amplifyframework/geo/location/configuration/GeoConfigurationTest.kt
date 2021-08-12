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

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that [GeoConfiguration] configuration behaves as expected.
 */
@RunWith(RobolectricTestRunner::class)
class GeoConfigurationTest {
    /**
     * Tests that [GeoConfiguration] object can be built from a JSON.
     */
    @Test
    fun geoConfigurationBuildsFromJson() {
        val json = JSONObject()
            .put("region", "us-west-2")
        val config = GeoConfiguration.fromJson(json).build()
        val expected = GeoConfiguration.builder()
            .region("us-west-2")
            .build()
        assertEquals(expected, config)
    }
}