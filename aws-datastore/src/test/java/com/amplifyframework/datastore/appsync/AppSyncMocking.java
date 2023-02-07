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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testutils.Varargs;
import com.amplifyframework.testutils.random.RandomString;

import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Stubber;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;

/**
 * A utility to mock behaviors of an {@link AppSync} from test code.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
public final class AppSyncMocking {
    private AppSyncMocking() {}

    /**
     * Prepare mocks on AppSync, to occur when a sync() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the sync() behavior.
     * @throws DataStoreException if a ModelSchema cannot be created in order to build the sync request.
     */
    @NonNull
    public static SyncConfigurator sync(@NonNull AppSync mock) throws DataStoreException {
        return new SyncConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepare mocks on AppSync, to occur when a create() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the create() behavior.
     */
    @NonNull
    public static CreateConfigurator create(@NonNull AppSync mock) {
        return new CreateConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepare mocks on AppSync, to occur when a update() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the update() behavior.
     */
    @NonNull
    public static UpdateConfigurator update(@NonNull AppSync mock) {
        return new UpdateConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepare mocks on AppSync, to occur when a delete() call is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the delete() behavior.
     */
    @NonNull
    public static DeleteConfigurator delete(@NonNull AppSync mock) {
        return new DeleteConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepares mocks on AppSync, to occur when an onCreate() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onCreate() subscription
     */
    @NonNull
    public static OnCreateConfigurator onCreate(@NonNull AppSync mock) {
        return new OnCreateConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepares mocks on AppSync, to occur when an onUpdate() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onUpdate() subscription
     */
    public static OnUpdateConfigurator onUpdate(@NonNull AppSync mock) {
        return new OnUpdateConfigurator(Objects.requireNonNull(mock));
    }

    /**
     * Prepares mocks on AppSync, to occur when an onDelete() subscription
     * request is made.
     * @param mock A mock of the AppSync interface
     * @return A configurator for the onDelete() subscription
     */
    @NonNull
    public static OnDeleteConfigurator onDelete(@NonNull AppSync mock) {
        return new OnDeleteConfigurator(Objects.requireNonNull(mock));
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
         * When the AppSync create() method is invoked with the provided model,
         * it will respond with the provided response.
         * @param model When this model is seen on the AppSync create(),
         * @param response This response is emitted on the response callback
         * @param <T> Type of model
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> CreateConfigurator mockResponse(
                @NonNull T model, @NonNull GraphQLResponse<ModelWithMetadata<T>> response) {
            Objects.requireNonNull(model);
            Objects.requireNonNull(response);
            callOnSuccess(/* onSuccess position = */ 2, response)
                .when(appSync)
                .create(eq(model), /* schema */ any(), /* onResponse */ any(), /* onFailure */ any());
            return CreateConfigurator.this;
        }

        /**
         * When the AppSync create() method is invoked with the provided model,
         * it will respond with the provided failure.
         * @param model When this model is seen on the AppSync create(),
         * @param error This error is emitted on the onFailure
         * @param <T> Type of model
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> CreateConfigurator mockResponseFailure(
                @NonNull T model, @NonNull Throwable error) {
            Objects.requireNonNull(model);
            Objects.requireNonNull(error);
            callOnFailure(/* onFailure position = */ 3, error)
                .when(appSync)
                .create(eq(model), /* schema */ any(), /* onResponse */ any(), /* onFailure */ any());
            return CreateConfigurator.this;
        }

        @SuppressWarnings("SameParameterValue")
        private static <T extends Model> Stubber callOnSuccess(
                int positionOfOnSuccess, GraphQLResponse<ModelWithMetadata<T>> response) {
            return doAnswer(invocation -> {
                // Simulate a successful response callback from the create() method.
                Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResult =
                    invocation.getArgument(positionOfOnSuccess);
                onResult.accept(response);
                // Technically, create() returns a Cancelable...
                return new NoOpCancelable();
            });
        }

        private static <T extends Model> Stubber callOnFailure(
                int positionOfOnFailure, Throwable error) {
            return doAnswer(invocation -> {
                // Simulate a failure response callback from the create() method.
                Consumer<Throwable> onFailure =
                    invocation.getArgument(positionOfOnFailure);
                onFailure.accept(error);
                // Technically, create() returns a Cancelable...
                return new NoOpCancelable();
            });
        }

        /**
         * When the AppSync create() method is invoked with the provided model,
         * return a successful GraphQLResponse that contains the given ModelWithMetadata
         * in the response data.
         * @param model When this model is passed to the AppSync create() method
         * @param modelWithMetadata Return this as data in a successful GraphQLResponse
         * @param <T> The type of model being created
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> CreateConfigurator mockSuccessResponse(
                @NonNull T model, @NonNull ModelWithMetadata<T> modelWithMetadata) {
            return mockResponse(model, new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
        }

        /**
         * When the AppSync create() method is invoked with the provided model,
         * return a canned successful response containing a ModelWithMetadata
         * in the response data. The metadata will show the same ID as the provided
         * model, and that the model is *not* deleted, has version 1, and was last
         * changed just now.
         * @param model When the AppSync create() API sees this model, respond
         *               with a reasonable "ok, it was created" response.
         * @param <T> The type of model being created
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> CreateConfigurator mockSuccessResponse(@NonNull T model) {
            ModelMetadata metadata = new ModelMetadata(model.getPrimaryKeyString(), false, 1, Temporal.Timestamp.now());
            ModelWithMetadata<T> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
            return mockSuccessResponse(model, modelWithMetadata);
        }

        /**
         * When the AppSync create() method is invoked with the provided model,
         * return a response containing no data, and instead containing the provided
         * GraphQLResponse.Errors in the error list.
         * @param model When the AppSync create() method is invoked with this model
         * @param errors Respond with these GraphQLResponse.Errors.
         * @param <T> Type of model being created
         * @return A create configurator
         */
        @NonNull
        public <T extends Model> CreateConfigurator mockErrorResponse(
                @NonNull T model, @Nullable GraphQLResponse.Error... errors) {
            return mockResponse(model, new GraphQLResponse<>(null, Varargs.toList(errors)));
        }
    }

    /**
     * Configures mock behaviors to occur when update() is invoked.
     */
    public static final class UpdateConfigurator {
        private final AppSync appSync;

        /**
         * Constructs a UpdateConfigurator, bound to a mock AppSync instance.
         * @param appSync A mock of the AppSync interface
         */
        UpdateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * When the given model and version are seen on the AppSync update() API,
         * the provided response will be emitted to the success callback.
         * @param model When we see this model,
         * @param version And this version,
         * @param response This response is emitted on the response callback
         * @param <T> Type of model
         * @return An update configurator
         */
        public <T extends Model> UpdateConfigurator mockResponse(
                @NonNull T model, int version, @NonNull GraphQLResponse<ModelWithMetadata<T>> response) {
            callOnSuccess(/* argument position = */ 4, response)
                .when(appSync).update(
                    eq(model),
                    any() /* schema */,
                    eq(version),
                    any() /* predicate */,
                    any() /* onResponse */,
                    any() /* onFailure */
                );
            callOnSuccess(/* argument position = */ 3, response)
                .when(appSync).update(
                    eq(model),
                    any() /* schema */,
                    eq(version),
                    any() /* onSuccess */,
                    any() /* onError */
                );
            return UpdateConfigurator.this;
        }

        private static <T extends Model> Stubber callOnSuccess(
                int position, GraphQLResponse<ModelWithMetadata<T>> response) {
            return doAnswer(invocation -> {
                Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResult =
                    invocation.getArgument(position);
                onResult.accept(response);
                // Technically, create() returns a Cancelable...
                return new NoOpCancelable();
            });
        }

        /**
         * When the AppSync update() API is invoked with the provided model and version,
         * respond with a GraphQLResponse containing the provided ModelWithMetadata in the
         * response data, and no error in the error list.
         * @param model When this model is provided to the update() API
         * @param version Along with this version
         * @param modelWithMetadata Then return this object in the response data
         * @param <T> Type of model being updated
         * @return An update configurator
         */
        @NonNull
        public <T extends Model> UpdateConfigurator mockSuccessResponse(
                @NonNull T model, int version, @NonNull ModelWithMetadata<T> modelWithMetadata) {
            return mockResponse(model, version, new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
        }

        /**
         * When the AppSync update() API is invoked with the provided model and version,
         * emit a successful GraphQLResponse containing a cooked ModelWithMetadata in the response
         * data, and no error in the error list. The ModelWithMetadata will contain
         * the model's ID, deleted as false, the model's version plus 1, and the current time
         * as the last change time.
         * @param model When this model is provided to the update() API
         * @param version And with this version
         * @param <T> The type of model being updated
         * @return An update configurator
         */
        @NonNull
        public <T extends Model> UpdateConfigurator mockSuccessResponse(@NonNull T model, int version) {
            Temporal.Timestamp lastChangedAt = Temporal.Timestamp.now();
            ModelMetadata metadata = new ModelMetadata(model.getPrimaryKeyString(), false, version + 1,
                    lastChangedAt);
            ModelWithMetadata<T> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
            return mockSuccessResponse(model, version, modelWithMetadata);
        }

        /**
         * When the AppSync update() API is invoked with the provided model
         * and version, emit a GraphQLResponse that contains no data, but instead
         * contains the provided GraphQLResponse.Errors in a list.
         * @param model When this model is seen on the update() API,
         * @param version accompanied with this version,
         * @param errors emit a GraphQLResponse with these errors (and no response data)
         * @param <T> The type of model being updated
         * @return An update configurator
         */
        @NonNull
        public <T extends Model> UpdateConfigurator mockErrorResponse(
                @NonNull T model, int version, @Nullable GraphQLResponse.Error... errors) {
            return mockResponse(model, version, new GraphQLResponse<>(null, Varargs.toList(errors)));
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
         * When the AppSync delete() API is invoked with the provided model
         * and version, emit the provided GraphQLResponse in response.
         * @param model When this model is seen on the delete() API,
         * @param version accompanied by this version
         * @param response Then respond with this response
         * @param <T> The type of model being deleted
         * @return A delete configurator
         */
        @NonNull
        public <T extends Model> DeleteConfigurator mockResponse(
                @NonNull T model, int version, @NonNull GraphQLResponse<ModelWithMetadata<T>> response) {
            callOnSuccess(/* onSuccess position = */ 4, response)
                .when(appSync).delete(
                    eq(model), // model
                    any(), // ModelSchema
                    eq(version), // version
                    any(), // predicate
                    any(), // onResponse
                    any() // onFailure
                );
            callOnSuccess(/* onSuccess position = */ 3, response)
                .when(appSync).delete(
                    eq(model), // model
                    any(), // ModelSchema
                    eq(version), // version
                    any(), // onResponse
                    any() // onFailure
                );
            return DeleteConfigurator.this;
        }

        private static <T extends Model> Stubber callOnSuccess(
                int positionOfOnSuccess, GraphQLResponse<ModelWithMetadata<T>> response) {
            return doAnswer(invocation -> {
                Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResult =
                    invocation.getArgument(positionOfOnSuccess);
                onResult.accept(response);
                // Technically, delete() returns a Cancelable...
                return new NoOpCancelable();
            });
        }

        /**
         * When the AppSync delete() API is invoked with the provided model and version,
         * respond with a GraphQLResponse containing the provided ModelWithMetadata
         * as response data. The response's error list will be empty.
         * @param model When the delete() API is invoked with this model
         * @param version And with this version
         * @param modelWithMetadata Emit a successful GraphQLResponse containing
         *                            no errors, and containing this item as reponse data
         * @param <T> The type of model being deleted
         * @return A delete configurator
         */
        @NonNull
        public <T extends Model> DeleteConfigurator mockSuccessResponse(
                @NonNull T model, int version, @NonNull ModelWithMetadata<T> modelWithMetadata) {
            GraphQLResponse<ModelWithMetadata<T>> response =
                new GraphQLResponse<>(modelWithMetadata, Collections.emptyList());
            return mockResponse(model, version, response);
        }

        /**
         * When the AppSync delete() API is invoked with the provided model and version,
         * emit a successful response with reasonable defaults. The response will contain
         * a ModelWithMetadata as response data, and no error in the error list. The ModelWithMetadata
         * will contain the model ID, the provided version, true for the isDeleted, and the
         * current time as the last changed time.
         * @param model When the delete() is called with this model,
         * @param version and this version, then emit a default success response
         * @param <T> The type of model being deleted
         * @return A delete configurator
         */
        @NonNull
        public <T extends Model> DeleteConfigurator mockSuccessResponse(@NonNull T model, int version) {
            Temporal.Timestamp lastChangedAt = Temporal.Timestamp.now();
            ModelMetadata metadata = new ModelMetadata(model.getPrimaryKeyString(), true, version + 1,
                    lastChangedAt);
            ModelWithMetadata<T> modelWithMetadata = new ModelWithMetadata<>(model, metadata);
            return mockSuccessResponse(model, version, modelWithMetadata);
        }

        /**
         * When the AppSync delete() API is invoked, emit a GraphQLResponse containing
         * no data, but instead containing the provided list of GraphQLResponse.Errors.
         * @param model When this model is seen on the delete() API,
         * @param version And with this version
         * @param errors Then emit a GraphQLResponse containing no data, but containing
         *                these errors in the error list
         * @param <T> The type of model being deleted
         * @return A delete configurator
         */
        @NonNull
        public <T extends Model> DeleteConfigurator mockErrorResponse(
                @NonNull T model, int version, @Nullable GraphQLResponse.Error... errors) {
            return mockResponse(model, version, new GraphQLResponse<>(null, Varargs.toList(errors)));
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
            doAnswer(invocation -> {
                ModelSchema schema = invocation.getArgument(0);
                Long lastSync = invocation.getArgument(1);
                Integer syncPageSize = invocation.getArgument(2);
                QueryPredicate queryPredicate = invocation.getArgument(3);
                return AppSyncRequestFactory.buildSyncRequest(schema,
                                                              lastSync,
                                                              syncPageSize,
                                                              queryPredicate,
                                                              AuthModeStrategyType.DEFAULT);
            }).when(appSync).buildSyncRequest(any(), any(), any(), any());
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
            return mockSuccessResponse(modelClass, null, null, Arrays.asList(responseItems),
                    Collections.emptyList());
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
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public final <M extends Model> SyncConfigurator mockSuccessResponse(
                Class<M> modelClass,
                String token,
                String nextToken,
                ModelWithMetadata<M>... responseItems) {
            return mockSuccessResponse(modelClass, token, nextToken, Arrays.asList(responseItems),
                    Collections.emptyList());
        }

        /**
         * Configures an instance of an {@link AppSync} to invoke the response callback when asked to
         * {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)}, with the ability to specify a nextToken to match,
         * a nextToken to return in the response and errors, for testing pagination.
         * @param modelClass Class of models for which the endpoint should respond
         * @param token nextToken to be expected on the GraphQLRequest for which the endpoint should respond.
         * @param nextToken nextToken that should be used to build the requestForNextResult on the GraphQLResponse.
         * @param responseItems The items that should be included in the mocked response, for the model class
         * @param errors The errors that should be included in the mocked response.
         * @param <M> Type of models for which a response is mocked
         * @return The same Configurator instance, to enable chaining of calls
         */
        public <M extends Model> SyncConfigurator mockSuccessResponse(
                Class<M> modelClass,
                String token,
                String nextToken,
                List<ModelWithMetadata<M>> responseItems,
                List<GraphQLResponse.Error> errors) {
            final Iterable<ModelWithMetadata<M>> items = new HashSet<>(responseItems);
            AppSyncGraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> requestForNextResult = null;
            if (nextToken != null) {
                try {
                    ModelSchema schema = ModelSchema.fromModelClass(modelClass);
                    requestForNextResult =
                        AppSyncRequestFactory.buildSyncRequest(schema,
                                                               null,
                                                               null,
                                                               QueryPredicates.all(),
                                                               AuthModeStrategyType.DEFAULT)
                            .newBuilder()
                            .variable("nextToken", "String", nextToken)
                            .build();
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
            }
            return mockSuccessResponse(
                    matchesRequest(modelClass, token),
                    new GraphQLResponse<>(
                            new PaginatedResult<>(items, requestForNextResult),
                            errors
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
     * {@link AppSync#onCreate(ModelSchema, Consumer, Consumer, Consumer, Action)} is invoked.
     */
    public static final class OnCreateConfigurator {
        private final AppSync appSync;

        OnCreateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onCreate(ModelSchema, Consumer, Consumer, Consumer, Action)} being
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
     * to be invoked when {@link AppSync#onUpdate(ModelSchema, Consumer, Consumer, Consumer, Action)}
     * is called.
     */
    public static final class OnUpdateConfigurator {
        private final AppSync appSync;

        OnUpdateConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onUpdate(ModelSchema, Consumer, Consumer, Consumer, Action)} being
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
     * Configures mock behaviors for when {@link AppSync#onDelete(ModelSchema, Consumer, Consumer, Consumer, Action)}
     * is called.
     */
    public static final class OnDeleteConfigurator {
        private final AppSync appSync;

        OnDeleteConfigurator(AppSync appSync) {
            this.appSync = appSync;
        }

        /**
         * In response to {@link AppSync#onDelete(ModelSchema, Consumer, Consumer, Consumer, Action)} being
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
