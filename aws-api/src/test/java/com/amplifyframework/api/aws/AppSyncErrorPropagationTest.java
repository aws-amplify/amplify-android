/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.api.graphql.model.ModelSubscription;
import com.amplifyframework.testutils.Await;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for error cause propagation in GraphQL operations.
 * Tests that exception causes are properly preserved when API calls fail.
 */
@RunWith(RobolectricTestRunner.class)
public final class AppSyncErrorPropagationTest {
    private MockWebServer webServer;
    private HttpUrl baseUrl;
    private AWSApiPlugin plugin;

    /**
     * Sets up the test with a mock web server.
     * @throws IOException On failure to start web server
     * @throws JSONException On failure to arrange configuration JSON
     * @throws AmplifyException On failure to configure plugin
     */
    @Before
    public void setup() throws AmplifyException, IOException, JSONException {
        webServer = new MockWebServer();
        webServer.start(8080);
        baseUrl = webServer.url("/");

        JSONObject configuration = new JSONObject()
            .put("graphQlApi", new JSONObject()
                .put("endpointType", "GraphQL")
                .put("endpoint", baseUrl.url())
                .put("region", "us-east-1")
                .put("authorizationType", "API_KEY")
                .put("apiKey", "FAKE-API-KEY"));

        this.plugin = AWSApiPlugin.builder().build();
        this.plugin.configure(configuration, ApplicationProvider.getApplicationContext());
    }

    /**
     * Stop the {@link MockWebServer} that was started in {@link #setup()}.
     * @throws IOException On failure to shutdown the MockWebServer
     */
    @After
    public void cleanup() throws IOException {
        webServer.shutdown();
    }

    /**
     * Tests that query 401 errors preserve the GraphQL error response as the cause.
     * The cause should be a GraphQLResponseException containing the error details.
     */
    @Test
    public void queryUnauthorizedErrorPreservesCause() {
        // Arrange - mock a 401 response with GraphQL error payload
        String errorResponse = "{"
            + "\"errors\": [{"
            + "  \"errorType\": \"UnauthorizedException\","
            + "  \"message\": \"You are not authorized to make this call.\""
            + "}]"
            + "}";
        
        webServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody(errorResponse));

        // Act - execute query and capture error
        ApiException error = Await.<GraphQLResponse<PaginatedResult<Todo>>, ApiException>error((onResult, onError) ->
            plugin.query(ModelQuery.list(Todo.class), onResult, onError)
        );

        // Assert - verify error has proper cause chain
        assertNotNull("Error should not be null", error);
        assertEquals("Should be NonRetryableException", 
            ApiException.NonRetryableException.class, error.getClass());
        
        Throwable cause = error.getCause();
        assertNotNull("Error cause should not be null", cause);
        assertTrue("Cause should be GraphQLResponseException", 
            cause instanceof GraphQLResponseException);
        
        GraphQLResponseException graphQLError = (GraphQLResponseException) cause;
        assertEquals(1, graphQLError.getErrors().size());
        
        GraphQLResponseException.GraphQLError firstError = graphQLError.getErrors().get(0);
        assertEquals("UnauthorizedException", firstError.getErrorType());
        assertEquals("You are not authorized to make this call.", firstError.getMessage());
    }

    /**
     * Tests that query errors with invalid JSON fall back to IOException with JSONException cause.
     */
    @Test
    public void queryInvalidJsonPreservesJsonException() {
        // Arrange - mock a 400 response with invalid JSON
        String invalidJson = "not valid json at all";
        
        webServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody(invalidJson));

        // Act
        ApiException error = Await.<GraphQLResponse<PaginatedResult<Todo>>, ApiException>error((onResult, onError) ->
            plugin.query(ModelQuery.list(Todo.class), onResult, onError)
        );

        // Assert
        assertNotNull(error);
        Throwable cause = error.getCause();
        assertNotNull("Error cause should not be null", cause);
        assertTrue("Cause should be IOException", cause instanceof IOException);
        
        // The IOException should have a JSONException as its cause
        Throwable jsonCause = cause.getCause();
        assertNotNull("IOException should have JSONException as cause", jsonCause);
        assertTrue("Root cause should be JSONException", jsonCause instanceof org.json.JSONException);
    }
}
