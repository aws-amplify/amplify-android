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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

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
        doAnswer(invocation -> {
            final int indexOfOnStart = 1;
            Consumer<String> onStart = invocation.getArgument(indexOfOnStart);
            onStart.accept(RandomString.string());
            return null;
        }).when(mockApi).subscribe(
            any(), // Class<T>
            any(), // Consumer<String>, onStart
            any(), // Consumer<GraphQLResponse<ModelWithMetadata<T>>>, onNextResponse
            any(), // Consumer<DataStoreException>, onSubscriptionFailure
            any() // Action, onSubscriptionCompleted
        );
    }

    /**
     * Mock a successful mutation call made for a given model.
     * @param mockApi Mock object of type {@link GraphQLBehavior}
     * @param model The model for the mutation operation.
     * @param <M> The model type.
     */
    public static <M extends Model> void mockSuccessfulMutation(GraphQLBehavior mockApi, M model) {
        doAnswer(invocation -> {
            // Simulate a successful response callback from the create() method.
            final int indexOfModelBeingCreated = 0;
            final int indexOfResultConsumer = 1;
            GraphQLRequest<M> request = invocation.getArgument(indexOfModelBeingCreated);

            // Pass back a ModelWithMetadata. Model is the one provided.
            ModelMetadata metadata =
                new ModelMetadata(model.getId(), false, 1, new Temporal.Timestamp());
            ModelWithMetadata<M> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
            Consumer<GraphQLResponse<ModelWithMetadata<M>>> onResult =
                invocation.getArgument(indexOfResultConsumer);
            onResult.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));

            // Technically, create() returns a Cancelable...
            return null;
        }).when(mockApi).mutate(
            argThat(requestContainsModelId(model.getId())), // Match the id of the model passed in to the function.
            any(), // onResponse
            any() // onFailure
        );
    }

    /**
     * Mock the results of successful query operation triggered by the DataStore.
     * @param mockApi Mock object of type {@link GraphQLBehavior}
     * @param models A list of models to be returned in the result.
     * @param <M> The model type.
     */
    @SuppressWarnings("unchecked")
    public static <M extends Model> void mockSuccessfulSyncQuery(GraphQLBehavior mockApi, M... models) {
        doAnswer(invocation -> {
            final int onResultsIndex = 1;
            // Transform from an array of Model types to, to a list of ModelWithMetadata<M extends Model>
            List<ModelWithMetadata<M>> syncQueryResults =
                Observable.fromArray(models)
                          .map(model -> {
                              ModelMetadata metadata = new ModelMetadata(model.getId(),
                                                                         false,
                                                                         1,
                                                                         Temporal.Timestamp.now());
                              ModelWithMetadata<M> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
                              return modelWithMetadata;
                          })
                          .toList()
                          .blockingGet();

            // The callback function to be invoked with the results
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>>> onResults =
                invocation.getArgument(onResultsIndex);
            onResults.accept(new GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>>(
                new PaginatedResult<ModelWithMetadata<M>>(syncQueryResults, null),
                Collections.emptyList()
            ));
            return null;
        }).when(mockApi).query(
            any(), // Request
            any(), // onResponse
            any()  // onFailure
        );
    }

    private static <M extends Model> ArgumentMatcher<GraphQLRequest<M>> requestContainsModelId(String modelId) {
        return graphQLRequest -> {
            try {
                JSONObject payload = new JSONObject(graphQLRequest.getContent());
                String modelIdInRequest = payload.getJSONObject("variables").getJSONObject("input").getString("id");
                return modelId.equals(modelIdInRequest);
            } catch (JSONException exception) {
                fail("Invalid GraphQLRequest payload." + exception.getMessage());
            }
            return false;
        };
    }
}
