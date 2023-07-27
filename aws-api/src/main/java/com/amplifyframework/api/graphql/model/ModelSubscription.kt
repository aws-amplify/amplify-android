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

package com.amplifyframework.api.graphql.model;

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;

/**
 * Helper class that provides methods to create {@link GraphQLRequest} that represents
 * subscriptions from {@link Model}.
 */
public final class ModelSubscription {

    /** This class should not be instantiated. */
    private ModelSubscription() {}

    /**
     * Builds a subscriptions request of a given {@code type} for a {@code modelType}.
     * @param modelType the model class.
     * @param type the subscription type.
     * @param <M> the concrete type of the model.
     * @return a valid {@link GraphQLRequest} instance.
     */
    public static <M extends Model> GraphQLRequest<M> of(Class<M> modelType, SubscriptionType type) {
        return AppSyncGraphQLRequestFactory.buildSubscription(modelType, type);
    }

    /**
     * Creates a subscription request of type {@link SubscriptionType#ON_CREATE}.
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid {@link GraphQLRequest} instance.
     * @see #of(Class, SubscriptionType)
     */
    public static <M extends Model> GraphQLRequest<M> onCreate(Class<M> modelType) {
        return of(modelType, SubscriptionType.ON_CREATE);
    }

    /**
     * Creates a subscription request of type {@link SubscriptionType#ON_DELETE}.
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid {@link GraphQLRequest} instance.
     * @see #of(Class, SubscriptionType)
     */
    public static <M extends Model> GraphQLRequest<M> onDelete(Class<M> modelType) {
        return of(modelType, SubscriptionType.ON_DELETE);
    }

    /**
     * Creates a subscription request of type {@link SubscriptionType#ON_UPDATE}.
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid {@link GraphQLRequest} instance.
     * @see #of(Class, SubscriptionType)
     */
    public static <M extends Model> GraphQLRequest<M> onUpdate(Class<M> modelType) {
        return of(modelType, SubscriptionType.ON_UPDATE);
    }

}
