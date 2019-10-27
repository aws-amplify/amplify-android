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

import com.amplifyframework.api.Resources;
import com.amplifyframework.api.ResponseFactory;
import com.amplifyframework.api.graphql.GraphQLResponse;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for implementation of ResponseFactory.
 */
public final class GsonResponseFactoryTest {

    private ResponseFactory responseFactory;

    /**
     * Set up the object under test, a GsonResponseFactory.
     */
    @Before
    public void setup() {
        responseFactory = new GsonResponseFactory();
    }

    /**
     * Validates that response with null content does not break
     * the response object builder. Null data and/or error will
     * return a non-null response object with null data and/or
     * an empty list of errors.
     */
    @Test
    public void nullDataNullErrorsReturnsEmptyResponseObject() {
        // Arrange some JSON string from a "server"
        final String nullResponseJson =
                Resources.readAsString("null-gql-response.json");

        // Act! Parse it into a model.
        final GraphQLResponse<ListTodosResult> response = (GraphQLResponse<ListTodosResult>)
                responseFactory.buildResponse(nullResponseJson, ListTodosResult.class);

        // Assert that the model is constructed without content
        assertNotNull(response);
        assertFalse(response.hasData());
        assertFalse(response.hasErrors());
    }

    /**
     * Validates that the converter is able to parse a partial GraphQL
     * response into a result. In this case, the result contains some
     * data, but also a list of errors.
     */
    @Test
    public void partialResponseRendersWithTodoDataAndErrors() {
        // Arrange some JSON string from a "server"
        final String partialResponseJson =
            Resources.readAsString("partial-gql-response.json");

        // Act! Parse it into a model.
        final GraphQLResponse<ListTodosResult> response = (GraphQLResponse<ListTodosResult>)
            responseFactory.buildResponse(partialResponseJson, ListTodosResult.class);

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

        final List<GraphQLResponse.Error> expectedErrors = Arrays.asList(
            new GraphQLResponse.Error("failed"),
            new GraphQLResponse.Error("failed"),
            new GraphQLResponse.Error("failed")
        );

        final List<GraphQLResponse.Error> actualErrors = response.getErrors();

        assertEquals(expectedErrors, actualErrors);
    }
}

