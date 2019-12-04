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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.testutils.Resources;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AWSApiPlugin#getSelectedApiName(EndpointType)} selector method.
 */
@RunWith(RobolectricTestRunner.class)
public final class ApiSelectorTest {

    /**
     * Selects the correct API name based on configuration
     * that contains only one of each API endpoint type.
     * @throws ApiException From plugin configuration failure
     */
    @Test
    public void testApiSelectorForSingleApi() throws ApiException {
        // Arrange an input JSONObject
        final JSONObject json = Resources.readAsJson("single-gql-single-rest-api.config");

        AWSApiPlugin plugin = new AWSApiPlugin();
        plugin.configure(json, null);
        assertEquals("api1", plugin.getSelectedApiName(EndpointType.GRAPHQL));
        assertEquals("api2", plugin.getSelectedApiName(EndpointType.REST));
    }

    /**
     * Test that runtime exception is thrown if configuration
     * contains more than one API of invoked endpoint type.
     * @throws ApiException From plugin configuration failure
     */
    @Test(expected = ApiException.class)
    public void testApiSelectorForMultiApi() throws ApiException {
        // Arrange an input JSONObject
        final JSONObject json = Resources.readAsJson("multi-gql-zero-rest-api.config");

        AWSApiPlugin plugin = new AWSApiPlugin();
        plugin.configure(json, null);
        plugin.getSelectedApiName(EndpointType.GRAPHQL);
    }

    /**
     * Test that runtime exception is thrown if configuration
     * contains no API of invoked endpoint type.
     * @throws ApiException From plugin configuration failure
     */
    @Test(expected = ApiException.class)
    public void testApiSelectorForZeroApi() throws ApiException {
        // Arrange an input JSONObject
        final JSONObject json = Resources.readAsJson("multi-gql-zero-rest-api.config");

        AWSApiPlugin plugin = new AWSApiPlugin();
        plugin.configure(json, null);
        plugin.getSelectedApiName(EndpointType.REST);
    }
}
