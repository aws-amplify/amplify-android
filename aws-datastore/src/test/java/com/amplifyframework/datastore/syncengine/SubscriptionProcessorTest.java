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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.EmptyAction;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
    private ModelSchemaRegistry modelSchemaRegistry;

    /**
     * Sets up an {@link SubscriptionProcessor} and associated test dependencies.
     * @throws AmplifyException On failure to load model schema registry
     */
    @Before
    public void setup() throws AmplifyException {
        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.register(modelProvider.modelSchemas());
        this.modelSchemas = sortedModels(modelProvider);
        this.appSync = mock(AppSync.class);
        this.merger = mock(Merger.class);
        this.subscriptionProcessor = new SubscriptionProcessor(appSync, modelProvider, merger);
    }

    private List<ModelSchema> sortedModels(ModelProvider modelProvider) throws AmplifyException {
        TopologicalOrdering topologicalOrdering =
            TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        Comparator<ModelSchema> comparator = (one, two) -> {
            return topologicalOrdering.compare(one, two);
        };
        List<ModelSchema> modelSchemas = new ArrayList<>(modelProvider.modelSchemas().values());
        Collections.sort(modelSchemas, comparator);
        return modelSchemas;
    }

    /**
     * When {@link SubscriptionProcessor#startSubscriptions()} is invoked,
     * the {@link AppSync} client receives subscription requests.
     */
    @Test
    public void appSyncInvokedWhenSubscriptionsStarted() {
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
     * When {@link SubscriptionProcessor#startDrainingMutationBuffer(Action)} is called, then the
     * {@link Merger} is invoked to begin merging whatever content has shown up on the subscriptions.
     * @throws DataStoreException On failure to arrange mocking
     * @throws InterruptedException On failure to await latch
     */
    @Test
    public void dataMergedWhenBufferDrained() throws DataStoreException, InterruptedException {
        // By default, start the subscriptions up.
        arrangeStartedSubscriptions(appSync, modelSchemas, SubscriptionType.values());

        // Arrange some subscription data
        BlogOwner model = BlogOwner.builder()
            .name("John P. Stetson, Jr.")
            .build();
        ModelMetadata modelMetadata = new ModelMetadata(model.getId(), false, 1, Temporal.Timestamp.now());
        ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(model, modelMetadata);
        GraphQLResponse<ModelWithMetadata<BlogOwner>> response = new GraphQLResponse<>(modelWithMetadata, null);
        arrangeDataEmittingSubscription(appSync,
                modelSchemaRegistry.getModelSchemaForModelInstance(model),
                SubscriptionType.ON_CREATE,
                response);

        // Merge will be invoked for the subcription data, when we start draining...
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return Completable.complete();
        }).when(merger).merge(eq(modelWithMetadata));

        // Start draining....
        subscriptionProcessor.startSubscriptions();
        subscriptionProcessor.startDrainingMutationBuffer(EmptyAction.create());

        // Was the data merged?
        assertTrue(latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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

    private static <T extends Model> void arrangeSubscription(
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
