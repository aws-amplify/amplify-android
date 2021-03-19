/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws.operation;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory;
import com.amplifyframework.api.rest.HttpMethod;
import com.amplifyframework.api.rest.RestOperationRequest;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.testutils.Await;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AWSRestOperation}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AWSRestOperationTest {
    private MockWebServer server;
    private HttpUrl baseUrl;
    private OkHttpClient client;
    private Map<String, String> responseHeaders;

    /**
     * Sets up a mock web server to serve fake responses.
     * @throws IOException while starting the mock web server
     * @throws JSONException on failure to arrange response JSON
     */
    @Before
    public void setup() throws IOException, JSONException {
        server = new MockWebServer();
        server.start(8080);
        baseUrl = server.url("/");

        MockResponse response = new MockResponse()
            .setResponseCode(200)
            .setBody(new JSONObject()
                .put("message", "thanks!")
                .toString()
            )
            .addHeader("foo", "bar")
            .addHeader("foo", "baz")
            .addHeader("qux", "quux");
        server.enqueue(response);

        client = new OkHttpClient();
    }

    /**
     * Stop the {@link MockWebServer} that was started in {@link #setup()}.
     * @throws IOException On failure to shutdown the MockWebServer
     */
    @After
    public void cleanup() throws IOException {
        server.shutdown();
    }

    /**
     * Tests the happy path, wherein the server returns a response, the
     * operation hasn't been canceled, and we get a {@link RestResponse}
     * at the end of it.
     * @throws ApiException
     *         A possible outcome of the operation. This is not
     *         expected, and would constitute a test failure.
     */
    @Test
    public void responseEmittedWhenOperationSucceeds() throws ApiException {
        ApiRequestDecoratorFactory apiRequestDecoratorFactory =
            new ApiRequestDecoratorFactory(ApiAuthProviders.noProviderOverrides(),
                                           AuthorizationType.NONE,
                                           "us-east-1");
        RestOperationRequest request =
            new RestOperationRequest(HttpMethod.GET,
                                     baseUrl.uri().getPath(),
                                     emptyMap(),
                                     emptyMap());
        RestResponse response = Await.<RestResponse, ApiException>result((onResult, onError) -> {
            AWSRestOperation operation =
                new AWSRestOperation(request,
                                     baseUrl.url().toString(),
                                     client,
                                     apiRequestDecoratorFactory,
                                     onResult,
                                     onError);
            operation.start();
        });
        assertTrue(response.getCode().isSuccessful());
        Map<String, String> expected = new HashMap<>();
        expected.put("foo", "bar,baz");
        expected.put("qux", "quux");
        expected.put("content-length", "21");
        assertEquals(expected, response.getHeaders());
    }

    /**
     * If the user calls {@link AWSRestOperation#cancel()}, then the operation
     * will not fire any callback. This behavior is consistent with iOS's REST operation.
     */
    @Test
    public void noErrorEmittedIfOperationIsCancelled() {
        ApiRequestDecoratorFactory apiRequestDecoratorFactory =
            new ApiRequestDecoratorFactory(ApiAuthProviders.noProviderOverrides(),
                                           AuthorizationType.NONE,
                                           "us-east-1");
        long timeToWaitForResponse = 300L;
        RestOperationRequest request =
            new RestOperationRequest(HttpMethod.GET,
                                     baseUrl.uri().getPath(),
                                     emptyMap(),
                                     emptyMap());
        assertTimedOut(() ->
            Await.<RestResponse, ApiException>result(timeToWaitForResponse, (onResult, onError) -> {
                AWSRestOperation operation =
                    new AWSRestOperation(request,
                                         baseUrl.url().toString(),
                                         client,
                                         apiRequestDecoratorFactory,
                                         onResult,
                                         onError);
                operation.start();
                operation.cancel();
            })
        );
    }

    private void assertTimedOut(Callable<RestResponse> action) {
        RuntimeException exception = assertThrows(RuntimeException.class, action::call);
        assertTrue(exception.getMessage().startsWith("Failed to count down latch"));
    }
}
