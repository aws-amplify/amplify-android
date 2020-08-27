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

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * An Rx-idiomatic expression of Amplify's {@link GraphQLBehavior}.
 */
@SuppressWarnings("unused") // This is a public API
public interface RxGraphQlBehavior {

    /**
     * Query a remote API for a list objects. This is the most flexible version of
     * query method available in the {@link RxGraphQlBehavior}, in that it accepts a primitive
     * {@link GraphQLRequest}, directly.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param graphQlRequest A raw GraphQL query request
     * @param <R> The type of object being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success,
     *         the {@link GraphQLResponse} will contain a list of objects that meet
     *         the criteria in the provided {@link GraphQLRequest}.
     *         The response object may itself contain errors that were communicated by
     *         the endpoint; these may be inspected with {@link GraphQLResponse#hasErrors()}
     *         and {@link GraphQLResponse#getErrors()}. The network operation does not begin
     *         until the {@link Single} is subscribed. The operation may be terminated by
     *         invoking {@link Disposable#dispose()} on the {@link Single} subscription.
     */
    @NonNull
    <R> Single<GraphQLResponse<R>> query(
            @NonNull GraphQLRequest<R> graphQlRequest
    );

    /**
     * Query a remote API for a list objects. This is the most flexible version of
     * query method available in the {@link RxGraphQlBehavior}, in that it accepts a primitive
     * {@link GraphQLRequest}, directly.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param graphQlRequest A raw GraphQL query request
     * @param <T> The type of object being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain a list of objects that meet the criteria
     *         in the provided {@link GraphQLRequest}. The response object may itself
     *         contain errors that were communicated by the endpoint; these may be inspected
     *         with {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()} on the
     *         {@link Single} subscription.
     */
    @NonNull
    <T> Single<GraphQLResponse<T>> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest
    );

    /**
     * Perform a mutation on a remote object using a raw {@link GraphQLRequest}.
     * Along with {@link RxGraphQlBehavior#mutate(String, GraphQLRequest)}, this is the most flexible
     * of the mutation methods, since it allows direct specification of a raw GraphQLRequest.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param graphQlRequest A mutation request
     * @param <T> The type of object being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the object,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <T> Single<GraphQLResponse<T>> mutate(
            @NonNull GraphQLRequest<T> graphQlRequest
    );

    /**
     * Perform a mutation on a remote object using a raw {@link GraphQLRequest}.
     * Along with {@link RxGraphQlBehavior#mutate(GraphQLRequest)}, this is the most flexible of the
     * mutation methods, since it allows direct specification of a raw GraphQLRequest.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param graphQlRequest A mutation request
     * @param <T> The type of object being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the object,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <T> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest
    );

    /**
     * Subscribe to mutation events that occur on a GraphQL endpoint. This, combined with
     * {@link RxGraphQlBehavior#subscribe(String, GraphQLRequest)} are the most flexible subscription
     * methods available, as they allow specification of a raw {@link GraphQLRequest}.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Observable}'s
     * error callback.
     * @param graphQlRequest A raw GraphQL subscription request
     * @param <T> The type of object for which notifications will be dispatched
     * @return An {@link Observable} which emits 0..n {@link GraphQLResponse}s when mutations
     *         occur for objects of the requested type. The stream of responses may terminate at
     *         any point with failure, emitted via the Observable's error callback. When the
     *         subscription terminates gracefully, the Observable's completion callback will be
     *         invoked. Each {@link GraphQLResponse} may itself contain errors, communicated from
     *         the endpoint. These can be inspected with {@link GraphQLResponse#hasErrors()} and
     *         {@link GraphQLResponse#getErrors()}. The network operation does not begin until the
     *         first {@link Observable} is subscribed. The subscription may be terminated at any
     *         time by invoking {@link Disposable#dispose()} on the {@link Observable} subscription.
     *         If no other Observable subscriptions exist for the object class and subscription type,
     *         the GraphQL network subscription will be closed.
     */
    @NonNull
    <T> RxApiBinding.RxSubscriptionOperation<GraphQLResponse<T>> subscribe(
            @NonNull GraphQLRequest<T> graphQlRequest
    );

    /**
     * Subscribe to mutation events that occur on a GraphQL endpoint. This, combined with
     * {@link RxGraphQlBehavior#subscribe(GraphQLRequest)} are the most flexible subscription
     * methods available, as they allow specification of a raw {@link GraphQLRequest}.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param graphQlRequest A raw GraphQL subscription request
     * @param <R> The type of object for which notifications will be dispatched
     * @return An {@link Observable} which emits 0..n {@link GraphQLResponse}s when mutations
     *         occur for objects of the requested type. The stream of responses may terminate at
     *         any point with failure, emitted via the Observable's error callback. When the
     *         subscription terminates gracefully, the Observable's completion callback will be
     *         invoked. Each {@link GraphQLResponse} may itself contain errors, communicated from
     *         the endpoint. These can be inspected with {@link GraphQLResponse#hasErrors()} and
     *         {@link GraphQLResponse#getErrors()}. The network operation does not begin until the
     *         first {@link Observable} is subscribed. The subscription may be terminated at any
     *         time by invoking {@link Disposable#dispose()} on the {@link Observable} subscription.
     *         If no other Observable subscriptions exist for the object class and subscription type,
     *         the GraphQL network subscription will be closed.
     */
    @NonNull
    <R> RxApiBinding.RxSubscriptionOperation<GraphQLResponse<R>> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest
    );
}
