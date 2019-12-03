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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
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

    private final ApiCategoryBehavior api;
    private final GraphQLRequest.VariablesSerializer variablesSerializer;
    private final ResponseDeserializer responseDeserializer;

    /**
     * Constructs a new AppSyncApi.
     * @param api The API Category, configured with a DataStore API
     */
    public AppSyncApi(@NonNull final ApiCategoryBehavior api) {
        this.api = Objects.requireNonNull(api);
        this.variablesSerializer = new GsonVariablesSerializer();
        this.responseDeserializer = new GsonResponseDeserializer();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable sync(
            @NonNull String apiName,
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

        final SyncAdapter<T> syncAdapter =
            new SyncAdapter<>(responseListener, modelClass, responseDeserializer);
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(queryDoc, Collections.emptyMap(), String.class, variablesSerializer);
        return api.query(apiName, request, syncAdapter);
    }

    @SuppressWarnings("unchecked") // (Class<T>)
    @NonNull
    @Override
    public <T extends Model> Cancelable create(
            @NonNull String apiName,
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

            return mutation(apiName, doc, variables, modelClass, responseListener);
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
            @NonNull String apiName,
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

            return mutation(apiName, doc, variables, (Class<T>) model.getClass(), responseListener);
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
            @NonNull String apiName,
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

            return mutation(apiName, doc, variables, clazz, responseListener);
        } catch (DataStoreException dataStoreException) {
            responseListener.onError(dataStoreException);
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onCreate(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(apiName, modelClass, subscriptionListener, SubscriptionType.ON_CREATE);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onUpdate(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(apiName, modelClass, subscriptionListener, SubscriptionType.ON_UPDATE);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onDelete(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener) {
        return subscription(apiName, modelClass, subscriptionListener, SubscriptionType.ON_DELETE);
    }

    private <T extends Model> Cancelable subscription(
            String apiName,
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
        final SubscriptionAdapter<T> subscriptionAdapter =
            new SubscriptionAdapter<>(subscriptionListener, clazz, responseDeserializer);
        return api.subscribe(apiName, request, subscriptionAdapter);
    }

    private <T extends Model> Cancelable mutation(
            final String apiName,
            final String document,
            final Map<String, Object> variables,
            final Class<T> itemClass,
            final ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener) {
        final MutationAdapter<T> mutationAdapter =
            new MutationAdapter<>(responseListener, itemClass, responseDeserializer);
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(document, variables, String.class, variablesSerializer);
        return api.mutate(apiName, request, mutationAdapter);
    }
}
