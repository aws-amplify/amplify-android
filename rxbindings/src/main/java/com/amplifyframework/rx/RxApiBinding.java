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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.rx.RxAdapters.CancelableBehaviors;
import com.amplifyframework.rx.RxOperations.RxSubscriptionOperation;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * An implementation of the RxApiCategoryBehavior which satisfies the API contract by wrapping
 * {@link ApiCategoryBehavior} in Rx primitives.
 */
public final class RxApiBinding implements RxApiCategoryBehavior {
    private final ApiCategoryBehavior api;

    RxApiBinding() {
        this(Amplify.API);
    }

    @VisibleForTesting
    RxApiBinding(ApiCategory api) {
        this.api = api;
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> query(@NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.query(graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> query(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.query(apiName, graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(@NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.mutate(graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.mutate(apiName, graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> RxSubscriptionOperation<GraphQLResponse<T>> subscribe(@NonNull GraphQLRequest<T> graphQlRequest) {
        return new RxSubscriptionOperation<GraphQLResponse<T>>((onStart, onItem, onError, onComplete) -> {
            return api.subscribe(graphQlRequest, onStart, onItem, onError, onComplete);
        });
    }

    @NonNull
    @Override
    public <T> RxSubscriptionOperation<GraphQLResponse<T>> subscribe(@NonNull String apiName,
                                                                     @NonNull GraphQLRequest<T> graphQlRequest) {
        return new RxSubscriptionOperation<GraphQLResponse<T>>((onStart, onItem, onError, onComplete) -> {
            return api.subscribe(apiName, graphQlRequest, onStart, onItem, onError, onComplete);
        });
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.get(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.get(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.put(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.put(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.post(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.post(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.delete(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.delete(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.head(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.head(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.patch(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.patch(apiName, request, onResult, onError));
    }

    private <T> Single<T> toSingle(CancelableBehaviors.ResultEmitter<T, ApiException> method) {
        return CancelableBehaviors.toSingle(method);
    }

    private <T> Observable<T> toObservable(
            CancelableBehaviors.StreamEmitter<String, T, ApiException> method) {
        return CancelableBehaviors.toObservable(method);
    }

}
