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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.GraphQlBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of the {@link AppSync} client interface.
 *
 * This implementation adds business rules on top of the generic GraphQL behaviors in the
 * {@link ApiCategoryBehavior}.
 *
 * AppSync requests are formed as raw GraphQL document strings, using the
 * {@link AppSyncRequestFactory}. The requests are not generic GraphQL, and contain
 * AppSync-specific details like protocol-specific operation names (create, update, delete),
 * assumptions about the structure of data types (unique IDs, versioning information), etc.
 */
public final class AppSyncClient implements AppSync {
    private final GraphQlBehavior api;
    private final GraphQLRequest.VariablesSerializer variablesSerializer;
    private final ResponseDeserializer responseDeserializer;

    private AppSyncClient(@NonNull final GraphQlBehavior api) {
        this.api = Objects.requireNonNull(api);
        this.variablesSerializer = new AppSyncVariablesSerializer();
        this.responseDeserializer = new AppSyncResponseDeserializer();
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * @return An App Sync API instance
     */
    @NonNull
    public static AppSyncClient instance() {
        return new AppSyncClient(Amplify.API);
    }

    /**
     * Creates a new AppSyncClient.
     * @param graphQlApi A GraphQL API that knows how to talk the AppSync protocol
     * @return An AppSyncClient instance which delegates to the provided GraphQL behavior
     */
    @NonNull
    public static AppSyncClient delegatingTo(@NonNull GraphQlBehavior graphQlApi) {
        Objects.requireNonNull(graphQlApi);
        return new AppSyncClient(graphQlApi);
    }

    @SuppressWarnings("checkstyle:LineLength")
    @NonNull
    @Override
    public <T extends Model> Cancelable sync(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @NonNull Consumer<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        final String queryDoc;
        try {
            queryDoc = AppSyncRequestFactory.buildSyncDoc(modelClass, lastSync, null);
        } catch (DataStoreException queryDocConstructionError) {
            onFailure.accept(queryDocConstructionError);
            return new NoOpCancelable();
        }

        final GraphQLRequest<String> request =
            new GraphQLRequest<>(queryDoc, Collections.emptyMap(), String.class, variablesSerializer);

        final Consumer<GraphQLResponse<Iterable<String>>> responseConsumer = apiQueryResponse -> {
            if (apiQueryResponse.hasErrors()) {
                onFailure.accept(new DataStoreException(
                    "Failure performing sync query to AppSync: " + apiQueryResponse.getErrors().toString(),
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else {
                onResponse.accept(responseDeserializer.deserialize(apiQueryResponse.getData(), modelClass));
            }
        };
        final Consumer<ApiException> failureConsumer =
            failure -> onFailure.accept(new DataStoreException(
                "Failure performing sync query to AppSync.",
                failure, AmplifyException.TODO_RECOVERY_SUGGESTION
            ));

        final Cancelable cancelable = api.query(request, responseConsumer, failureConsumer);
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
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final String doc = AppSyncRequestFactory.buildCreationDoc(model.getClass());

            Class<T> modelClass = (Class<T>) model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);

            final Map<String, Object> variables =
                Collections.singletonMap("input", schema.getMapOfFieldNameAndValues(model));

            return mutation(doc, variables, modelClass, onResponse, onFailure);
        } catch (AmplifyException amplifyException) {
            onFailure.accept(new DataStoreException(
                "Error encountered while creating model schema",
                amplifyException, "See attached exception for more details"
            ));
        }

        return new NoOpCancelable();
    }

    @SuppressWarnings("unchecked") // (Class<T>)
    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final String doc = AppSyncRequestFactory.buildUpdateDoc(model.getClass());

            Class<T> modelClass = (Class<T>) model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);

            final Map<String, Object> updateInput = schema.getMapOfFieldNameAndValues(model);
            updateInput.put("_version", version);

            final Map<String, Object> variables = Collections.singletonMap("input", updateInput);

            return mutation(doc, variables, (Class<T>) model.getClass(), onResponse, onFailure);
        } catch (AmplifyException amplifyException) {
            onFailure.accept(new DataStoreException(
                "Error encountered while creating model schema",
                amplifyException, "See attached exception for more details"
            ));
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final String doc = AppSyncRequestFactory.buildDeletionDoc(clazz);

            final Map<String, Object> deleteInput = new HashMap<>();
            deleteInput.put("id", objectId);
            deleteInput.put("_version", version);

            final Map<String, Object> variables = Collections.singletonMap("input", deleteInput);

            return mutation(doc, variables, clazz, onResponse, onFailure);
        } catch (DataStoreException dataStoreException) {
            onFailure.accept(dataStoreException);
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onCreate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_CREATE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onUpdate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_UPDATE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onDelete(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_DELETE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    private <T extends Model> Cancelable subscription(
            SubscriptionType subscriptionType,
            Class<T> clazz,
            Consumer<String> onSubscriptionStarted,
            Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            Consumer<DataStoreException> onSubscriptionFailure,
            Action onSubscriptionCompleted) {
        final String document;
        try {
            document = AppSyncRequestFactory.buildSubscriptionDoc(clazz, subscriptionType);
        } catch (DataStoreException docGenerationException) {
            onSubscriptionFailure.accept(docGenerationException);
            return new NoOpCancelable();
        }
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(document, Collections.emptyMap(), String.class, variablesSerializer);

        final Consumer<GraphQLResponse<String>> stringResponseConsumer = stringResponse -> {
            if (stringResponse.hasErrors()) {
                onSubscriptionFailure.accept(new DataStoreException(
                    "Bad subscription data for " + clazz.getSimpleName() + ": " + stringResponse.getErrors(),
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else {
                onNextResponse.accept(responseDeserializer.deserialize(stringResponse.getData(), clazz));
            }
        };
        final Consumer<ApiException> failureConsumer = failure ->
            onSubscriptionFailure.accept(new DataStoreException(
                "Error during subscription.", failure, "Evaluate details."
            ));

        final Cancelable cancelable = api.subscribe(
            request,
            onSubscriptionStarted,
            stringResponseConsumer,
            failureConsumer,
            onSubscriptionCompleted
        );
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    private <T extends Model> Cancelable mutation(
            final String document,
            final Map<String, Object> variables,
            final Class<T> itemClass,
            final Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            final Consumer<DataStoreException> onFailure) {
        final GraphQLRequest<String> request =
            new GraphQLRequest<>(document, variables, String.class, variablesSerializer);

        final Consumer<GraphQLResponse<String>> responseConsumer = response -> {
            if (response.hasErrors()) {
                onResponse.accept(new GraphQLResponse<>(null, response.getErrors()));
            } else {
                onResponse.accept(responseDeserializer.deserialize(response.getData(), itemClass));
            }
        };
        final Consumer<ApiException> failureConsumer =
            failure -> onFailure.accept(new DataStoreException(
                "Failure during mutation.", failure, "Check details."
            ));
        final Cancelable cancelable = api.mutate(request, responseConsumer, failureConsumer);
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    interface ResponseDeserializer {
        <T extends Model> GraphQLResponse<ModelWithMetadata<T>> deserialize(
            String json,
            Class<T> intoClazz);

        <T extends Model> GraphQLResponse<Iterable<ModelWithMetadata<T>>> deserialize(
            Iterable<String> json,
            Class<T> memberClazz);
    }
}
