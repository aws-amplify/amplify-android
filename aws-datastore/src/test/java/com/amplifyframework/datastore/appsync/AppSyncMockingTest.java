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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.MatchAllQueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSyncMocking.SyncConfigurator;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.EmptyAction;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import static org.mockito.Mockito.mock;

/**
 * Tests the {@link AppSyncMocking} test utility.
 */
public final class AppSyncMockingTest {
    private static final long TIMEOUT_SECONDS = 2;

    private ModelSchema schema;
    private AppSync appSync;

    /**
     * Sets up the test.
     * @throws AmplifyException On failure to build ModelSchema
     */
    @Before
    public void setup() throws AmplifyException {
        this.schema = ModelSchema.fromModelClass(BlogOwner.class);
        this.appSync = mock(AppSync.class);
    }

    /**
     * When mockFailure() is called on the SyncConfigurator, the AppSync mock
     * will emit the provided failure.
     * @throws DataStoreException On failure to get a SyncConfigurator via sync()
     */
    @Test
    public void mockFailureForSync() throws DataStoreException {
        DataStoreException failure = new DataStoreException("Foo", "Bar");
        AppSyncMocking.sync(appSync).mockFailure(failure);

        GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> request =
            appSync.buildSyncRequest(schema, null, 100);
        Single
            .create(emitter -> appSync.sync(request, emitter::onSuccess, emitter::onError))
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertError(failure);
    }

