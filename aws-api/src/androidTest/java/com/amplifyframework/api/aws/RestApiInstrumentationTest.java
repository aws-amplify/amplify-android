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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.testutils.EmptyConsumer;
import com.amplifyframework.testutils.LatchedConsumer;

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

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     * @throws AmplifyException Exception is thrown if configuration fails.
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        AmplifyTestConfigurator.configureIfNotConfigured();
    }

    /**
     * Test whether we can make api Rest call in none auth.
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void getRequestWithNoAuth() throws JSONException {
        final RestOptions options = new RestOptions("simplesuccess");
        LatchedConsumer<RestResponse> responseConsumer = LatchedConsumer.instance();
        ResultListener<RestResponse> resultListener =
            ResultListener.instance(responseConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.get("nonAuthApi", options, resultListener);
        RestResponse response = responseConsumer.awaitValue();
        assertNotNull("Should return a non null data", response.getData());

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
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
        LatchedConsumer<RestResponse> responseConsumer = LatchedConsumer.instance();
        ResultListener<RestResponse> responseListener =
            ResultListener.instance(responseConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.post("nonAuthApi", options, responseListener);
        RestResponse response = responseConsumer.awaitValue();
        assertNotNull("Should return a non null data", response.getData());

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
        final RestOptions options = new RestOptions("simplesuccessapikey");
        LatchedConsumer<RestResponse> responseConsumer = LatchedConsumer.instance();
        ResultListener<RestResponse> responseListener =
            ResultListener.instance(responseConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.get("apiKeyApi", options, responseListener);
        RestResponse response = responseConsumer.awaitValue();
        assertNotNull("Should return a non null data", response.getData());

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
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
