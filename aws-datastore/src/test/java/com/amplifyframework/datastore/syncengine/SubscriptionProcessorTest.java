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

package com.amplifyframework.datastore.syncengine;

import android.util.Pair;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link SubscriptionProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class SubscriptionProcessorTest {
    private static final long OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    private List<ModelSchema> modelSchemas;
    private AppSync appSync;
    private Merger merger;
    private SubscriptionProcessor subscriptionProcessor;
    private SchemaRegistry schemaRegistry;
    private Consumer<Throwable> onFailure;

    /**
     * Sets up an {@link SubscriptionProcessor} and associated test dependencies.
     * @throws DataStoreException on error building the {@link DataStoreConfiguration}
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws DataStoreException {
        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(modelProvider.modelSchemas());
        this.modelSchemas = sortedModels(modelProvider);
        this.appSync = mock(AppSync.class);
        this.merger = mock(Merger.class);
        this.onFailure = (Consumer<Throwable>) mock(Consumer.class);
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.builder()
                .syncExpression(BlogOwner.class, () -> BlogOwner.NAME.beginsWith("John"))
                .build();
        QueryPredicateProvider queryPredicateProvider = new QueryPredicateProvider(() -> dataStoreConfiguration);
        queryPredicateProvider.resolvePredicates();
        this.subscriptionProcessor = SubscriptionProcessor.builder()
                .appSync(appSync)
                .modelProvider(modelProvider)
                .schemaRegistry(schemaRegistry)
                .merger(merger)
                .queryPredicateProvider(queryPredicateProvider)
                .onFailure(onFailure)
                .build();
    }

    private List<ModelSchema> sortedModels(ModelProvider modelProvider) {
        TopologicalOrdering topologicalOrdering =
            TopologicalOrdering.forRegisteredModels(schemaRegistry, modelProvider);
        List<ModelSchema> modelSchemas = new ArrayList<>(modelProvider.modelSchemas().values());
        Collections.sort(modelSchemas, topologicalOrdering::compare);
        return modelSchemas;
    }

    /**
     * When {@link SubscriptionProcessor#startSubscriptions()} is invoked,
     * the {@link AppSync} client receives subscription requests.
     * @throws DataStoreException If test is broken
     */
    @Test
    public void appSyncInvokedWhenSubscriptionsStarted() throws DataStoreException {
        // For every Class-SubscriptionType pairing, use a CountDownLatch
        // to tell whether or not we've "seen" a subscription event for it.
        Map<Pair<ModelSchema, SubscriptionType>, CountDownLatch> seen = new HashMap<>();
        // Build a stream of such pairs.
        Observable.fromIterable(modelSchemas)
            .flatMap(modelSchema -> Observable.fromArray(SubscriptionType.values())
                .map(value -> Pair.create(modelSchema, value)))
            .blockingForEach(pair -> {
                // For each one, store a latch. Add a mocking behavior to count down
                // the latch when the subscription API is hit, for that class and subscription type.
                CountDownLatch latch = new CountDownLatch(1);
                seen.put(Pair.create(pair.first, pair.second), latch);
                Answer<Cancelable> answer = invocation -> {
                    latch.countDown();
                    final int startConsumerIndex = 1;
                    Consumer<String> onStart = invocation.getArgument(startConsumerIndex);
                    onStart.accept(RandomString.string());
                    return new NoOpCancelable();
                };
                arrangeSubscription(appSync, answer, pair.first, pair.second);
            });

        // Act: start some subscriptions.
        subscriptionProcessor.startSubscriptions();

        // Make sure that all of the subscriptions have been
        Observable.fromIterable(seen.entrySet())
            .blockingForEach(entry -> {
                CountDownLatch latch = entry.getValue();
                assertTrue(latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            });
    }

    /**
     * When {@link SubscriptionProcessor#startDrainingMutationBuffer()} is called, then the
     * {@link Merger} is invoked to begin merging whatever content has shown up on the subscriptions.
     * @throws DataStoreException On failure to arrange mocking
     * @throws InterruptedException On failure to await latch
     */
    @Test
    public void dataMergedWhenBufferDrained() throws DataStoreException, InterruptedException {
        assertTrue(isDataMergedWhenBufferDrainedForBlogOwnerNamed("John P. Stetson, Jr."));
    }

    /**
     * Verify that data not matching the configured sync query predicate does NOT get merged.  Since
     * DataStoreConfiguration built in {@link #setup()} has a syncExpression specifying that the BlogOwner name
     * must start with "John", this test verifies that a BlogOwner named "Paul Hudson" does NOT get merged.
     * @throws DataStoreException On failure to arrange mocking
     * @throws InterruptedException On failure to await latch
     */
    @Test
    public void dataIsFilteredIfSyncExpressionExists() throws DataStoreException, InterruptedException {
        assertFalse(isDataMergedWhenBufferDrainedForBlogOwnerNamed("Paul Hudson"));
    }

    /**
     * Verifies that an exception caused by an ApiAuthException will NOT call onFailure. This is because models may
     * fail to subscribe due to authorization failure, but we want to continue to subscribe the other models.
     * @throws DataStoreException On failure to arrange mocking
     */
    @Test
    public void apiAuthExceptionIsIgnored() throws DataStoreException {
        Answer<Cancelable> answer = invocation -> {
            final int modelSchemaIndex = 0;
            final int startConsumerIndex = 1;
            final int errorConsumerIndex = 3;
            ModelSchema modelSchema = invocation.getArgument(modelSchemaIndex);
            Consumer<String> onStart = invocation.getArgument(startConsumerIndex);
            Consumer<DataStoreException> onError = invocation.getArgument(errorConsumerIndex);
            onStart.accept(RandomString.string());
            if (modelSchema.equals(modelSchemas.get(1))) {
                onError.accept(
                    new DataStoreException(
                        "Failure for model",
                        new ApiException.ApiAuthException("Simulated auth failure", "This is intentional"),
                        "This is intentional"
                    )
                );
            }
            return new NoOpCancelable();
        };
        arrangeSubscriptions(appSync, answer, modelSchemas, SubscriptionType.values());

        subscriptionProcessor.startSubscriptions();

        verify(onFailure, never()).accept(any());
    }

    /**
     * Return whether a response with a BlogOwner with the given name gets merged with the merger.
     * @param name name of the BlogOwner returned in the subscription
     * @return whether the data was merged
     * @throws DataStoreException On failure to arrange mocking
     * @throws InterruptedException On failure to await latch
     */
    private boolean isDataMergedWhenBufferDrainedForBlogOwnerNamed(String name)
            throws DataStoreException, InterruptedException {
        // By default, start the subscriptions up.
        arrangeStartedSubscriptions(appSync, modelSchemas, SubscriptionType.values());

        // Arrange some subscription data
        BlogOwner model = BlogOwner.builder()
            .name(name)
            .build();
        ModelMetadata modelMetadata = new ModelMetadata(model.getPrimaryKeyString(), false, 1,
                Temporal.Timestamp.now());
        ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(model, modelMetadata);
        GraphQLResponse<ModelWithMetadata<BlogOwner>> response = new GraphQLResponse<>(modelWithMetadata, null);
        arrangeDataEmittingSubscription(appSync,
                schemaRegistry.getModelSchemaForModelInstance(model),
                SubscriptionType.ON_CREATE,
                response);

        // Merge will be invoked for the subcription data, when we start draining...
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return Completable.complete();
        }).when(merger).merge(eq(response.getData()));

        // Start draining....
        subscriptionProcessor.startSubscriptions();
        subscriptionProcessor.startDrainingMutationBuffer();

        // Was the data merged?
        return latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Model> void arrangeDataEmittingSubscription(
            AppSync appSync,
            ModelSchema modelSchema,
            SubscriptionType subscriptionType,
            GraphQLResponse<ModelWithMetadata<T>> response) throws DataStoreException {
        Answer<Cancelable> answer = invocation -> {
            final int startConsumerIndex = 1;
            Consumer<String> onStart = invocation.getArgument(startConsumerIndex);
            onStart.accept(RandomString.string());

            final int dataConsumerIndex = 2;
            Consumer<GraphQLResponse<ModelWithMetadata<T>>> onData = invocation.getArgument(dataConsumerIndex);
            onData.accept(response);

            return new NoOpCancelable();
        };
        arrangeSubscription(appSync, answer, modelSchema, subscriptionType);
    }

    private static void arrangeStartedSubscriptions(
        AppSync appSync, List<ModelSchema> modelSchemas, SubscriptionType[] subscriptionTypes) {
        Answer<Cancelable> answer = invocation -> {
            final int startConsumerIndex = 1;
            Consumer<String> onStart = invocation.getArgument(startConsumerIndex);
            onStart.accept(RandomString.string());
            return new NoOpCancelable();
        };
        arrangeSubscriptions(appSync, answer, modelSchemas, subscriptionTypes);
    }

    private static void arrangeSubscriptions(
            AppSync appSync,
            Answer<Cancelable> answer,
            List<ModelSchema> modelSchemas,
            SubscriptionType[] subscriptionTypes) {
        Observable.fromIterable(modelSchemas)
            .flatMap(modelSchema -> Observable.fromArray(subscriptionTypes)
                .map(subscriptionType -> Pair.create(modelSchema, subscriptionType)))
            .blockingForEach(pair -> arrangeSubscription(appSync, answer, pair.first, pair.second));
    }

    private static void arrangeSubscription(
            AppSync appSync, Answer<Cancelable> answer, ModelSchema modelSchema, SubscriptionType subscriptionType)
            throws DataStoreException {
        AppSync stub = doAnswer(answer).when(appSync);
        SubscriptionProcessor.SubscriptionMethod method =
            SubscriptionProcessor.subscriptionMethodFor(stub, subscriptionType);
        method.subscribe(eq(modelSchema), anyConsumer(), anyConsumer(), anyConsumer(), anyAction());
    }

    private static Action anyAction() {
        return any();
    }

    private static <T> Consumer<T> anyConsumer() {
        return any();
    }
}
