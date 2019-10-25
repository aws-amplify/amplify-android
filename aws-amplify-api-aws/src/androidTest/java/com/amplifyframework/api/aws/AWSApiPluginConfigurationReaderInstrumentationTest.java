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

package com.amplifyframework.api.aws;

import com.amplifyframework.ConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AWSApiPluginConfigurationReader} JSON parser utility.
 */
public class AWSApiPluginConfigurationReaderInstrumentationTest {

    /**
     * An attempt to read a null JSON object should result in a
     * configuration exception.
     */
    @Test(expected = ConfigurationException.class)
    public void readFromNullJsonObjectThrowsConfigurationException() {
        AWSApiPluginConfigurationReader.readFrom(null);
    }

    /**
     * An attempt to read from an empty JSON object should result in a configuration
     * exception.
     * @throws JSONException On failure to arrange test inputs
     */
    @Test(expected = ConfigurationException.class)
    public void readFromApiWithNoSpecThrowsConfigurationException() throws JSONException {
        final JSONObject emptyApiSpec = new JSONObject().put("api1", new JSONObject());
        AWSApiPluginConfigurationReader.readFrom(emptyApiSpec);
    }

    /**
     * Validates that the configuration reader is able to parse a valid config
     * for a single API spec, returning a modeled object that resembles the input.
     * @throws JSONException On failure to arrange the test input
     */
    @Test
    public void readFromWellFormedJsonObjectProducesValidConfig() throws JSONException {

        // Arrange an input JSONObject
        final JSONObject json = new JSONObject(TestAssets.readAsString("single-api.config"));

        // Act: try to parse it to a modeled configuration object
        final AWSApiPluginConfiguration config = AWSApiPluginConfigurationReader.readFrom(json);

        // Assert: the modeled version "matches" the raw json
        assertNotNull(config);
        assertEquals(1, config.getApis().size());
        assertTrue(config.getApis().containsKey("api1"));
        assertEquals("https://www.foo.bar/baz", config.getApi("api1").getEndpoint());
        assertEquals("us-east-1", config.getApi("api1").getRegion());
    }
}
