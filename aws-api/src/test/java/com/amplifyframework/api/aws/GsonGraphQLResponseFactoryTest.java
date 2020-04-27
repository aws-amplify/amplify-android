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
import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for implementation of ResponseFactory.
 */
@RunWith(RobolectricTestRunner.class)
public final class GsonGraphQLResponseFactoryTest {

    private GraphQLResponse.Factory responseFactory;

    /**
     * Set up the object under test, a GsonGraphQLResponseFactory.
     */
    @Before
    public void setup() {
        responseFactory = new GsonGraphQLResponseFactory();
    }

    /**
     * Validates that response with null content does not break
     * the response object builder. Null data and/or error will
     * return a non-null response object with null data and/or
     * an empty list of errors.
     * @throws ApiException From API configuration
     */
    @Test
    public void nullDataNullErrorsReturnsEmptyResponseObject() throws ApiException {
        // Arrange some JSON string from a "server"
        final String nullResponseJson =
            Resources.readAsString("null-gql-response.json");

        // Act! Parse it into a model.
        final GraphQLResponse<ListTodosResult> response =
            responseFactory.buildSingleItemResponse(nullResponseJson, ListTodosResult.class);

        // Assert that the model is constructed without content
        assertNotNull(response);
        assertFalse(response.hasData());
        assertFalse(response.hasErrors());
    }

    /**
     * Validates that the converter is able to parse a partial GraphQL
     * response into a result. In this case, the result contains some
     * data, but also a list of errors.
     * @throws ApiException From API configuration
     */
    @Test
    public void partialResponseRendersWithTodoDataAndErrors() throws ApiException {
        // Arrange some JSON string from a "server"
        final String partialResponseJson =
            Resources.readAsString("partial-gql-response.json");

        // Act! Parse it into a model.
        final GraphQLResponse<ListTodosResult> response =
            responseFactory.buildSingleItemResponse(partialResponseJson, ListTodosResult.class);

        // Assert that the model contained things...
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getItems());

        // Assert that all of the fields of the different todos
        // match what we would expect from a manual inspection of the
        // JSON.
        final List<ListTodosResult.Todo> actualTodos = response.getData().getItems();

        final List<ListTodosResult.Todo> expectedTodos = Arrays.asList(
            ListTodosResult.Todo.builder()
                .id("fa1c21cc-0458-4bca-bcb1-101579fb85c7")
                .name(null)
                .description("Test")
                .build(),
            ListTodosResult.Todo.builder()
                .id("68bad242-dec5-415b-acb3-daee3b069ce5")
                .name(null)
                .description("Test")
                .build(),
            ListTodosResult.Todo.builder()
                .id("f64e2e9a-42ad-4455-b8ee-d1cfae7e9f01")
                .name(null)
                .description("Test")
                .build()
        );

        assertEquals(expectedTodos, actualTodos);

        // Assert that we parsed the errors successfully.
        assertNotNull(response.getErrors());

        final List<GraphQLResponse.Error> expectedErrors = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String message = "failed";
            List<GraphQLLocation> locations = Arrays.asList(
                    new GraphQLLocation(5, 7));
            List<GraphQLPathSegment> path = Arrays.asList(
                    new GraphQLPathSegment("listTodos"),
                    new GraphQLPathSegment("items"),
                    new GraphQLPathSegment(i),
                    new GraphQLPathSegment("name")
            );
            Map<String, Object> extensions = new HashMap<>();
            extensions.put("errorType", null);
            extensions.put("errorInfo", null);
            extensions.put("data", null);
            expectedErrors.add(new GraphQLResponse.Error(message, locations, path, extensions));
        }

        assertEquals(expectedErrors, response.getErrors());
    }

    /**
     * This tests the GsonErrorDeserializer.  The test JSON response has 4 errors, which are all in
     * different formats, but are expected to be parsed into the same resulting object:
     * 1. Error contains errorType, errorInfo, data (AppSync specific fields) at root level
     * 2. Error contains errorType, errorInfo, data inside extensions object
     * 3. Error contains errorType, errorInfo, data at root AND inside extensions (fields inside
     * extensions take precedence)
     * 4. Error contains errorType at root, and errorInfo, data inside extensions (all should be
     * merged into extensions)
     *
     * @throws ApiException From API configuration
     */
    @Test
    public void errorResponseDeserializesExtensionsMap() throws ApiException {
        // Arrange some JSON string from a "server"
        final String partialResponseJson =
                Resources.readAsString("error-extensions-gql-response.json");

        // Act! Parse it into a model.
        final GraphQLResponse<ListTodosResult> response =
                responseFactory.buildSingleItemResponse(partialResponseJson, ListTodosResult.class);

        // Build the expected response.
        String message = "Conflict resolver rejects mutation.";
        List<GraphQLLocation> locations = Arrays.asList(
                new GraphQLLocation(11, 3));
        List<GraphQLPathSegment> path = Arrays.asList(
                new GraphQLPathSegment("listTodos"),
                new GraphQLPathSegment("items"),
                new GraphQLPathSegment(0),
                new GraphQLPathSegment("name")
        );

        Map<String, Object> data = new HashMap<>();
        data.put("id", "EF48518C-92EB-4F7A-A64E-D1B9325205CF");
        data.put("title", "new3");
        data.put("content", "Original content from DataStoreEndToEndTests at 2020-03-26 21:55:47 " +
                "+0000");
        data.put("_version", 2);

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorType", "ConflictUnhandled");
        extensions.put("errorInfo", null);
        extensions.put("data", data);

        GraphQLResponse.Error expectedError = new GraphQLResponse.Error(message, locations, path, extensions);
        GraphQLResponse<ListTodosResult> expectedResponse = new GraphQLResponse<>(null,
                Arrays.asList(expectedError, expectedError, expectedError, expectedError));

        // Assert that the response is expected
        assertEquals(expectedResponse, response);
    }

    /**
     * It is possible to cast the response data as a string, instead of as the strongly
     * modeled type, if you choose to do so.
     * @throws JSONException Shouldn't, but might while arranging test input
     * @throws ApiException From API configuration
     */
    @Test
    public void partialResponseCanBeRenderedAsStringType() throws JSONException, ApiException {
        // Arrange some known JSON response
        final JSONObject partialResponseJson = Resources.readAsJson("partial-gql-response.json");

        // Act! Parse it into a String data type.
        final GraphQLResponse<String> response =
                responseFactory.buildSingleItemResponse(partialResponseJson.toString(), String.class);

        // Assert that the response data is just the data block as a JSON string
        assertEquals(
                partialResponseJson.getJSONObject("data").getJSONObject("listTodos").toString(),
                response.getData()
        );
    }

    /**
     * The response to a base sync query must be resolvable by the response factory.
     * @throws ApiException From API configuration
     */
    @Test
    public void syncQueryResponseCanBeRenderedAsStringType() throws ApiException {
        final JSONObject baseQueryResponseJson =
            Resources.readAsJson("base-sync-posts-response.json");

        final Iterable<String> queryResults =
            responseFactory.buildSingleArrayResponse(baseQueryResponseJson.toString(), String.class)
                .getData();

        final List<String> resultJsons = new ArrayList<>();
        for (final String queryResult : queryResults) {
            resultJsons.add(queryResult);
        }

        assertEquals(
            Resources.readLines("base-sync-posts-response-items.json"),
            resultJsons
        );
    }
}
