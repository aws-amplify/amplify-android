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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates the functionality of the {@link AWSApiPlugin} for REST operations.
 */
public final class RestApiInstrumentationTest {
    private static SynchronousApi api;

    /**
     * Configure the Amplify framework and auth.
     * @throws AmplifyException if configuration fails
     * @throws SynchronousMobileClient.MobileClientException If AWS Mobile Client initialization fails
     */
    @Before
    public void setUp() throws AmplifyException, SynchronousMobileClient.MobileClientException {
        ApiCategory asyncDelegate = TestApiCategory.fromConfiguration(R.raw.amplifyconfiguration);
        api = SynchronousApi.delegatingTo(asyncDelegate);
        SynchronousMobileClient mobileClient = SynchronousMobileClient.instance();
        mobileClient.initialize();
    }

    /**
     * Test whether we can make api Rest call in none auth.
     * @throws JSONException If JSON parsing of arranged data fails
     * @throws ApiException On failure to obtain a valid response from API endpoint
     */
    @Test
    public void getRequestWithNoAuth() throws JSONException, ApiException {
        final RestOptions options = RestOptions.builder()
            .addPath("/simplesuccess")
            .build();
        final RestResponse response = api.get("nonAuthApi", options);
        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method")
        );
        assertEquals(
                "Should return the right value",
                "/simplesuccess",
                contextJSON.getString("resource-path")
        );
    }

    /**
     * Test whether we can make POST api Rest call in none auth.
     * @throws ApiException On failure to obtain a valid response from API endpoint
     */
    @Test
    public void postRequestWithNoAuth() throws ApiException {
        final RestOptions options = RestOptions.builder()
            .addPath("/simplesuccess")
            .addBody("sample body".getBytes())
            .build();
        final RestResponse response = api.post("nonAuthApi", options);
        assertNotNull("Should return non-null data", response.getData());
        assertTrue("Response should be successful", response.getCode().isSuccessful());
    }

    /**
     * Test whether we can make api Rest call in api key as auth type.
     * @throws JSONException If JSON parsing of arranged data fails
     * @throws ApiException On failure to obtain a valid response from API endpoint
     */
    @Test
    public void getRequestWithApiKey() throws JSONException, ApiException {
        final RestOptions options = RestOptions.builder()
            .addPath("/simplesuccessapikey")
            .build();
        final RestResponse response = api.get("apiKeyApi", options);
        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method")
        );
        assertEquals(
                "Should return the right value",
                "/simplesuccessapikey",
                contextJSON.getString("resource-path")
        );
    }

    /**
     * Test whether we can make api Rest call in IAM as auth type.
     * @throws ApiException On failure to obtain a valid response from API endpoint
     */
    @Test
    @Ignore("Relies on an AWS account which is no longer active.  Resources need to be regenerated.")
    public void getRequestWithIAM() throws ApiException {
        final RestOptions options = RestOptions.builder()
            .addPath("/items")
            .addQueryParameters(Collections.singletonMap("key", "value"))
            .build();
        final RestResponse response = api.get("iamAuthApi", options);
        assertNotNull("Should return non-null data", response.getData());
        assertTrue("Response should be successful", response.getCode().isSuccessful());
    }

    /**
     * Test whether we can get failed response for access denied.
     * @throws ApiException On failure to obtain a valid response from API endpoint
     */
    @Test
    @Ignore("Relies on an AWS account which is no longer active.  Resources need to be regenerated.")
    public void getRequestWithIAMFailedAccess() throws ApiException {
        final RestOptions options = RestOptions.builder()
            .addPath("/invalidPath")
            .build();
        final RestResponse response = api.get("iamAuthApi", options);
        assertNotNull("Should return non-null data", response.getData());
        assertFalse("Response should be unsuccessful", response.getCode().isSuccessful());
    }
}
