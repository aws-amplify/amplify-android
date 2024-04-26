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

import com.amplifyframework.geo.GeoException
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
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

    @Test
    fun `configuration can be built from amplify outputs`() {
        val data = amplifyOutputsData {
            geo {
                awsRegion = "test-region"
                maps {
                    map("other-map", "other-style")
                    map("default-map", "default-style")
                    default = "default-map"
                }
                searchIndices {
                    items += listOf("default-search-index", "other-search-index")
                    default = "default-search-index"
                }
            }
        }

        val configuration = GeoConfiguration.from(data)

        configuration.region shouldBe "test-region"

        configuration.maps?.shouldNotBeNull()
        configuration.maps?.run {
            default should {
                it.mapName shouldBe "default-map"
                it.style shouldBe "default-style"
            }

            items shouldMatchEach listOf(
                {
                    it.mapName shouldBe "other-map"
                    it.style shouldBe "other-style"
                },
                {
                    it.mapName shouldBe "default-map"
                    it.style shouldBe "default-style"
                }
            )
        }

        configuration.searchIndices?.shouldNotBeNull()
        configuration.searchIndices?.run {
            items shouldContainExactly listOf("default-search-index", "other-search-index")
            default shouldBe "default-search-index"
        }
    }

    @Test
    fun `configures with minimal amplify outputs`() {
        val data = amplifyOutputsData {
            geo {
                awsRegion = "test-region"
            }
        }

        val configuration = GeoConfiguration.from(data)

        configuration.region shouldBe "test-region"
    }

    @Test
    fun `throws if missing geo configuration`() {
        val data = amplifyOutputsData {
            // missing geo configuration
        }

        shouldThrow<GeoException> {
            GeoConfiguration.from(data)
        }
    }

    @Test
    fun `throws if maps configuration is inconsistent`() {
        val data = amplifyOutputsData {
            geo {
                maps {
                    map("name1", "style1")
                    default = "map2" // default does not exist in items
                }
            }
        }

        shouldThrow<GeoException> {
            GeoConfiguration.from(data)
        }
    }

    @Test
    fun `throws if search indices configuration is inconsistent`() {
        val data = amplifyOutputsData {
            geo {
                searchIndices {
                    items += "test1"
                    default = "test2" // default does not exist in items
                }
            }
        }

        shouldThrow<GeoException> {
            GeoConfiguration.from(data)
        }
    }
}
