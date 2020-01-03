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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility to perform synchronous calls to the {@link ApiCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousApi {
    private static SynchronousApi singleton = null;

    @SuppressWarnings("checkstyle:all") private SynchronousApi() {}

    /**
     * Gets a singleton instance of the Synchronous API utility.
     * @return Singleton instance of Synchronous API
     */
    @NonNull
    public static synchronized SynchronousApi singleton() {
        if (SynchronousApi.singleton == null) {
            SynchronousApi.singleton = new SynchronousApi();
        }
        return SynchronousApi.singleton;
    }

    /**
     * Create a model via API.
     * @param apiName One of the configured, available APIs that expects type T
     * @param model A model to be created on API
     * @param <T> The type of model
     * @return The instance of the model as created on endpoint
     */
    @NonNull
    public <T extends Model> T create(@NonNull String apiName, @NonNull T model) {
        return awaitResponseData(listener ->
            Amplify.API.mutate(apiName, model, MutationType.CREATE, listener));
    }

    /**
     * Create a model via API. Assumes there is exactly one configured API endpoint.
     * @param model Model to create in remote API
     * @param <T> The type of the model
     * @return The endpoint's understanding of what was created
     */
    @NonNull
    public <T extends Model> T create(@NonNull T model) {
        return awaitResponseData(listener -> Amplify.API.mutate(model, MutationType.CREATE, listener));
    }

    /**
     * Create an object at the remote endpoint using a raw request.
     * @param apiName One of the configured APIs
     * @param request A GraphQL creation request
     * @param <T> Type of object being created
     * @return The endpoint's understanding of the thing that was created
     */
    @NonNull
    public <T> T create(@NonNull String apiName, @NonNull GraphQLRequest<T> request) {
        return awaitResponseData(listener -> Amplify.API.mutate(apiName, request, listener));
    }

    /**
     * Update a model.
     * @param apiName One of the configured API endpoints, that knows about the model type
     * @param model A model whose ID is already known to endpoint, but that has other changes
     * @param predicate Conditions to check on existing model in endpoint, before applying updates
     * @param <T> The type of model being updated
     * @return The server's understanding of the model, after the update
     */
    @NonNull
    public <T extends Model> T update(
            @NonNull String apiName, @NonNull T model, @NonNull QueryPredicate predicate) {
        return awaitResponseData(listener ->
            Amplify.API.mutate(apiName, model, predicate, MutationType.UPDATE, listener));
    }

    /**
     * Update a model, without any conditional predicate.
     * Assume there is exactly one well-configured API endpoint.
     * @param model Updated copy of model
     * @param <T> The type of model being updated
     * @return The updated item as understood by the API endpoint
     */
    @NonNull
    public <T extends Model> T update(@NonNull T model) {
        return awaitResponseData(listener -> Amplify.API.mutate(model, MutationType.UPDATE, listener));
    }

    /**
     * Attempt to update a model, but expect it not to succeed.
     * Obtain and return the GraphQL errors from the endpoint's response.
     * @param apiName One of the configure API endpoints, that knows about this model type
     * @param model Model that we will attempt to update
     * @param predicate Only update the model if these conditions are met on existing data
     * @param <T> The type of model being updated
     * @return Errors contained in the endpoint's response, detailing why the update didn't succeed
     */
    @NonNull
    public <T extends Model> List<GraphQLResponse.Error> updateExpectingErrors(
            @NonNull String apiName, @NonNull T model, @NonNull QueryPredicate predicate) {
        return this.<T>awaitResponseErrors(listener ->
            Amplify.API.mutate(apiName, model, predicate, MutationType.UPDATE, listener));
    }

    /**
     * Gets an instance of a model by its model class and model ID.
     * @param apiName One of the configured API endpoints
     * @param clazz The class of model being searched at the endpoint
     * @param modelId The ID of the model being searched
     * @param <T> The type of the model being searched
     * @return A result, if available
     */
    @NonNull
    public <T extends Model> T get(
            @NonNull final String apiName, @NonNull Class<T> clazz, @NonNull String modelId) {
        return awaitResponseData(listener -> Amplify.API.query(apiName, clazz, modelId, listener));
    }

    /**
     * Gets an instance of a model by its model class and model ID.
     * Assumes a single endpoint is properly configured in API category.
     * @param clazz The class of the model being queried
     * @param modelId The ID of the specific model instance being queried
     * @param <T> Type of model being queried
     * @return If available, an exact match for the queried class and ID
     */
    @NonNull
    public <T extends Model> T get(@NonNull Class<T> clazz, @NonNull String modelId) {
        return awaitResponseData(listener -> Amplify.API.query(clazz, modelId, listener));
    }

    /**
     * Peform a blocking REST GET.
     * @param apiName A configured REST API
     * @param options REST options for GET
     * @return REST Response
     */
    @NonNull
    public RestResponse get(@NonNull String apiName, @NonNull RestOptions options) {
        LatchedConsumer<RestResponse> responseConsumer = LatchedConsumer.instance();
        Amplify.API.get(apiName, options, responseConsumer, EmptyConsumer.of(ApiException.class));
        return responseConsumer.awaitValue();
    }

    /**
     * Perform a REST POST.
     * @param apiName One of the configured endpoints APIs
     * @param options POST options
     * @return REST Response
     */
    @NonNull
    public RestResponse post(@NonNull String apiName, @NonNull RestOptions options) {
        LatchedConsumer<RestResponse> responseConsumer = LatchedConsumer.instance();
        Amplify.API.post(apiName, options, responseConsumer, EmptyConsumer.of(ApiException.class));
        return responseConsumer.awaitValue();
    }

    /**
     * Gets a list of models of certain class and that match a querying predicate.
     * @param apiName One of the configured API endpoints
     * @param clazz The class of models being listed
     * @param predicate A querying predicate to match against models of the requested class
     * @param <T> The type of models being listed
     * @return A list of models of the requested type, that match the predicate
     */
    @NonNull
    public <T extends Model> List<T> list(
            @NonNull String apiName,
            @NonNull Class<T> clazz,
            @SuppressWarnings("NullableProblems") @NonNull QueryPredicate predicate) {
        final Iterable<T> queryResults =
            awaitResponseData(listener -> Amplify.API.query(apiName, clazz, predicate, listener));
        final List<T> results = new ArrayList<>();
        for (T item : queryResults) {
            results.add(item);
        }
        return Immutable.of(results);
    }

    /**
     * Lists all remote instances of a model, by model class.
     * @param apiName One of the configured API endpoints
     * @param clazz The class of models being queried
     * @param <T> The type of models being queried
     * @return A list of all models of the requested class
     */
    @NonNull
    public <T extends Model> List<T> list(@NonNull String apiName, @NonNull Class<T> clazz) {
        //noinspection ConstantConditions To save boiler plate, we do this internally.
        return list(apiName, clazz, null);
    }

    /**
     * Deletes a model from the remote endpoint.
     * @param apiName One of the configured API endpoints
     * @param modelToDelete Model to be deleted from endpoint
     * @param <T> Type of model being deleted
     * @return The endpoint's view of the model that was deleted
     */
    @NonNull
    public <T extends Model> T delete(@NonNull String apiName, @NonNull T modelToDelete) {
        return awaitResponseData(listener ->
            Amplify.API.mutate(apiName, modelToDelete, MutationType.DELETE, listener));
    }

    /**
     * Subscribe to model creations.
     * @param apiName One of the configured API endpoints
     * @param clazz Class of model for which you want notifications when they're created
     * @param <T> The type of model for which creation notifications will be dispatched
     * @return A Cancelable interface that can be used to end the subscription
     */
    @NonNull
    public <T extends Model> Subscription<T> onCreate(@NonNull String apiName, @NonNull Class<T> clazz) {
        return createSubscription(streamListener ->
            Amplify.API.subscribe(apiName, clazz, SubscriptionType.ON_CREATE, streamListener));
    }

    /**
     * Subscribe to create mutations, by forming a GraphQL subscription request.
     * @param apiName One of the configured APIs
     * @param request A GraphQL subscription request
     * @param <T> Type of object for which creation notifications are generated
     * @return A subscription object representing this ongoing subscription
     */
    @NonNull
    public <T> Subscription<T> onCreate(@NonNull String apiName, @NonNull GraphQLRequest<T> request) {
        return createSubscription(streamListener -> Amplify.API.subscribe(apiName, request, streamListener));
    }

    private <T> Subscription<T> createSubscription(SubscriptionCreationMethod<T> method) {
        LatchedResponseConsumer<T> streamItemConsumer = LatchedResponseConsumer.instance();
        LatchedAction streamCompletionAction = LatchedAction.instance();
        LatchedConsumer<ApiException> errorConsumer = LatchedConsumer.instance();
        StreamListener<GraphQLResponse<T>, ApiException> streamListener =
            StreamListener.instance(streamItemConsumer, errorConsumer, streamCompletionAction);

        final Cancelable cancelable = method.streamTo(streamListener);
        if (cancelable == null) {
            throw new RuntimeException("Got a null operation back from API subscribe.");
        }

        return new Subscription<>(
            streamItemConsumer,
            errorConsumer,
            streamCompletionAction,
            cancelable
        );
    }

    private <T> T awaitResponseData(AsyncOperation<T> operation) {
        LatchedResponseConsumer<T> responseConsumer = LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<T>, ApiException> responseListener =
            ResultListener.instance(responseConsumer, EmptyConsumer.of(ApiException.class));
        operation.respondWith(responseListener);
        return responseConsumer.awaitResponseData();
    }

    private <T> List<GraphQLResponse.Error> awaitResponseErrors(AsyncOperation<T> operation) {
        LatchedResponseConsumer<T> responseConsumer = LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<T>, ApiException> responseListener =
            ResultListener.instance(responseConsumer, EmptyConsumer.of(ApiException.class));
        operation.respondWith(responseListener);
        return responseConsumer.awaitErrorsInNextResponse();
    }

    interface AsyncOperation<T> {
        void respondWith(ResultListener<GraphQLResponse<T>, ApiException> responseListener);
    }

    interface SubscriptionCreationMethod<T> {
        Cancelable streamTo(StreamListener<GraphQLResponse<T>, ApiException> streamListener);
    }

    /**
     * A subscription instance provides synchronous methods to interact
     * with an ongoing background subscription.
     * @param <T> Type of data that is subscribed
     */
    @SuppressWarnings("unused")
    public static final class Subscription<T> {
        private final LatchedResponseConsumer<T> itemConsumer;
        private final LatchedConsumer<ApiException> errorConsumer;
        private final LatchedAction completionAction;
        private final Cancelable cancellationMethod;

        Subscription(
                @NonNull LatchedResponseConsumer<T> itemConsumer,
                @NonNull LatchedConsumer<ApiException> errorConsumer,
                @NonNull LatchedAction completionAction,
                @NonNull Cancelable cancellationMethod) {
            this.itemConsumer = itemConsumer;
            this.errorConsumer = errorConsumer;
            this.completionAction = completionAction;
            this.cancellationMethod = cancellationMethod;
        }

        /**
         * Await the first value that arrives on the subscription.
         * Its response envelope must not contain any errors.
         * @return The first value received on the subscription
         */
        @NonNull
        public T awaitFirstValue() {
            return itemConsumer.awaitResponseData();
        }

        /**
         * Await values to arrive in responses. Respones must not contain errors.
         * @param count Number of values to await
         * @return The values
         */
        @NonNull
        public List<T> awaitValues(int count) {
            return Immutable.of(itemConsumer.awaitResponseData(count));
        }

        /**
         * Await the next response, and validate that it contains errors,
         * and return them.
         * @return The errors in the next response
         */
        @NonNull
        public List<GraphQLResponse.Error> awaitNextResponseErrors() {
            return Immutable.of(itemConsumer.awaitErrorsInNextResponse());
        }

        /**
         * Wait for the subscription to commplete.
         */
        public void awaitSubscriptionCompletion() {
            completionAction.awaitCall();
        }

        /**
         * Await a failure of the subscription.
         * @return The failure
         */
        @NonNull
        public Throwable awaitSubscriptionFailure() {
            return errorConsumer.awaitValue();
        }

        /**
         * Cancel the subscription.
         */
        public void cancel() {
            cancellationMethod.cancel();
        }
    }
}
