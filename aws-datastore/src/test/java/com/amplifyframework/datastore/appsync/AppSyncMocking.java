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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.Time;

import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * A utility to mock behaviors of an {@link AppSync} from test code.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
public final class AppSyncMocking {
    private AppSyncMocking() {}

    /**
     * Prepare mocks on AppSync, to occur when a sync() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the sync() behavior.
     * @throws DataStoreException if a ModelSchema cannot be created in order to build the sync request.
     */
    @NonNull
    public static SyncConfigurator sync(AppSync mock) throws DataStoreException {
        return new SyncConfigurator(mock);
    }

    /**
     * Prepare mocks on AppSync, to occur when a delete() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the delete() behavior.
     */
    @NonNull
    public static DeleteConfigurator delete(AppSync mock) {
        return new DeleteConfigurator(mock);
    }

    /**
     * Prepare mocks on AppSync, to occur when a create() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the create() behavior.
     */
    @NonNull
    public static CreateConfigurator create(AppSync mock) {
        return new CreateConfigurator(mock);
    }

    /**
     * Prepares mocks on AppSync, to occur when an onCreate() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onCreate() subscription
     */
    public static OnCreateConfigurator onCreate(AppSync mock) {
        return new OnCreateConfigurator(mock);
    }

    /**
     * Prepares mocks on AppSync, to occur when an onUpdate() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onUpdate() subscription
     */
    public static OnUpdateConfigurator onUpdate(AppSync mock) {
        return new OnUpdateConfigurator(mock);
    }

    /**
     * Prepares mocks on AppSync, to occur when an onDelete() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onDelete() subscription
     */
    public static OnDeleteConfigurator onDelete(AppSync mock) {
        return new OnDeleteConfigurator(mock);
    }

    /**
     * Configures mock behaviors to occur when create() is invoked.
     */
    public static final class CreateConfigurator {
        private final AppSync appSync;

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
                    new ModelMetadata(capturedModel.getId(), false, 1, Time.now());
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
        private final AppSync appSync;

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
                ModelMetadata metadata = new ModelMetadata(modelId, true, version, Time.now());
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
        private final AppSync appSync;

        /**
         * Constructs a new SyncConfigurator.
         * @param appSync A mock AppSync instance
         * @throws DataStoreException if a ModelSchema cannot be created in order to build the sync request.
         */
        SyncConfigurator(AppSync appSync) throws DataStoreException {
            this.appSync = appSync;
            this.mockBuildSyncRequest();
            this.mockSuccessResponses();
        }

        private <M extends Model> SyncConfigurator mockBuildSyncRequest() throws DataStoreException {
            when(appSync.buildSyncRequest(any(), any(), any()))
                    .thenAnswer((Answer<GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>>>) invocation -> {
                        Class<M> modelClass = invocation.getArgument(0);
                        Long lastSync = invocation.getArgument(1);
                        Integer syncPageSize = invocation.getArgument(2);
                        return AppSyncRequestFactory.buildSyncRequest(modelClass, lastSync, syncPageSize);
                    });
            return this;
        }

        /**
         * By default, return an empty list of items when attempting to sync any/all Model classes.
         * @param <M> Type of model for which a response is mocked.
         * @return Configurator instance
         */
        @NonNull
        public <M extends Model> SyncConfigurator mockSuccessResponses() {
            return mockSuccessResponse(
                arg -> true, // Match all GraphQLRequest objects.
                new GraphQLResponse<>(
                    new PaginatedResult<>(Collections.emptyList(), null),
                    Collections.emptyList()
                )
            );
        }

