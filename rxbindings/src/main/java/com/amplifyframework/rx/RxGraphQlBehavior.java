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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

/**
 * An Rx-idiomatic expression of Amplify's {@link GraphQLBehavior}.
 */
@SuppressWarnings("unused") // This is a public API
public interface RxGraphQlBehavior {

    /**
     * Query a remote API for a list of Amplify {@link Model}s of a given class.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param modelClass Class of models to query
     * @param <M> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response.
     *         On success, the {@link GraphQLResponse} will contain a list of models
     *         of the requested type, if there are any known to the remote endpoint.
     *         The response object may itself contain errors that were communicated by
     *         the endpoint; these may be inspected with {@link GraphQLResponse#hasErrors()}
     *         and {@link GraphQLResponse#getErrors()}. The network operation does not begin
     *         until the {@link Single} is subscribed. The operation may be terminated by
     *         invoking {@link Disposable#dispose()} on the {@link Single} subscription.
     */
    @NonNull
    <M extends Model> Single<GraphQLResponse<Iterable<M>>> query(
            @NonNull Class<M> modelClass
    );

    /**
     * Query a remote API for a particular {@link Model} with a given class and ID.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param modelClass Class of the unique model instance being queried
     * @param modelId ID of the unique model instance being queried
     * @param <M> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success,
     *         the {@link GraphQLResponse} will contain the unique model having the requested
     *         type and ID, if it is known to the remote endpoint.
     *         The response object may itself contain errors that were communicated by
     *         the endpoint; these may be inspected with {@link GraphQLResponse#hasErrors()}
     *         and {@link GraphQLResponse#getErrors()}. The network operation does not
     *         begin until the {@link Single} is subscribed. The operation may be terminated
     *         by invoking {@link Disposable#dispose()} on the {@link Single} subscription.
     */
    @NonNull
    <M extends Model> Single<GraphQLResponse<M>> query(
            @NonNull Class<M> modelClass,
            @NonNull String modelId
    );

