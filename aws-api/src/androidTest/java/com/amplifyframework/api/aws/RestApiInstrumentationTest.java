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
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.testutils.SynchronousApi;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Validates the functionality of the {@link AWSApiPlugin} for REST operations.
 *
 */
public final class RestApiInstrumentationTest {

    private static SynchronousApi api;

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     * @throws AmplifyException Exception is thrown if configuration fails.
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        AmplifyTestConfigurator.configureIfNotConfigured();
        api = SynchronousApi.singleton();
    }

    /**
     * Test whether we can make api Rest call in none auth.
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void getRequestWithNoAuth() throws JSONException {
        RestResponse responseData =
            api.get("nonAuthApi", new RestOptions("simplesuccess"));

        final JSONObject contextJSON = responseData.getData().asJSONObject().getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccess",
                contextJSON.getString("resource-path"));
    }

    /**
     * Test whether we can make POST api Rest call in none auth.
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void postRequestWithNoAuth() throws JSONException {
        final RestOptions options = new RestOptions("simplesuccess", "sample body".getBytes());
        final RestResponse response = api.post("nonAuthApi", options);

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "POST",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccess",
                contextJSON.getString("resource-path"));
    }

    /**
     * Test whether we can make api Rest call in api key as auth type.
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void getRequestWithApiKey() throws JSONException {
        final RestResponse response =
            api.get("apiKeyApi", new RestOptions("simplesuccessapikey"));

        final JSONObject contextJSON = response.getData().asJSONObject().getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccessapikey",
                contextJSON.getString("resource-path"));
    }
}
