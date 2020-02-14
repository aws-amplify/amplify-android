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
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

/**
 * A utility to perform synchronous calls to the {@link ApiCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousApi {

    private static final long EXTENDED_TIMEOUT_IN_MILLISECONDS =
            TimeUnit.SECONDS.toMillis(10); // 5 seconds is insufficient

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
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T create(@NonNull String apiName, @NonNull T model) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(apiName, model, MutationType.CREATE, onResponse, onFailure));
    }

    /**
     * Create a model via API. Assumes there is exactly one configured API endpoint.
     * @param model Model to create in remote API
     * @param <T> The type of the model
     * @return The endpoint's understanding of what was created
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T create(@NonNull T model) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(model, MutationType.CREATE, onResponse, onFailure));
    }

    /**
     * Create an object at the remote endpoint using a raw request.
     * @param apiName One of the configured APIs
     * @param request A GraphQL creation request
     * @param <T> Type of object being created
     * @return The endpoint's understanding of the thing that was created
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T> T create(@NonNull String apiName, @NonNull GraphQLRequest<T> request) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(apiName, request, onResponse, onFailure));
    }

    /**
     * Update a model.
     * @param apiName One of the configured API endpoints, that knows about the model type
     * @param model A model whose ID is already known to endpoint, but that has other changes
     * @param predicate Conditions to check on existing model in endpoint, before applying updates
     * @param <T> The type of model being updated
     * @return The server's understanding of the model, after the update
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T update(
            @NonNull String apiName, @NonNull T model, @NonNull QueryPredicate predicate) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(apiName, model, predicate, MutationType.UPDATE, onResponse, onFailure));
    }

    /**
     * Update a model, without any conditional predicate.
     * Assume there is exactly one well-configured API endpoint.
     * @param model Updated copy of model
     * @param <T> The type of model being updated
     * @return The updated item as understood by the API endpoint
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T update(@NonNull T model) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(model, MutationType.UPDATE, onResponse, onFailure));
    }

    /**
     * Attempt to update a model, but expect it not to succeed.
     * Obtain and return the GraphQL errors from the endpoint's response.
     * @param apiName One of the configure API endpoints, that knows about this model type
     * @param model Model that we will attempt to update
     * @param predicate Only update the model if these conditions are met on existing data
     * @param <T> The type of model being updated
     * @return Errors contained in the endpoint's response, detailing why the update didn't succeed
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> List<GraphQLResponse.Error> updateExpectingErrors(
            @NonNull String apiName, @NonNull T model, @NonNull QueryPredicate predicate) throws ApiException {
        return this.<T>awaitResponseErrors((onResponse, onFailure) ->
            Amplify.API.mutate(apiName, model, predicate, MutationType.UPDATE, onResponse, onFailure));
    }

    /**
     * Gets an instance of a model by its model class and model ID.
     * @param apiName One of the configured API endpoints
     * @param clazz The class of model being searched at the endpoint
     * @param modelId The ID of the model being searched
     * @param <T> The type of the model being searched
     * @return A result, if available
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T get(
            @NonNull final String apiName, @NonNull Class<T> clazz, @NonNull String modelId) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.query(apiName, clazz, modelId, onResponse, onFailure));
    }

    /**
     * Gets an instance of a model by its model class and model ID.
     * Assumes a single endpoint is properly configured in API category.
     * @param clazz The class of the model being queried
     * @param modelId The ID of the specific model instance being queried
     * @param <T> Type of model being queried
     * @return If available, an exact match for the queried class and ID
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T get(@NonNull Class<T> clazz, @NonNull String modelId) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.query(clazz, modelId, onResponse, onFailure));
    }

    /**
     * Peform a blocking REST GET.
     * @param apiName A configured REST API
     * @param options REST options for GET
     * @return REST Response
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public RestResponse get(@NonNull String apiName, @NonNull RestOptions options) throws ApiException {
        return awaitRestResponse((onResponse, onFailure) ->
            Amplify.API.get(apiName, options, onResponse, onFailure));
    }

    /**
     * Perform a REST POST.
     * @param apiName One of the configured endpoints APIs
     * @param options POST options
     * @return REST Response
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public RestResponse post(@NonNull String apiName, @NonNull RestOptions options) throws ApiException {
        return awaitRestResponse((onResponse, onFailure) ->
            Amplify.API.post(apiName, options, onResponse, onFailure));
    }

    /**
     * Gets a list of models of certain class and that match a querying predicate.
     * @param apiName One of the configured API endpoints
     * @param clazz The class of models being listed
     * @param predicate A querying predicate to match against models of the requested class
     * @param <T> The type of models being listed
     * @return A list of models of the requested type, that match the predicate
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> List<T> list(
            @NonNull String apiName,
            @NonNull Class<T> clazz,
            @SuppressWarnings("NullableProblems") @NonNull QueryPredicate predicate) throws ApiException {
        final Iterable<T> queryResults = awaitResponseData((onResponse, onFailure) ->
            Amplify.API.query(apiName, clazz, predicate, onResponse, onFailure));
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
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> List<T> list(@NonNull String apiName, @NonNull Class<T> clazz) throws ApiException {
        //noinspection ConstantConditions To save boiler plate, we do this internally.
        return list(apiName, clazz, null);
    }

    /**
     * Deletes a model from the remote endpoint.
     * @param apiName One of the configured API endpoints
     * @param modelToDelete Model to be deleted from endpoint
     * @param <T> Type of model being deleted
     * @return The endpoint's view of the model that was deleted
     * @throws ApiException If unable to obtain response from endpoint
     */
    @NonNull
    public <T extends Model> T delete(@NonNull String apiName, @NonNull T modelToDelete) throws ApiException {
        return awaitResponseData((onResponse, onFailure) ->
            Amplify.API.mutate(apiName, modelToDelete, MutationType.DELETE, onResponse, onFailure));
    }

    /**
     * Subscribe to model creations.
     * @param apiName One of the configured API endpoints
     * @param clazz Class of model for which you want notifications when they're created
     * @param <T> The type of model for which creation notifications will be dispatched
     * @return An Observable that can be used to observe the subscription data
     */
    @NonNull
    public <T extends Model> Observable<GraphQLResponse<T>> onCreate(@NonNull String apiName, @NonNull Class<T> clazz) {
        return Observable.create(emitter -> {
            CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            Await.<String, ApiException>result(
                EXTENDED_TIMEOUT_IN_MILLISECONDS,
                (onSubscriptionStarted, onError) -> {
                    Cancelable cancelable = Amplify.API.subscribe(
                        apiName,
                        clazz,
                        SubscriptionType.ON_CREATE,
                        onSubscriptionStarted,
                        emitter::onNext,
                        onError,
                        emitter::onComplete
                    );
                    if (cancelable != null) {
                        disposable.add(Disposables.fromAction(cancelable::cancel));
                    }
                }
            );
        });
    }

    /**
     * Subscribe to create mutations, by forming a GraphQL subscription request.
     * @param apiName One of the configured APIs
     * @param request A GraphQL subscription request
     * @param <T> Type of object for which creation notifications are generated
     * @return An observable with which creations may be observed
     */
    @NonNull
    public <T> Observable<GraphQLResponse<T>> onCreate(@NonNull String apiName, @NonNull GraphQLRequest<T> request) {
        return Observable.create(emitter -> {
            CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            Await.<String, ApiException>result(
                EXTENDED_TIMEOUT_IN_MILLISECONDS,
                (onSubscriptionStarted, onError) -> {
                    Cancelable cancelable = Amplify.API.subscribe(
                        apiName,
                        request,
                        onSubscriptionStarted,
                        emitter::onNext,
                        onError,
                        emitter::onComplete
                    );
                    if (cancelable != null) {
                        disposable.add(Disposables.fromAction(cancelable::cancel));
                    }
                }
            );
        });
    }

    // Syntax fluff to get rid of type bounds when calling Await.result(...).
    private <T> T awaitResponseData(
            Await.ResultErrorEmitter<GraphQLResponse<T>, ApiException> resultErrorEmitter)
            throws ApiException {
        final GraphQLResponse<T> response = Await.result(EXTENDED_TIMEOUT_IN_MILLISECONDS,
                resultErrorEmitter);
        if (response.hasErrors()) {
            String firstErrorMessage = response.getErrors().get(0).getMessage();
            throw new RuntimeException("Response has error:" + firstErrorMessage);
        } else if (response.getData() == null) {
            throw new RuntimeException("Response data was null.");
        }
        return response.getData();
    }

    // Syntax fluff to await GraphQL errors
    private <T> List<GraphQLResponse.Error> awaitResponseErrors(
            Await.ResultErrorEmitter<GraphQLResponse<T>, ApiException> resultErrorEmitter)
            throws ApiException {
        final GraphQLResponse<T> response = Await.result(EXTENDED_TIMEOUT_IN_MILLISECONDS,
                resultErrorEmitter);
        if (!response.hasErrors()) {
            throw new RuntimeException("No errors in response.");
        }
        return response.getErrors();
    }

    // Syntax fluff to get rid of type bounds on REST calls
    private RestResponse awaitRestResponse(
            Await.ResultErrorEmitter<RestResponse, ApiException> resultErrorEmitter)
            throws ApiException {
        return Await.result(EXTENDED_TIMEOUT_IN_MILLISECONDS,
                resultErrorEmitter);
    }
}