    /**
     * Query a remote API for a list of {@link Model}s of a given class,
     * that additionally match a {@link QueryPredicate}.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param modelClass Class of models being queried
     * @param searchCriteria Additional criteria applied to models of the requested class,
     *                       before being returned to the client. The criteria are evaluated
     *                       on the server.
     * @param <M> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response.
     *         On success, the {@link GraphQLResponse} will contain a list of models of the
     *         requested class, that match the provided query predicate, if any such are
     *         known to the remote endpoint. The response object may itself contain errors that
     *         were communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()} on
     *         the {@link Single} subscription.
     */
    @NonNull
    <M extends Model> Single<GraphQLResponse<Iterable<M>>> query(
            @NonNull Class<M> modelClass,
            @NonNull QueryPredicate searchCriteria
    );

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
     * Query a remote API for a list of Amplify {@link Model}s of a given class.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param modelClass Class of models to query
     * @param <T> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain a list of models of the requested type,
     *         if there are any known to the remote endpoint. The response object may itself
     *         contain errors that were communicated by the endpoint; these may be inspected
     *         with {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()} on the
     *         {@link Single} subscription.
     */
    @NonNull
    <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass
    );

    /**
     * Query a remote API for a single, unique Amplify {@link Model} by its class and ID.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param modelClass Class of the unique model instance being queried
     * @param modelId Unique ID of the model being queried
     * @param <T> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the unique model being queried, if there is
     *         one such model known to the remote endpoint. The response object may itself
     *         contain errors that were communicated by the endpoint; these may be inspected
     *         with {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()} on
     *         the {@link Single} subscription.
     */
    @NonNull
    <T extends Model> Single<GraphQLResponse<T>> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull String modelId
    );

    /**
     * Query a remote API for a list of Amplify {@link Model}s of a given class,
     * that match additional search criteria.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param modelClass Class of models to query
     * @param searchCriteria Additional criteria to apply to models of the requested class,
     *                       before returning to client. The criteria are evaluated on the server.
     * @param <T> The type of model being queried
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain a list of models of the requested type,
     *         if there are any known to the remote endpoint. The response object may itself
     *         contain errors that were communicated by the endpoint; these may be inspected
     *         with {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()} on the
     *         {@link Single} subscription.
     */
    @NonNull
    <T extends Model> Single<GraphQLResponse<Iterable<T>>> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull QueryPredicate searchCriteria
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
     * Perform a mutation on the provided model, via a remote GraphQL API.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param model A model to mutate on remote GraphQL API
     * @param mutationType Type of mutation being performed, e.g. {@link MutationType#CREATE}, etc.
     * @param <M> The type of model being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the model,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <M extends Model> Single<GraphQLResponse<M>> mutate(
            @NonNull M model,
            @NonNull MutationType mutationType
    );

    /**
     * Perform a mutation on the provided model, only if the remote GraphQL API's current
     * version of the model matches the provided criteria.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Single}'s error callback.
     * @param model A model to mutate on remote GraphQL API
     * @param mutationCriteria Criteria that will be evaluated on the endpoint's current version
     *                         of the model. If matching, the mutation will be performed. Otherwise,
     *                         the mutation will not be performed.
     * @param mutationType Type of mutation being performed, e.g. {@link MutationType#UPDATE}, etc.
     * @param <M> The type of model being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the model,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <M extends Model> Single<GraphQLResponse<M>> mutate(
            @NonNull M model,
            @NonNull QueryPredicate mutationCriteria,
            @NonNull MutationType mutationType
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
     * Perform a mutation on the provided model, via a remote GraphQL API.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param model A model to mutate on remote GraphQL API
     * @param mutationType Type of mutation being performed, e.g. {@link MutationType#CREATE}, etc.
     * @param <T> The type of model being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the model,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull MutationType mutationType
    );

    /**
     * Perform a mutation on the provided model, only if the remote GraphQL API's current
     * version of the model matches the provided criteria.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param model A model to mutate on remote GraphQL API
     * @param mutationCriteria Criteria that will be evaluated on the endpoint's current version
     *                         of the model. If matching, the mutation will be performed. Otherwise,
     *                         the mutation will not be performed.
     * @param mutationType Type of mutation being performed, e.g. {@link MutationType#UPDATE}, etc.
     * @param <T> The type of model being mutated
     * @return A cold Single which emits a {@link GraphQLResponse} on success, or
     *         an {@link ApiException} on failure to obtain a response. On success, the
     *         {@link GraphQLResponse} will contain the endpoint's understanding of the model,
     *         after the mutation. The response object may itself contain errors that were
     *         communicated by the endpoint; these may be inspected with
     *         {@link GraphQLResponse#hasErrors()} and {@link GraphQLResponse#getErrors()}.
     *         The network operation does not begin until the {@link Single} is subscribed.
     *         The operation may be terminated by invoking {@link Disposable#dispose()}
     *         on the {@link Single} subscription.
     */
    @NonNull
    <T extends Model> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull QueryPredicate mutationCriteria,
            @NonNull MutationType mutationType
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
     * Subscribe to mutation events that occur on a GraphQL endpoint, for a given
     * class of Amplify {@link Model}.
     * This method assumes that the `amplifyconfiguration.json` contains a single GraphQL API,
     * configured during the call to {@link RxAmplify#configure(Context)}. If not,
     * this method emits an {@link ApiException} via the returned {@link Observable}'s
     * error callback.
     * @param modelClass Class of model for which mutation notifications will be dispatched
     * @param subscriptionType Type of mutations notifications desired, e.g.
     *                         {@link SubscriptionType#ON_CREATE} corresponding to
     *                         {@link MutationType#CREATE} events
     * @param <T> The type of model for which notifications will be dispatched
     * @return An {@link Observable} which emits 0..n {@link GraphQLResponse}s when mutations
     *         occur for models of the requested type. The stream of responses may terminate at
     *         any point with failure, emitted via the Observable's error callback. When the
     *         subscription terminates gracefully, the Observable's completion callback will be
     *         invoked. Each {@link GraphQLResponse} may itself contain errors, communicated from
     *         the endpoint. These can be inspected with {@link GraphQLResponse#hasErrors()} and
     *         {@link GraphQLResponse#getErrors()}. The network operation does not begin until the
     *         first {@link Observable} is subscribed. The subscription may be terminated at any
     *         time by invoking {@link Disposable#dispose()} on the {@link Observable} subscription.
     *         If no other Observable subscriptions exist for the model class and subscription type,
     *         the GraphQL network subscription will be closed.
     */
    @NonNull
    <T extends Model> Observable<GraphQLResponse<T>> subscribe(
            @NonNull Class<T> modelClass,
            @NonNull SubscriptionType subscriptionType
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
    <T> Observable<GraphQLResponse<T>> subscribe(
            @NonNull GraphQLRequest<T> graphQlRequest
    );

    /**
     * Subscribe to mutation events that occur on a GraphQL endpoint, for a given
     * class of Amplify {@link Model}.
     * @param apiName The name of any GraphQL endpoint for which a configuration exists in the
     *                `amplifyconfiguration.json` that was used during the call to
     *                {@link RxAmplify#configure(Context)}.
     * @param modelClass Class of model for which mutation notifications will be dispatched
     * @param subscriptionType Type of mutations notifications desired, e.g.
     *                         {@link SubscriptionType#ON_CREATE} corresponding to
     *                         {@link MutationType#CREATE} events
     * @param <T> The type of model for which notifications will be dispatched
     * @return An {@link Observable} which emits 0..n {@link GraphQLResponse}s when mutations
     *         occur for models of the requested type. The stream of responses may terminate at
     *         any point with failure, emitted via the Observable's error callback. When the
     *         subscription terminates gracefully, the Observable's completion callback will be
     *         invoked. Each {@link GraphQLResponse} may itself contain errors, communicated from
     *         the endpoint. These can be inspected with {@link GraphQLResponse#hasErrors()} and
     *         {@link GraphQLResponse#getErrors()}. The network operation does not begin until the
     *         first {@link Observable} is subscribed. The subscription may be terminated at any
     *         time by invoking {@link Disposable#dispose()} on the {@link Observable} subscription.
     *         If no other Observable subscriptions exist for the model class and subscription type,
     *         the GraphQL network subscription will be closed.
     */
    @NonNull
    <T extends Model> Observable<GraphQLResponse<T>> subscribe(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull SubscriptionType subscriptionType
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
    <R> Observable<GraphQLResponse<R>> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest
    );
}
