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
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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
        Gson gson = GsonFactory.instance();
        responseFactory = new GsonGraphQLResponseFactory(gson);
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
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
            responseFactory.buildResponse(request, nullResponseJson);

        // Assert that the model is constructed without content
        assertNotNull(response);
        assertFalse(response.hasData());
        assertFalse(response.hasErrors());
        assertEquals(new ArrayList<>(), response.getErrors());
    }

    /**
     * Validates that a response with non-Json content throws an
     * ApiException instead of a RuntimeException.
     * @throws ApiException From API configuration
     */
    @Test
    public void nonJsonResponseThrowsApiException() throws ApiException {
        // Arrange some non-JSON string from a "server"
        final String nonJsonResponse =
            Resources.readAsString("non-json-gql-response.json");

        // Act! Parse it into a model.
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);

        // Assert that the appropriate exception is thrown
        assertThrows(ApiException.class, () -> {
            responseFactory.buildResponse(request, nonJsonResponse);
        });
    }

    /**
     * Validates that an empty response throws an
     * ApiException instead of returning a null reference.
     * @throws ApiException From API configuration
     */
    @Test
    public void emptyResponseThrowsApiException() throws ApiException {
        // Arrange some empty string from a "server"
        final String emptyResponse = "";

        // Act! Parse it into a model.
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);

        // Assert that the appropriate exception is thrown
        assertThrows(ApiException.class, () -> {
            responseFactory.buildResponse(request, emptyResponse);
        });
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
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
            responseFactory.buildResponse(request, partialResponseJson);

        // Assert that the model contained things...
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getItems());

        // Assert that all of the fields of the different todos
        // match what we would expect from a manual inspection of the
        // JSON.
        final Iterable<Todo> actualTodos = response.getData().getItems();

        final List<Todo> expectedTodos = Arrays.asList(
            Todo.builder()
                .id("fa1c21cc-0458-4bca-bcb1-101579fb85c7")
                .name(null)
                .description("Test")
                .build(),
            Todo.builder()
                .id("68bad242-dec5-415b-acb3-daee3b069ce5")
                .name(null)
                .description("Test")
                .build(),
            Todo.builder()
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
            List<GraphQLLocation> locations = Collections.singletonList(
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
     * Validates that the converter is able to parse a partial GraphQL
     * response into a result. In this case, the result contains some
     * data, but also a list of errors.
     * @throws AmplifyException From API configuration
     */
    @Test
    public void responseRendersAsPaginatedResult() throws AmplifyException {
        // Expect
        final List<Todo> expectedTodos = Arrays.asList(
                Todo.builder()
                        .id("fa1c21cc-0458-4bca-bcb1-101579fb85c7")
                        .name(null)
                        .description("Test")
                        .build(),
                Todo.builder()
                        .id("68bad242-dec5-415b-acb3-daee3b069ce5")
                        .name(null)
                        .description("Test")
                        .build(),
                Todo.builder()
                        .id("f64e2e9a-42ad-4455-b8ee-d1cfae7e9f01")
                        .name(null)
                        .description("Test")
                        .build()
        );

        String nextToken = "eyJ2ZXJzaW9uIjoyLCJ0b2tlbiI6IkFRSUNBSGg5OUIvN3BjWU41eE96NDZJMW5GeGM4";
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        AppSyncGraphQLRequest<PaginatedResult<Todo>> expectedRequest = buildDummyRequest(responseType);
        expectedRequest = expectedRequest.newBuilder().variable("nextToken", "String", nextToken).build();
        final PaginatedResult<Todo> expectedPaginatedResult = new PaginatedResult<>(expectedTodos, expectedRequest);

        final List<GraphQLResponse.Error> expectedErrors = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String message = "failed";
            List<GraphQLLocation> locations = Collections.singletonList(
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

        final GraphQLResponse<PaginatedResult<Todo>> expectedResponse =
                new GraphQLResponse<>(expectedPaginatedResult, expectedErrors);

        // Act
        final String partialResponseJson = Resources.readAsString("partial-gql-response.json");

        final GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
                responseFactory.buildResponse(request, partialResponseJson);

        // Assert
        assertEquals(expectedResponse, response);
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
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
                responseFactory.buildResponse(request, partialResponseJson);

        // Build the expected response.
        String message = "Conflict resolver rejects mutation.";
        List<GraphQLLocation> locations = Collections.singletonList(
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
        GraphQLResponse<PaginatedResult<Todo>> expectedResponse = new GraphQLResponse<>(null,
                Arrays.asList(expectedError, expectedError, expectedError, expectedError));

        // Assert that the response is expected
        assertEquals(expectedResponse, response);
    }

    /**
     * If an {@link GraphQLResponse} contains a non-null {@link GraphQLResponse.Error},
     * and if that error object itself contains some null-valued fields, the response factory
     * should be resilient to this, and continue to render a response, anyway, without
     * throwing an exception over the issue.
     * @throws ApiException On failure to build a response, perhaps because the null
     *                      valued items inside of the {@link GraphQLResponse.Error}
     *                      could not be parsed
     */
    @Test
    public void errorWithNullFieldsCanBeParsed() throws ApiException {
        // Arrange some JSON string from a "server"
        final String responseJson = Resources.readAsString("error-null-properties.json");

        // Act! Parse it into a model.
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
                responseFactory.buildResponse(request, responseJson);

        // Build the expected response.
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorType", null);
        extensions.put("errorInfo", null);
        extensions.put("data", null);

        GraphQLResponse.Error expectedError =
            new GraphQLResponse.Error("the message", null, null, extensions);
        GraphQLResponse<PaginatedResult<Todo>> expectedResponse =
            new GraphQLResponse<>(null, Collections.singletonList(expectedError));

        // Assert that the response is expected
        assertEquals(expectedResponse, response);
    }

    /**
     * If an {@link GraphQLResponse} contains a non-null {@link GraphQLResponse.Error},
     * and if that error object "message" as a null, the response factory
     * should be resilient to this, and continue to render a response, anyway, without
     * throwing an exception over the issue but adding a default message notifying that the
     * message was null or missing.
     * @throws ApiException On failure to build a response, perhaps because the null
     *                      valued items inside of the {@link GraphQLResponse.Error}
     *                      could not be parsed
     */
    @Test
    public void errorWithNullMessageCanBeParsed() throws ApiException {
        // Arrange some JSON string from a "server"
        final String responseJson = Resources.readAsString("error-null-message.json");

        // Act! Parse it into a model.
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Todo.class);
        GraphQLRequest<PaginatedResult<Todo>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Todo>> response =
                responseFactory.buildResponse(request, responseJson);

        // Build the expected response.
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorType", null);
        extensions.put("errorInfo", null);
        extensions.put("data", null);

        final String defaultMessage = "Message was null or missing while deserializing error";

        GraphQLResponse.Error expectedError =
                new GraphQLResponse.Error(defaultMessage, null, null, extensions);
        GraphQLResponse<PaginatedResult<Todo>> expectedResponse =
                new GraphQLResponse<>(null, Collections.singletonList(expectedError));

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

        GraphQLRequest<String> request = buildDummyRequest(String.class);

        // Act! Parse it into a String data type.
        final GraphQLResponse<String> response =
                responseFactory.buildResponse(request, partialResponseJson.toString());

        // Assert that the response data is just the data block as a JSON string
        assertEquals(
                partialResponseJson.getJSONObject("data").toString(),
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

        Type responseType = TypeMaker.getParameterizedType(Iterable.class, String.class);
        GraphQLRequest<Iterable<String>> request = buildDummyRequest(responseType);
        GraphQLResponse<Iterable<String>> response =
                responseFactory.buildResponse(request, baseQueryResponseJson.toString());
        final Iterable<String> queryResults = response.getData();

        final List<String> resultJsons = new ArrayList<>();
        for (final String queryResult : queryResults) {
            resultJsons.add(queryResult);
        }

        assertEquals(
            Resources.readLines("base-sync-posts-response-items.json"),
            resultJsons
        );
    }

    /**
     * {@link Temporal.Date}, {@link Temporal.DateTime}, {@link Temporal.Time}, and {@link Temporal.Timestamp} all
     * have different JSON representations. It must be possible to recover the Java type which
     * models the JSON representation of each.
     * @throws ApiException If the response factory fails to construct a response,
     *                      perhaps because deserialization to one of these types
     *                      has failed.
     */
    @Test
    public void awsDateTypesCanBeDeserialized() throws ApiException {
        // Expect
        List<Meeting> expectedMeetings = Arrays.asList(
            Meeting.builder()
                .name("meeting0")
                .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
                .date(new Temporal.Date("2001-02-03"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30Z"))
                .time(new Temporal.Time("01:22"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build(),
            Meeting.builder()
                .name("meeting1")
                .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
                .date(new Temporal.Date("2001-02-03"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15Z"))
                .time(new Temporal.Time("01:22:33"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build(),
            Meeting.builder()
                .name("meeting2")
                .id("7a3d5d76-667e-4714-a882-8c8e00a6ffc9")
                .date(new Temporal.Date("2001-02-03Z"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15.444Z"))
                .time(new Temporal.Time("01:22:33.444"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build(),
            Meeting.builder()
                .name("meeting3")
                .id("3a880283-5402-4ad7-bc41-052ca6edeba8")
                .date(new Temporal.Date("2001-02-03+01:30"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15.444+05:30"))
                .time(new Temporal.Time("01:22:33.444Z"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build(),
            Meeting.builder()
                .name("meeting4")
                .id("5dfc35eb-f75a-4848-9655-9b8ca813b74d")
                .date(new Temporal.Date("2001-02-03+01:30:15"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15.444+05:30:15"))
                .time(new Temporal.Time("01:22:33.444+05:30"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build(),
            Meeting.builder()
                .name("meeting5")
                .id("3ce161af-14e7-4880-843b-921838efdc9d")
                .date(new Temporal.Date("2001-02-03+01:30:15"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15.444+05:30:15"))
                .time(new Temporal.Time("01:22:33.444+05:30:15"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build()
        );

        // Act
        final String responseString = Resources.readAsString("list-meetings-response.json");

        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, Meeting.class);
        GraphQLRequest<PaginatedResult<Meeting>> request = buildDummyRequest(responseType);
        final GraphQLResponse<PaginatedResult<Meeting>> response =
                responseFactory.buildResponse(request, responseString);
        final Iterable<Meeting> actualMeetings = response.getData().getItems();

        // Assert
        assertEquals(expectedMeetings, actualMeetings);
    }

    private <T> AppSyncGraphQLRequest<T> buildDummyRequest(Type responseType) throws ApiException {
        try {
            return AppSyncGraphQLRequest.builder()
                    .modelClass(Todo.class)
                    .operation(QueryType.LIST)
                    .requestOptions(new ApiGraphQLRequestOptions())
                    .responseType(responseType)
                    .build();
        } catch (AmplifyException exception) {
            throw new ApiException("Failed to build AppSyncGraphQLRequest", exception,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
        }
    }
}