    /**
     * The {@link SyncConfigurator#mockSuccessResponse(Class, ModelWithMetadata[])}
     * method allows you to configure a collection of {@link ModelWithMetadata} to be
     * included in a successful {@link GraphQLResponse}, emitted from the
     * {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)} operation.
     * @throws DataStoreException On failure to configure the mock behavior
     */
    @Test
    public void mockSuccessResponsesForSync() throws DataStoreException {
        // Act: configure the mock
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(BlogOwner.class, StrawMen.JOE, StrawMen.TONY);

        // Build a request object. This will itself test the mockSuccessResponse(),
        // since that method configures this call to return a meaningful result.
        GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> request =
            appSync.buildSyncRequest(schema, null, 100);

        // Lastly, when we actually call sync, we should see the expected response,
        // As a result of the mockSuccessResponse() on the AppSyncMocking.
        Single
            .<GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>>>create(emitter ->
                appSync.sync(request, emitter::onSuccess, emitter::onError)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertComplete()
            // TODO: response items change order. Why?
            .assertValue(response -> {
                if (response.hasErrors() || !response.hasData()) {
                    return false;
                }
                return Observable.fromIterable(response.getData().getItems())
                    .toList()
                    .map(HashSet::new)
                    .blockingGet()
                    .equals(new HashSet<>(Arrays.asList(StrawMen.JOE, StrawMen.TONY)));
            });
    }

    /**
     * When mockSuccessResponse() is called on the CreateConfigurator,
     * the AppSync mock will return a successful creation response
     * whenever AppSync's create() API is called.
     */
    @Test
    public void mockSuccessResponseForCreate() {
        AppSyncMocking.create(appSync)
            .mockSuccessResponse(StrawMen.JOE_MODEL, StrawMen.JOE);
        GraphQLResponse<ModelWithMetadata<BlogOwner>> expectedResponse =
            new GraphQLResponse<>(StrawMen.JOE, Collections.emptyList());
        Single
            .<GraphQLResponse<ModelWithMetadata<BlogOwner>>>create(emitter ->
                appSync.create(StrawMen.JOE_MODEL, schema, emitter::onSuccess, emitter::onError)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(expectedResponse);
    }

    /**
     * When mockErrorResponse() is called on the CreateConfigurator,
     * the AppSync mock will emit a response containing the provided GraphQL errors
     * whenever AppSync's create() API is called.
     */
    @Test
    public void mockErrorResponseForCreate() {
        GraphQLResponse.Error error = new GraphQLResponse.Error(
            "Uh oh!", Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );
        AppSyncMocking.create(appSync).mockErrorResponse(StrawMen.JOE_MODEL, error);
        Single
            .<GraphQLResponse<ModelWithMetadata<BlogOwner>>>create(emitter ->
                appSync.create(StrawMen.JOE_MODEL, schema, emitter::onSuccess, emitter::onError)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(new GraphQLResponse<>(null, Collections.singletonList(error)));
    }

    /**
     * When mockSuccessResponse() is called on the UpdateConfigurator,
     * the bound AppSync instance will reply with a successful response
     * whenever its update() API is invoked.
     */
    @Test
    public void mockSuccessResponseForUpdate() {
        ModelMetadata updatedMetadata =
            new ModelMetadata(StrawMen.TONY_MODEL.getId(), false, 2, StrawMen.JOE_METADATA.getLastChangedAt());
        ModelWithMetadata<BlogOwner> tonyWithUpdatedMetadata =
            new ModelWithMetadata<>(StrawMen.TONY_MODEL, updatedMetadata);
        AppSyncMocking.update(appSync)
            .mockSuccessResponse(StrawMen.TONY_MODEL, 1, tonyWithUpdatedMetadata);

        GraphQLResponse<ModelWithMetadata<BlogOwner>> expectedResponse =
            new GraphQLResponse<>(tonyWithUpdatedMetadata, Collections.emptyList());

        Single
            .<GraphQLResponse<ModelWithMetadata<BlogOwner>>>create(emitter ->
                appSync.update(StrawMen.TONY_MODEL, schema, 1, emitter::onSuccess, emitter::onError)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(expectedResponse);
    }

    /**
     * mockErrorResponse() on the UpdateConfigurator will prepare the
     * bound AppSync instance to return an error response whenever
     * its update() API is invoked.
     */
    @Test
    public void mockErrorResponseForUpdate() {
        GraphQLResponse.Error error = new GraphQLResponse.Error(
            "Uh oh!", Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );
        AppSyncMocking.update(appSync)
            .mockErrorResponse(StrawMen.JOE_MODEL, 1, error);
        Single
            .<GraphQLResponse<ModelWithMetadata<BlogOwner>>>create(emitter ->
                appSync.update(StrawMen.JOE_MODEL, schema, 1, emitter::onSuccess, emitter::onError)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(new GraphQLResponse<>(null, Collections.singletonList(error)));
    }

    /**
     * When mockSuccessResponse() is called on the DeleteConfigurator,
     * the bound AppSync instance will reply with a successful response
     * whenever its delete() API is invoked.
     */
    @Test
    public void mockSuccessResponseForDelete() {
        ModelMetadata deletedMetadata =
            new ModelMetadata(StrawMen.TONY_MODEL.getId(), true, 2, StrawMen.JOE_METADATA.getLastChangedAt());
        ModelWithMetadata<BlogOwner> tonyWithDeleteMetadata =
            new ModelWithMetadata<>(StrawMen.TONY_MODEL, deletedMetadata);
        AppSyncMocking.delete(appSync)
            .mockSuccessResponse(StrawMen.TONY_MODEL, 1, tonyWithDeleteMetadata);
        GraphQLResponse<ModelWithMetadata<BlogOwner>> expectedResponse =
            new GraphQLResponse<>(tonyWithDeleteMetadata, Collections.emptyList());
        Single
            .<GraphQLResponse<ModelWithMetadata<BlogOwner>>>create(emitter ->
                appSync.delete(
                    schema,
                    StrawMen.TONY_MODEL.getId(),
                    1,
                    MatchAllQueryPredicate.instance(),
                    emitter::onSuccess,
                    emitter::onError
                )
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(expectedResponse);
    }

    /**
     * When mockErrorResponse() is called on the DeleteConfigurator,
     * the bound AppSync instance will be configured to emit an error
     * response whenever its delete() API is invoked.
     */
    @Test
    public void mockErrorResponseForDelete() {
        GraphQLResponse.Error error = new GraphQLResponse.Error(
            "Uh oh!", Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );
        AppSyncMocking.delete(appSync)
            .mockErrorResponse(StrawMen.JOE_MODEL, 1, error);
        Single
            .create(emitter -> appSync.delete(
                schema,
                StrawMen.JOE_MODEL.getId(),
                1,
                MatchAllQueryPredicate.instance(),
                emitter::onSuccess,
                emitter::onError
            ))
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(new GraphQLResponse<>(null, Collections.singletonList(error)));
    }

    /**
     * callOnStart() arranges a mock behavior on the provided AppSync instance.
     * When the mock is called upon to create a subscription for creations events,
     * the mock will immediately callback on the provided onStart callback.
     */
    @Test
    public void onStartCallbackIsCalledForMockOnCreate() {
        AppSyncMocking.onCreate(appSync).callOnStart();
        Completable
            .create(subscriber -> appSync.onCreate(
                schema,
                subscriptionToken -> subscriber.onComplete(),
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                EmptyAction.create()
            ))
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertComplete();
    }

    /**
     * callOnStart() arranges a mock behavior on the provided AppSync instance.
     * When the mock is called upon to create a subscription for update events,
     * the mock will immediately callback on the provided onStart callback.
     */
    @Test
    public void onStartCallbackIsCalledForMockOnUpdate() {
        AppSyncMocking.onUpdate(appSync).callOnStart();
        Completable
            .create(subscriber -> appSync.onUpdate(
                schema,
                subscriptionToken -> subscriber.onComplete(),
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                EmptyAction.create()
            ))
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertComplete();
    }

    /**
     * callOnStart() arranges a mock behavior on the provided AppSync instance.
     * When the mock is called upon to create a subscription for delete events,
     * the mock will immediately callback on the provided onStart callback.
     */
    @Test
    public void onStartCallbackIsCalledForMockOnDelete() {
        AppSyncMocking.onDelete(appSync).callOnStart();
        Completable
            .create(subscriber -> appSync.onDelete(
                schema,
                subscriptionToken -> subscriber.onComplete(),
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                EmptyAction.create()
            ))
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertComplete();
    }

    static final class StrawMen {
        static final BlogOwner JOE_MODEL = BlogOwner.builder()
            .name("Joe")
            .build();
        static final ModelMetadata JOE_METADATA =
            new ModelMetadata(JOE_MODEL.getId(), false, 1, Temporal.Timestamp.now());
        static final ModelWithMetadata<BlogOwner> JOE =
            new ModelWithMetadata<>(JOE_MODEL, JOE_METADATA);

        static final BlogOwner TONY_MODEL = BlogOwner.builder()
            .name("Tony")
            .build();
        static final ModelMetadata TONY_METADATA =
            new ModelMetadata(TONY_MODEL.getId(), false, 1, Temporal.Timestamp.now());
        static final ModelWithMetadata<BlogOwner> TONY =
            new ModelWithMetadata<>(TONY_MODEL, TONY_METADATA);

        private StrawMen() {}
    }
}
