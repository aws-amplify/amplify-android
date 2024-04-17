/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.aws.AWSApiPluginConfigurationReader.GEN2_API_NAME
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the [AWSApiPluginConfigurationReader] JSON parser utility.
 */
@RunWith(RobolectricTestRunner::class)
class AWSApiPluginConfigurationReaderTest {

    fun `read from null json object throws configuration exception`() {
        shouldThrow<ApiException> {
            AWSApiPluginConfigurationReader.readFrom(null)
        }
    }
    fun `read from api with no spec throws configuration exception`() {
        val emptyApiSpec = JSONObject().put("api1", JSONObject())

        shouldThrow<ApiException> {
            AWSApiPluginConfigurationReader.readFrom(emptyApiSpec)
        }
    }

    @Test
    fun `read from well formed json object produces valid config`() {
        // Arrange an input JSONObject
        val json = Resources.readAsJson("single-api.config")

        // Act: try to parse it to a modeled configuration object
        val config = AWSApiPluginConfigurationReader.readFrom(json)

        // Assert: the modeled version "matches" the raw json
        assertNotNull(config)
        assertEquals(1, config.apis.size.toLong())
        assertTrue(config.apis.containsKey("api1"))
        assertEquals(EndpointType.GRAPHQL, config.getApi("api1")!!.endpointType)
        assertEquals("https://www.foo.bar/baz", config.getApi("api1")!!.endpoint)
        assertEquals("us-east-1", config.getApi("api1")!!.region)
    }

    @Test
    fun `reads from AmplifyOutputsData`() {
        val outputs = amplifyOutputsData {
            data {
                awsRegion = "test-region"
                apiKey = "api-key"
                url = "https://aws.com"
                defaultAuthorizationType = AmplifyOutputsData.AwsAppsyncAuthorizationType.OPENID_CONNECT
            }
        }

        val config = AWSApiPluginConfigurationReader.from(outputs)

        config.apis shouldContainKey GEN2_API_NAME
        config.apis[GEN2_API_NAME]!!.run {
            region shouldBe "test-region"
            endpointType shouldBe EndpointType.GRAPHQL
            endpoint shouldBe "https://aws.com"
            authorizationType shouldBe AuthorizationType.OPENID_CONNECT
            apiKey shouldBe "api-key"
        }
    }

    @Test
    fun `apiKey can be null`() {
        val outputs = amplifyOutputsData {
            data {
                apiKey = null
            }
        }

        val config = AWSApiPluginConfigurationReader.from(outputs)

        config.apis shouldContainKey GEN2_API_NAME
        config.apis[GEN2_API_NAME]!!.run {
            apiKey.shouldBeNull()
        }
    }

    @Test
    fun `throws if no data config in AmplifyOutputsData`() {
        val outputs = amplifyOutputsData {
            // do not add data
        }

        shouldThrow<ApiException> {
            AWSApiPluginConfigurationReader.from(outputs)
        }
    }
}
