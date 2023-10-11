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
package com.amplifyframework.api.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory.buildSubscription
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SubscriptionType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPath
import com.amplifyframework.core.model.PropertyContainerPath

/**
 * Helper class that provides methods to create [GraphQLRequest] that represents
 * subscriptions from [Model].
 */
object ModelSubscription {
    /**
     * Builds a subscriptions request of a given `type` for a `modelType`.
     * @param modelType the model class.
     * @param type the subscription type.
     * @param <M> the concrete type of the model.
     * @return a valid [GraphQLRequest] instance.
     </M> */
    fun <M : Model> of(
        modelType: Class<M>,
        type: SubscriptionType,
    ): GraphQLRequest<M> {
        return buildSubscription(modelType, type)
    }

    /**
     * Builds a subscriptions request of a given `type` for a `modelType`.
     * @param modelType the model class.
     * @param type the subscription type.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete type of the model.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     </M> */
    fun <M : Model, P : ModelPath<M>> of(
        modelType: Class<M>,
        type: SubscriptionType,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildSubscription(modelType, type, includes)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_CREATE].
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    @JvmStatic
    fun <M : Model> onCreate(modelType: Class<M>): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_CREATE)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_CREATE].
     * @param modelType the model class.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete type of the model.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> onCreate(
        modelType: Class<M>,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_CREATE, includes)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_DELETE].
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    fun <M : Model> onDelete(modelType: Class<M>): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_DELETE)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_DELETE].
     * @param modelType the model class.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete type of the model.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> onDelete(
        modelType: Class<M>,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_DELETE, includes)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_UPDATE].
     * @param modelType the model class.
     * @param <M> the concrete type of the model.
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    fun <M : Model> onUpdate(modelType: Class<M>): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_UPDATE)
    }

    /**
     * Creates a subscription request of type [SubscriptionType.ON_UPDATE].
     * @param modelType the model class.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete type of the model.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see .of
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> onUpdate(
        modelType: Class<M>,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return of(modelType, SubscriptionType.ON_UPDATE, includes)
    }
}
