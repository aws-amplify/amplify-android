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

package com.amplifyframework.testutils.mocks;

import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

/**
 * Utility to mock behaviors of the API category.
 */
public final class ApiMocking {
    private ApiMocking() {}

    /**
     * Mock successful subscription start calls.
     * @param mockApi Mock object of type {@link GraphQLBehavior}
     */
    public static void mockSubscriptionStart(GraphQLBehavior mockApi) {
        Mockito.doAnswer(invocation -> {
            final int indexOfOnStart = 1;
            Consumer<String> onStart = invocation.getArgument(indexOfOnStart);
            onStart.accept(RandomString.string());
            return null;
        }).when(mockApi).subscribe(
            ArgumentMatchers.any(), // Class<T>
            ArgumentMatchers.any(), // Consumer<String>, onStart
            ArgumentMatchers.any(), // Consumer<GraphQLResponse<ModelWithMetadata<T>>>, onNextResponse
            ArgumentMatchers.any(), // Consumer<DataStoreException>, onSubscriptionFailure
            ArgumentMatchers.any() // Action, onSubscriptionCompleted
        );
    }

    /**
     * Mock a successful mutation call made for a given model.
     * @param mockApi Mock object of type {@link GraphQLBehavior}
     * @param subjectId The id of the subject being mutated.
     * @param result The object to be returned by the mock.
     * @param <M> The model type.
     */
    public static <M> void mockSuccessfulMutation(GraphQLBehavior mockApi, String subjectId, M result) {
        Mockito.doAnswer(invocation -> {
            // Simulate a successful response callback from the create() method.
            final int indexOfResultConsumer = 1;
            Consumer<GraphQLResponse<M>> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(new GraphQLResponse<>(result, Collections.emptyList()));

            // Technically, create() returns a Cancelable...
            return null;
        }).when(mockApi).mutate(
            ArgumentMatchers.argThat(requestContainsModelId(subjectId)),
            ArgumentMatchers.any(), // onResponse
            ArgumentMatchers.any() // onFailure
        );
    }

    /**
     * Mock the results of successful query operation triggered by the DataStore.
     * @param mockApi Mock object of type {@link GraphQLBehavior}
     * @param results A list of objects to be returned in the result.
     * @param <M> The model type.
     */
    @SuppressWarnings("unchecked")
    public static <M> void mockSuccessfulQuery(GraphQLBehavior mockApi, M... results) {
        Mockito.doAnswer(invocation -> {
            final int onResultsIndex = 1;

            // The callback function to be invoked with the results
            Consumer<GraphQLResponse<PaginatedResult<M>>> onResults = invocation.getArgument(onResultsIndex);
            onResults.accept(new GraphQLResponse<PaginatedResult<M>>(
                new PaginatedResult<M>(Arrays.asList(results), null),
                Collections.emptyList()
            ));
            return null;
        }).when(mockApi).query(
            ArgumentMatchers.any(), // Request
            ArgumentMatchers.any(), // onResponse
            ArgumentMatchers.any()  // onFailure
        );
    }

    private static <M extends Model> ArgumentMatcher<GraphQLRequest<M>> requestContainsModelId(String modelId) {
        return graphQLRequest -> {
            try {
                JSONObject payload = new JSONObject(graphQLRequest.getContent());
                String modelIdInRequest = payload.getJSONObject("variables").getJSONObject("input").getString("id");
                return modelId.equals(modelIdInRequest);
            } catch (JSONException exception) {
                return false;
            }
        };
    }
}