        /**
         * Configures an instance of an {@link AppSync} to provide a fake response when asked to
         * to {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)}. The response callback will
         * be invoked, and will contain the provided ModelWithMetadata in its response.
         * @param modelClass Class of models for which the endpoint should respond
         * @param responseItems The items that should be included in the mocked response, for the model class
         * @param <M> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public final <M extends Model> SyncConfigurator mockSuccessResponse(
                Class<M> modelClass, ModelWithMetadata<M>... responseItems) {
            return mockSuccessResponse(
                    matchesRequest(modelClass, null),
                    new GraphQLResponse<>(
                            new PaginatedResult<>(new HashSet<>(Arrays.asList(responseItems)), null),
                            Collections.emptyList()
                    )
            );
        }

        /**
         * Configures an instance of an {@link AppSync} to invoke the response callback when asked to
         * {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)}, with the ability to specify a nextToken to match,
         * and a nextToken to return in the response, for testing pagination.
         * @param modelClass Class of models for which the endpoint should respond
         * @param token nextToken to be expected on the GraphQLRequest for which the endpoint should respond.
         * @param nextToken nextToken that should be used to build the requestForNextResult on the GraphQLResponse.
         * @param responseItems The items that should be included in the mocked response, for the model class
         * @param <M> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         * @throws AmplifyException if a ModelSchema cannot be created in order to build the sync request.
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public final <M extends Model> SyncConfigurator mockSuccessResponse(
                Class<M> modelClass,
                String token,
                String nextToken,
                ModelWithMetadata<M>... responseItems) throws AmplifyException {
            final Iterable<ModelWithMetadata<M>> items = new HashSet<>(Arrays.asList(responseItems));
            AppSyncGraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> requestForNextResult = null;
            if (nextToken != null) {
                requestForNextResult = AppSyncRequestFactory.buildSyncRequest(modelClass, null, null)
                        .newBuilder()
                        .variable("nextToken", "String", nextToken)
                        .build();
            }
            return mockSuccessResponse(
                    matchesRequest(modelClass, token),
                    new GraphQLResponse<>(
                            new PaginatedResult<>(items, requestForNextResult),
                            Collections.emptyList()
                    )
            );
        }

        /**
         * Configures an instance of an {@link AppSync} to invoke the response callback with the provided mockResponse
         * when asked to {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)}.
         * @param matchesRequest ArgumentMatcher which returns true if the GraphQLRequest should be mocked.
         * @param mockResponse GraphQLResponse to be passed back in the response callback.
         * @param <M> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        public <M extends Model> SyncConfigurator mockSuccessResponse(
                ArgumentMatcher<GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>>> matchesRequest,
                GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>> mockResponse) {
            doAnswer(invocation -> {
                // Get a handle to the response consumer that is passed into the sync() method
                // Response consumer is the second param, at index 1 (@0, @1, @2).
                final int argumentPositionForResponseConsumer = 1;
                final Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>>> consumer =
                    invocation.getArgument(argumentPositionForResponseConsumer);

                // Call the response consumer, and pass the mocked response
                consumer.accept(mockResponse);

                // Return a NoOp cancelable via the sync() method's return.
                return new NoOpCancelable();
            }).when(appSync).sync(
                argThat(matchesRequest),
                any(), // Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>>>
                any()  // Consumer<DataStoreException>
            );
            return SyncConfigurator.this;
        }

        private <M extends Model> ArgumentMatcher<GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>>> matchesRequest(
                Class<M> modelClass, String nextToken) {
            return graphQLRequest -> {
                if (graphQLRequest instanceof AppSyncGraphQLRequest) {
                    AppSyncGraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> request =
                            (AppSyncGraphQLRequest<PaginatedResult<ModelWithMetadata<M>>>) graphQLRequest;
                    return ObjectsCompat.equals(request.getModelSchema().getName(), modelClass.getSimpleName())
                            && ObjectsCompat.equals(request.getVariables().get("nextToken"), nextToken);
                }
                return false;
            };
        }

        /**
         * Triggers an exception when invoking the sync method.
         * @param dataStoreException The exception that will be used for the mock.
         * @param <M> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        public <M extends Model> SyncConfigurator mockFailure(DataStoreException dataStoreException) {
            doAnswer(invocation -> {
                // Error consumer is the third param, at index 2 (@0, @1, @2).
                final int errorConsumerPosition = 2;
                final Consumer<DataStoreException> consumer = invocation.getArgument(errorConsumerPosition);
                consumer.accept(dataStoreException);
                return new NoOpCancelable();
            }).when(appSync).sync(
                any(), // GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>>
                any(), // Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<M>>>>
                any() // Consumer<DataStoreException>
            );
            return this;
        }
    }

    /**
     * Configures mock behaviors on an {@link AppSync} instance, to occur when
     * {@link AppSync#onCreate(Class, Consumer, Consumer, Consumer, Action)} is invoked.
     */
    public static final class OnCreateConfigurator {
        private final AppSync appSync;

        OnCreateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onCreate(Class, Consumer, Consumer, Consumer, Action)} being
         * called, the onStart consumer will be called back immediately.
         * @return The current configurator instance, for fluent method chaining
         */
        public OnCreateConfigurator callOnStart() {
            doAnswer(invocation -> {
                final int indexOfOnStart = 1;
                Consumer<String> onStart = invocation.getArgument(indexOfOnStart);
                onStart.accept(RandomString.string());
                return null;
            }).when(appSync).onCreate(
                any(), // Class<M>
                any(), // Consumer<String>, onStart
                any(), // Consumer<GraphQLResponse<ModelWithMetadata<M>>>, onNextResponse
                any(), // Consumer<DataStoreException>, onSubscriptionFailure
                any() // Action, onSubscriptionCompleted
            );
            return this;
        }
    }

    /**
     * Configured mocked behaviors on an {@link AppSync} instance,
     * to be invoked when {@link AppSync#onUpdate(Class, Consumer, Consumer, Consumer, Action)}
     * is called.
     */
    public static final class OnUpdateConfigurator {
        private final AppSync appSync;

        OnUpdateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onUpdate(Class, Consumer, Consumer, Consumer, Action)} being
         * called, the onStart consumer will be called back immediately.
         * @return The current configurator instance, for fluent method chaining
         */
        public OnUpdateConfigurator callOnStart() {
            doAnswer(invocation -> {
                final int indexOfOnStart = 1;
                Consumer<String> onStart = invocation.getArgument(indexOfOnStart);
                onStart.accept(RandomString.string());
                return null;
            }).when(appSync).onUpdate(
                any(), // Class<T>
                any(), // Consumer<String>, onStart
                any(), // Consumer<GraphQLResponse<ModelWithMetadata<T>>>, onNextResponse
                any(), // Consumer<DataStoreException>, onSubscriptionFailure
                any() // Action, onSubscriptionCompleted
            );
            return this;
        }
    }

    /**
     * Configures mock behaviors for when {@link AppSync#onDelete(Class, Consumer, Consumer, Consumer, Action)}
     * is called.
     */
    public static final class OnDeleteConfigurator {
        private final AppSync appSync;

        OnDeleteConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onDelete(Class, Consumer, Consumer, Consumer, Action)} being
         * called, the onStart consumer will be called back immediately.
         * @return The current configurator instance, for fluent method chaining
         */
        public OnDeleteConfigurator callOnStart() {
            doAnswer(invocation -> {
                final int indexOfOnStart = 1;
                Consumer<String> onStart = invocation.getArgument(indexOfOnStart);
                onStart.accept(RandomString.string());
                return null;
            }).when(appSync).onDelete(
                any(), // Class<T>
                any(), // Consumer<String>, onStart
                any(), // Consumer<GraphQLResponse<ModelWithMetadata<T>>>, onNextResponse
                any(), // Consumer<DataStoreException>, onSubscriptionFailure
                any() // Action, onSubscriptionCompleted
            );
            return this;
        }
    }
}
