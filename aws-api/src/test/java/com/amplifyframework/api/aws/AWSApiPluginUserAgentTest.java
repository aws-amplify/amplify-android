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

package com.amplifyframework.api.aws;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.core.NoOpConsumer;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the user agent is applied as expected by the {@link UserAgentInterceptor}
 * in {@link AWSApiPlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AWSApiPluginUserAgentTest {
    // This was previously 200ms, but resulted in flaky tests because server.takeRequest would sometimes return null.
    private static final long REQUEST_TIMEOUT_SECONDS = 5;
    private static final String USER_AGENT_REGEX = "^(?<libraryName>.*?):(?<libraryVersion>.*?) " +
            "md/(?<deviceManufacturer>.*?)/(?<deviceName>.*?) " +
            "md/locale/(?<userLanguage>.*?)_(?<userRegion>.*?)$";
    private static final Pattern USER_AGENT_PATTERN = Pattern.compile(USER_AGENT_REGEX);

    private MockWebServer server;
    private AWSApiPlugin api;

    /**
     * Set up the mock web server and API that talks to it.
     * @throws Exception If setting up the server or API plugin fails
     */
    @Before
    public void setUp() throws Exception {
        // Set up backend
        server = new MockWebServer();
        server.start();

        // Set up the API
        api = new AWSApiPlugin();
        JSONObject config = new JSONObject()
                .put("name", new JSONObject()
                        .put("endpointType", "GraphQL")
                        .put("endpoint", getEndpoint())
                        .put("region", "")
                        .put("authorizationType", "API_KEY")
                        .put("apiKey", ""));
        api.configure(config, getApplicationContext());
    }

    /**
     * Shuts down the mock web server.
     * @throws IOException If closing the server encounters an error
     */
    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * Make an API request to the mock server and check the user agent
     * header format on the request. systemName and systemVersion not
     * required, as it is part of sdk user-agent string.
     * @throws Exception if API call fails or thread is interrupted while
     *          waiting for request
     */
    @Test
    @Config(sdk = 28)
    public void testUserAgentWithApi28() throws Exception {
        String userAgent = checkUserAgent();

        // Assert the correct format and content
        assertNotNull(userAgent);
        Matcher regexMatcher = USER_AGENT_PATTERN.matcher(userAgent);
        assertTrue(regexMatcher.matches());
        assertEquals("amplify-android", regexMatcher.group("libraryName"));
    }

    private String checkUserAgent() throws Exception {
        // Make a new query request
        GraphQLRequest<PaginatedResult<Todo>> listTodos = ModelQuery.list(Todo.class);
        api.query(listTodos, NoOpConsumer.create(), NoOpConsumer.create()); // Ignore result

        // Wait for server to receive the request and return user agent
        RecordedRequest request = server.takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(request);
        return request.getHeader("User-Agent");
    }

    private String getEndpoint() {
        return "http://" + server.getHostName() + ":" + server.getPort();
    }
}
