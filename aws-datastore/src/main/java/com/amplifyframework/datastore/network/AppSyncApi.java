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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.GraphQlBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of the {@link AppSyncEndpoint} contract,
 * which used the {@link ApiCategoryBehavior} to implement the various
 * GraphQL operations.
 */
@SuppressWarnings("unused") // Hold my beer...
public final class AppSyncApi implements AppSyncEndpoint {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final int DEFAULT_QUERY_LIMIT = 1000;

    private final GraphQlBehavior api;
    private final GraphQLRequest.VariablesSerializer variablesSerializer;
    private final ResponseDeserializer responseDeserializer;

    /**
     * Constructs a new AppSyncApi.
     * @param api The API Category, configured with a DataStore API
     */
    @VisibleForTesting
    AppSyncApi(@NonNull final GraphQlBehavior api) {
        this.api = Objects.requireNonNull(api);
        this.variablesSerializer = new GsonVariablesSerializer();
        this.responseDeserializer = new GsonResponseDeserializer();
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * @return An App Sync API instance
     */
    @NonNull
    public static AppSyncApi instance() {
        return new AppSyncApi(Amplify.API);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable sync(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @NonNull ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener) {

        final String queryDoc;
        try {
            queryDoc = AppSyncRequestFactory.buildSyncDoc(modelClass, lastSync, null);
        } catch (DataStoreException queryDocConstructionError) {
            responseListener.onError(queryDocConstructionError);
            return new NoOpCancelable();
        }

        final GraphQLRequest<String> request =
            new GraphQLRequest<>(queryDoc, Collections.emptyMap(), String.class, variablesSerializer);
        final Cancelable cancelable =
            api.query(request, SyncAdapter.instance(responseListener, modelClass, responseDeserializer));
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    @SuppressWarnings("unchecked") // (Class<T>)
    @NonNull
    @Override
    public <T extends Model> Cancelable create(
            @NonNull T model,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener) {
        final String doc;
        try {
            doc = AppSyncRequestFactory.buildCreationDoc(model.getClass());

            Class<T> modelClass = (Class<T>) model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);

            final Map<String, Object> variables = Collections.singletonMap(
                    "input",
                    schema.getMapOfFieldNameAndValues(model)
            );

            return mutation(doc, variables, modelClass, responseListener);
        } catch (AmplifyException amplifyException) {
            responseListener.onError(
                    new DataStoreException(
                            "Error encountered while creating model schema",
                            amplifyException,
                            "See attached exception for more details"
                    )
            );
        }

        return new NoOpCancelable();
    }

    @SuppressWarnings("unchecked") // (Class<T>)
    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener) {
        final String doc;
        try {
            doc = AppSyncRequestFactory.buildUpdateDoc(model.getClass());

            Class<T> modelClass = (Class<T>) model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);

            Map<String, Object> updateInput = schema.getMapOfFieldNameAndValues(model);
            updateInput.put("_version", version);

            final Map<String, Object> variables = Collections.singletonMap(
                    "input",
                    updateInput
            );

            return mutation(doc, variables, (Class<T>) model.getClass(), responseListener);
        } catch (AmplifyException amplifyException) {
            responseListener.onError(
                    new DataStoreException(
                            "Error encountered while creating model schema",
                            amplifyException,
                            "See attached exception for more details"
                    )
            );
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener) {
        final String doc;
        try {
            doc = AppSyncRequestFactory.buildDeletionDoc(clazz);

            Map<String, Object> deleteInput = new HashMap<>();
            deleteInput.put("id", objectId);
            deleteInput.put("_version", version);

            final Map<String, Object> variables = Collections.singletonMap(
                    "input",
                    deleteInput
            );

            return mutation(doc, variables, clazz, responseListener);
        } catch (DataStoreException dataStoreException) {
            responseListener.onError(dataStoreException);
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onCreate(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(modelClass, subscriptionListener, SubscriptionType.ON_CREATE);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onUpdate(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(modelClass, subscriptionListener, SubscriptionType.ON_UPDATE);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onDelete(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(modelClass, subscriptionListener, SubscriptionType.ON_DELETE);
    }

    private <T extends Model> Cancelable subscription(
            Class<T> clazz,
            StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener,
            SubscriptionType subscriptionType) {
        final String document;
        try {
            document = AppSyncRequestFactory.buildSubscriptionDoc(clazz, subscriptionType);
        } catch (DataStoreException docGenerationException) {
            subscriptionListener.onError(docGenerationException);
            return new NoOpCancelable();
        }
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(document, Collections.emptyMap(), String.class, variablesSerializer);
        final Cancelable cancelable =
            api.subscribe(request, SubscriptionAdapter.instance(subscriptionListener, clazz, responseDeserializer));
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    private <T extends Model> Cancelable mutation(
            final String document,
            final Map<String, Object> variables,
            final Class<T> itemClass,
            final ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener) {
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(document, variables, String.class, variablesSerializer);
        final Cancelable cancelable =
            api.mutate(request, MutationAdapter.instance(responseListener, itemClass, responseDeserializer));
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }
}
