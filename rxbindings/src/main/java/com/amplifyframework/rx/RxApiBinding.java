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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * An implementation of the RxApi which satisfies the API contract by wrapping
 * {@link ApiCategoryBehavior} in Rx primitives.
 */
final class RxApiBinding implements RxApi {
    private final ApiCategoryBehavior api;

    RxApiBinding() {
        this(Amplify.API);
    }

    @SuppressWarnings("WeakerAccess")
    RxApiBinding(ApiCategoryBehavior api) {
        this.api = api;
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(@NonNull Class<T> modelClass) {
        return RxAdapters.toSingle(listener -> api.query(modelClass, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> query(
            @NonNull Class<T> modelClass, @NonNull String modelId) {
        return RxAdapters.toSingle(listener -> api.query(modelClass, modelId, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull Class<T> modelClass, @NonNull QueryPredicate searchCriteria) {
        return RxAdapters.toSingle(listener -> api.query(modelClass, searchCriteria, listener));

    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<Iterable<T>>> query(@NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toSingle(listener -> api.query(graphQlRequest, listener));

    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull String apiName, @NonNull Class<T> modelClass) {
        return RxAdapters.toSingle(listener -> api.query(apiName, modelClass, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> query(
            @NonNull String apiName, @NonNull Class<T> modelClass, @NonNull String modelId) {
        return RxAdapters.toSingle(listener -> api.query(apiName, modelClass, modelId, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull String apiName, @NonNull Class<T> modelClass, @NonNull QueryPredicate searchCriteria) {
        return RxAdapters.toSingle(listener -> api.query(apiName, modelClass, searchCriteria, listener));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toSingle(listener -> api.query(apiName, graphQlRequest, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull T model, @NonNull MutationType mutationType) {
        return RxAdapters.toSingle(listener -> api.mutate(model, mutationType, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull T model, @NonNull QueryPredicate mutationCriteria, @NonNull MutationType mutationType) {
        return RxAdapters.toSingle(listener -> api.mutate(model, mutationCriteria, mutationType, listener));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(@NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toSingle(listener -> api.mutate(graphQlRequest, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName, @NonNull T model, @NonNull MutationType mutationType) {
        return RxAdapters.toSingle(listener -> api.mutate(apiName, model, mutationType, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull QueryPredicate mutationCriteria,
            @NonNull MutationType mutationType) {
        return RxAdapters.toSingle(listener -> api.mutate(apiName, model, mutationCriteria, mutationType, listener));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toSingle(listener -> api.mutate(apiName, graphQlRequest, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<GraphQLResponse<T>> subscribe(
            @NonNull Class<T> modelClass, @NonNull SubscriptionType subscriptionType) {
        return RxAdapters.toObservable(listener -> api.subscribe(modelClass, subscriptionType, listener));
    }

    @NonNull
    @Override
    public <T> Observable<GraphQLResponse<T>> subscribe(@NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toObservable(listener -> api.subscribe(graphQlRequest, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<GraphQLResponse<T>> subscribe(
            @NonNull String apiName, @NonNull Class<T> modelClass, @NonNull SubscriptionType subscriptionType) {
        return RxAdapters.toObservable(listener -> api.subscribe(apiName, modelClass, subscriptionType, listener));
    }

    @NonNull
    @Override
    public <T> Observable<GraphQLResponse<T>> subscribe(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return RxAdapters.toObservable(listener -> api.subscribe(apiName, graphQlRequest, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.get(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.get(apiName, request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.put(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.put(apiName, request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.post(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.post(apiName, request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.delete(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.delete(apiName, request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.head(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.head(apiName, request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.patch(request, listener));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull String apiName, @NonNull RestOptions request) {
        return RxAdapters.toSingle(listener -> api.patch(apiName, request, listener));
    }
}
