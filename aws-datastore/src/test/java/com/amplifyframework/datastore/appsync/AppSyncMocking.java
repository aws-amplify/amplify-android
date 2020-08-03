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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;

/**
 * A utility to mock behaviors of an {@link AppSync} from test code.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public final class AppSyncMocking {
    private AppSyncMocking() {}

    /**
     * Prepare mocks on AppSync, to occur when a sync() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the sync() behavior.
     */
    @NonNull
    public static SyncConfigurator onSync(AppSync mock) {
        return new SyncConfigurator(mock);
    }

    /**
     * Prepare mocks on AppSync, to occur when a delete() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the delete() behavior.
     */
    @NonNull
    public static DeleteConfigurator onDelete(AppSync mock) {
        return new DeleteConfigurator(mock);
    }

    /**
     * Prepare mocks on AppSync, to occur when a create() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the create() behavior.
     */
    @NonNull
    public static CreateConfigurator onCreate(AppSync mock) {
        return new CreateConfigurator(mock);
    }

    /**
     * Configures mock behaviors to occur when create() is invoked.
     */
    public static final class CreateConfigurator {
        private AppSync appSync;

        /**
         * Constructs a CreateConfigurator, bound to a mock AppSync instance.
         * @param appSync A mock of the AppSync interface
         */
        CreateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * Mocks a response to the create() API. The mock will call back the the success consumer.
         * The provided value is a ModelWithMetadata. The model is the one passed to the mock.
         * The metadata simply echos the model ID and includes the current time.
         * @param model When this model is received, mock is enacted. This model is passed back in response.
         * @param <T> Type of model
         * @return A create configurator
         */
        public <T extends Model> CreateConfigurator mockResponse(T model) {
            doAnswer(invocation -> {
                // Simulate a successful response callback from the create() method.
                final int indexOfModelBeingCreated = 0;
                final int indexOfResultConsumer = 1;
                T capturedModel = invocation.getArgument(indexOfModelBeingCreated);

                // Pass back a ModelWithMetadata. Model is the one provided.
                ModelMetadata metadata =
                    new ModelMetadata(capturedModel.getId(), false, 1, new Temporal.Timestamp());
                ModelWithMetadata<T> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
                Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResult =
                    invocation.getArgument(indexOfResultConsumer);
                onResult.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));

                // Technically, create() returns a Cancelable...
                return new NoOpCancelable();
            }).when(appSync).create(
                eq(model),
                any(), // onResponse
                any() // onFailure
            );
            return CreateConfigurator.this;
        }
    }

    /**
     * Configures mocked behavior when the AppSync delete() API is exercised.
     */
    public static final class DeleteConfigurator {
        private AppSync appSync;

        /**
         * Constructs a DeleteConfigurator.
         * @param appSync A mock instance of AppSync.
         */
        DeleteConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * Mocks a response to the delete() API. The mock will call back the the success consumer.
         * The provided value is a ModelWithMetadata. The model is the one passed to the mock.
         * The metadata simply echos the model ID and includes the current time, and includes
         * the _delete == true flag.
         * @param model When this model is received, mock is enacted. This model is passed back in response.
         * @param <T> Type of model
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> DeleteConfigurator mockResponse(T model) {
            doAnswer(invocation -> {
                // Simulate a successful response callback from the delete() method.
                final int indexOfModelId = 1;
                final int indexOfVersion = 2;
                final int indexOfResultConsumer = 4;
                Consumer<GraphQLResponse<ModelWithMetadata<? extends Model>>> onResult =
                    invocation.getArgument(indexOfResultConsumer);

                String modelId = invocation.getArgument(indexOfModelId);
                int version = invocation.getArgument(indexOfVersion);
                ModelMetadata metadata = new ModelMetadata(modelId, true, version, Temporal.Timestamp.now());
                ModelWithMetadata<? extends Model> modelWithMetadata = new ModelWithMetadata<>(model, metadata);

                onResult.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));

                // Technically, delete() returns a Cancelable...
                return new NoOpCancelable();
            }).when(appSync).delete(
                eq(model.getClass()), // Class of the model
                eq(model.getId()), // model ID
                anyInt(), // version
                any(), // predicate
                any(), // onResponse
                any() // onFailure
            );
            return this;
        }
    }

    /**
     * Configures mocking for a particular {@link AppSync} mock.
     */
    public static final class SyncConfigurator {
        private AppSync appSync;

        /**
         * Constructs a new SyncConfigurator.
         * @param appSync A mock AppSync instance
         */
        SyncConfigurator(AppSync appSync) {
            this.appSync = appSync;
            this.mockSuccessResponses();
        }

        /**
         * By default, return an empty list of items when attempting to sync any/all Model classes.
         * @return Configurator instance
         */
        @NonNull
        public SyncConfigurator mockSuccessResponses() {
            doAnswer(invocation -> {
                // Get a handle to the response consumer that is passed into the sync() method
                // Response consumer is the third param, at index 2 (@0, @1, @2, @3).
                final int argumentPositionForResponseConsumer = 2;
                final Consumer<GraphQLResponse<Iterable<ModelWithMetadata<? extends Model>>>> consumer =
                    invocation.getArgument(argumentPositionForResponseConsumer);

                // Call the response consumer, and pass EMPTY items inside of a GraphQLResponse wrapper
                consumer.accept(new GraphQLResponse<>(Collections.emptyList(), Collections.emptyList()));

                // Return a NoOp cancelable via the sync() method's return.
                return new NoOpCancelable();
            }).when(appSync).sync(
                any(), // Item class to sync
                any(), // last sync time
                any(), // Consumer<Iterable<ModelWithMetadata<T>>>
                any() // Consumer<DataStoreException>
            );
            return this;
        }

        /**
         * Creates an instance of an {@link AppSync}, which will provide a fake response when asked to
         * to {@link AppSync#sync(Class, Long, Consumer, Consumer)}. The response callback will
         * be invoked, and will contain the provided ModelWithMetadata in its response.
         * @param modelClass Class of models for which the endpoint should respond
         * @param responseItems The items that should be included in the mocked response, for the model class
         * @param <T> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public final <T extends Model> SyncConfigurator mockSuccessResponse(
                Class<T> modelClass, ModelWithMetadata<T>... responseItems) {
            doAnswer(invocation -> {
                // Get a handle to the response consumer that is passed into the sync() method
                // Response consumer is the third param, at index 2 (@0, @1, @2, @3).
                final int argumentPositionForResponseConsumer = 2;
                final Consumer<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> consumer =
                    invocation.getArgument(argumentPositionForResponseConsumer);

                // Call the response consumer, and pass the mocked items
                // inside of a GraphQLResponse wrapper
                final Iterable<ModelWithMetadata<T>> data = new HashSet<>(Arrays.asList(responseItems));
                consumer.accept(new GraphQLResponse<>(data, Collections.emptyList()));

                // Return a NoOp cancelable via the sync() method's return.
                return new NoOpCancelable();
            }).when(appSync).sync(
                eq(modelClass), // Item class to sync
                any(), // last sync time
                any(), // Consumer<Iterable<ModelWithMetadata<T>>>
                any() // Consumer<DataStoreException>
            );
            return SyncConfigurator.this;
        }

        /**
         * Triggers an exception when invoking the sync method.
         * @param dataStoreException The exception that will be used for the mock.
         * @param <T> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        public <T extends Model> SyncConfigurator mockFailure(DataStoreException dataStoreException) {
            doAnswer(invocation -> {
                final int errorConsumerPosition = 3;
                final Consumer<DataStoreException> consumer = invocation.getArgument(errorConsumerPosition);
                consumer.accept(dataStoreException);
                return new NoOpCancelable();
            }).when(appSync).sync(
                any(), // Item class to sync
                any(), // last sync time
                any(), // Consumer<Iterable<ModelWithMetadata<T>>>
                any() // Consumer<DataStoreException>
            );
            return this;
        }
    }
}
