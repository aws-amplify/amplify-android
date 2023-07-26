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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.events.ApiChannelEventName;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.personcar.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue")
@RunWith(RobolectricTestRunner.class)
@Ignore("Test class is unstable on CI - to be enabled after investigation")
public final class AWSDataStorePluginTest {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:datastore:test");
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    private static final String MOCK_API_PLUGIN_NAME = "MockApiPlugin";
    private Context context;
    private ModelProvider modelProvider;
    private AtomicInteger subscriptionStartedCounter;
    private AtomicInteger subscriptionCancelledCounter;
    private int modelCount;

    /**
     * Sets up the test. The {@link SimpleModelProvider} is spy'd, so that
     * we can check if the SyncProcessor queries it. If it does, that means
     * the SyncProcessor is running. Otherwise, either the SyncProcessor is *not*
     * running, or it is running but not functioning as we expect it to.
     */
    @Before
    public void setup() {
        this.context = getApplicationContext();
        modelProvider = spy(AmplifyCliGeneratedModelProvider.singletonInstance());
        subscriptionCancelledCounter = new AtomicInteger();
        subscriptionStartedCounter = new AtomicInteger();
        modelCount = modelProvider.modelNames().size();
    }

    /**
     * Configuring and initializing the plugin succeeds without freezing or crashing the calling thread. Basic. 🙄
     * @throws AmplifyException On failure to configure or initialize plugin.
     */
    @Test
    public void configureAndInitialize() throws AmplifyException {
        //Configure DataStore with an empty config (All defaults)
        ApiCategory emptyApiCategory = spy(ApiCategory.class);
        AWSDataStorePlugin standAloneDataStorePlugin = AWSDataStorePlugin.builder()
                                                                         .modelProvider(modelProvider)
                                                                         .apiCategory(emptyApiCategory)
                                                                         .build();
        standAloneDataStorePlugin.configure(new JSONObject(), context);
        standAloneDataStorePlugin.initialize(context);
    }

    /**
     * Starting the plugin in local mode (no API plugin) works without freezing or crashing the calling thread.
     * @throws AmplifyException Not expected; on failure to configure of initialize plugin.
     */
    @Test
    public void startInLocalMode() throws AmplifyException {
        // Configure DataStore with an empty config (All defaults)
        HubAccumulator dataStoreReadyObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.READY, 1)
                .start();
        ApiCategory emptyApiCategory = spy(ApiCategory.class);
        AWSDataStorePlugin standAloneDataStorePlugin = AWSDataStorePlugin.builder()
                                                                         .modelProvider(modelProvider)
                                                                         .apiCategory(emptyApiCategory)
                                                                         .build();
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(standAloneDataStorePlugin);
        standAloneDataStorePlugin.configure(new JSONObject(), context);
        standAloneDataStorePlugin.initialize(context);
        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        synchronousDataStore.start();

        dataStoreReadyObserver.await();
        assertSyncProcessorNotStarted(emptyApiCategory);

        Person person1 = createPerson("Test", "Dummy I");
        synchronousDataStore.save(person1);
        assertNotNull(person1.getId());
        Person person1FromDb = synchronousDataStore.get(Person.class, person1.getPrimaryKeyString());
        assertEquals(person1, person1FromDb);
    }

    /**
     * Starting the plugin when in API sync mode succeeds without freezing or crashing the calling thread.
     * @throws JSONException on failure to arrange plugin config
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void startInApiMode() throws JSONException, AmplifyException {
        HubAccumulator dataStoreReadyObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.READY, 1)
                .start();
        HubAccumulator subscriptionsEstablishedObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.SUBSCRIPTIONS_ESTABLISHED, 1)
                .start();
        HubAccumulator networkStatusObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.NETWORK_STATUS, 1)
                .start();
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(mockApiCategory)
                                                                  .build();
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        synchronousDataStore.start();

        dataStoreReadyObserver.await();
        subscriptionsEstablishedObserver.await();

        assertRemoteSubscriptionsStarted();
    }

    /**
     * Verify that when the clear method is called, the following happens
     * - All remote synchronization processes are stopped
     * - The database is deleted.
     * - On the next interaction with the DataStore, the synchronization processes are restarted.
     * @throws JSONException on failure to arrange plugin config
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void clearStopsSyncAndDeletesDatabase() throws AmplifyException, JSONException {
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        ApiPlugin<?> mockApiPlugin = mockApiCategory.getPlugin(MOCK_API_PLUGIN_NAME);
        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(mockApiCategory)
                                                                  .build();
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        // Setup objects
        Person person1 = createPerson("Test", "Dummy I");
        Person person2 = createPerson("Test", "Dummy II");

        // Mock responses for person 1
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Person>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(person1.getId(), false, 1, Temporal.Timestamp.now());
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person1, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            return mock(GraphQLOperation.class);
        }).when(mockApiPlugin).mutate(any(), any(), any());

        HubAccumulator apiInteractionObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, 1)
                .start();

        // Save person 1
        synchronousDataStore.save(person1);
        Person result1 = synchronousDataStore.get(Person.class, person1.getPrimaryKeyString());
        assertEquals(person1, result1);

        apiInteractionObserver.await(15, TimeUnit.SECONDS);
        verify(mockApiCategory).mutate(argThat(getMatcherFor(person1)), any(), any());

        // Mock responses for person 2
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Person>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(person2.getId(), false, 1,
                    Temporal.Timestamp.now());
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person2, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            return mock(GraphQLOperation.class);
        }).when(mockApiPlugin).mutate(any(), any(), any());

        // Do the thing!
        synchronousDataStore.clear();

        assertRemoteSubscriptionsCancelled();

        apiInteractionObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, 1)
                .start();
        HubAccumulator orchestratorInitObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.READY, 1)
                .start();

        // Interact with the DataStore after the clear
        synchronousDataStore.save(person2);

        // Verify person 2 was published to the cloud
        apiInteractionObserver.await();

        // Verify the orchestrator started back up and subscriptions are active.
        orchestratorInitObserver.await();
        assertRemoteSubscriptionsStarted();

        Person result2 = synchronousDataStore.get(Person.class, person2.getPrimaryKeyString());
        assertEquals(person2, result2);

        verify(mockApiCategory, atLeastOnce())
            .mutate(argThat(getMatcherFor(person2)), any(), any());
    }

    /**
     * Verify that when the stop method is called, the following happens
     * - All remote synchronization processes are stopped
     * - On the next interaction with the DataStore, the synchronization processes are restarted.
     * @throws JSONException on failure to arrange plugin config
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void stopStopsSyncUntilNextInteraction() throws AmplifyException, JSONException {
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        ApiPlugin<?> mockApiPlugin = mockApiCategory.getPlugin(MOCK_API_PLUGIN_NAME);
        JSONObject dataStorePluginJson = new JSONObject()
                .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(mockApiCategory)
                                                                  .build();
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        // Setup objects
        Person person1 = createPerson("Test", "Dummy I");
        Person person2 = createPerson("Test", "Dummy II");

        // Mock responses for person 1
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Person>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(person1.getPrimaryKeyString(), false, 1,
                    Temporal.Timestamp.now());
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person1, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            return mock(GraphQLOperation.class);
        }).when(mockApiPlugin).mutate(any(), any(), any());

        HubAccumulator apiInteractionObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, 1)
                .start();

        // Save person 1
        synchronousDataStore.save(person1);
        Person result1 = synchronousDataStore.get(Person.class, person1.getPrimaryKeyString());
        assertEquals(person1, result1);

        apiInteractionObserver.await();
        verify(mockApiCategory).mutate(argThat(getMatcherFor(person1)), any(), any());

        // Mock responses for person 2
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Person>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(person2.getPrimaryKeyString(), false, 1,
                    Temporal.Timestamp.now());
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person2, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            return mock(GraphQLOperation.class);
        }).when(mockApiPlugin).mutate(any(), any(), any());

        // Do the thing!
        synchronousDataStore.stop();

        assertRemoteSubscriptionsCancelled();

        apiInteractionObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, 1)
                .start();
        HubAccumulator orchestratorInitObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.READY, 1)
                .start();

        // Interact with the DataStore after the stop
        synchronousDataStore.save(person2);

        // Verify person 2 was published to the cloud
        apiInteractionObserver.await();

        // Verify the orchestrator started back up and subscriptions are active.
        orchestratorInitObserver.await();
        assertRemoteSubscriptionsStarted();

        // Verify person 1 and 2 are in the DataStore
        List<Person> results = synchronousDataStore.list(Person.class);
        assertEquals(Arrays.asList(person1, person2), results);

        verify(mockApiCategory, atLeastOnce())
                .mutate(argThat(getMatcherFor(person2)), any(), any());
    }

    private void assertRemoteSubscriptionsCancelled() {
        // Check that we've had active subscriptions
        assertTrue(subscriptionStartedCounter.get() > 0);
        // And the number of started and cancelled are the same
        assertEquals(subscriptionStartedCounter.get(), subscriptionCancelledCounter.get());
    }

    private void assertRemoteSubscriptionsStarted() {
        // For each model, there should be 3 subscriptions setup.
        // If subscriptions are active, the active counters should be:
        // activeCount - cancelledCount = modelCount * 3
        // The difference between active and cancelled should always be at most modelCount * 3
        final int diffTypesOfSubscriptions = SubscriptionType.values().length;
        assertEquals(
            modelCount * diffTypesOfSubscriptions,
            subscriptionStartedCounter.get() - subscriptionCancelledCounter.get()
        );
    }

    /**
     * Check that the SyncProcessor did not start by asserting that the
     * only interaction with the API category was triggered by the getPlugins
     * method.
     * @param mockApi Mock or spy of the ApiCategory being used for the test.
     */
    private void assertSyncProcessorNotStarted(ApiCategory mockApi) {
        Long callsToApiCategory = Observable.fromIterable(mockingDetails(mockApi).getInvocations())
            .filter(invocation -> !"getPlugins".equals(invocation.getMethod().getName()))
            .count()
            .blockingGet();
        assertEquals(Long.valueOf(0), callsToApiCategory);
    }

    @SuppressWarnings("unchecked")
    private ApiCategory mockApiCategoryWithGraphQlApi() throws AmplifyException {
        ApiCategory mockApiCategory = spy(ApiCategory.class);
        ApiPlugin<?> mockApiPlugin = mock(ApiPlugin.class);
        when(mockApiPlugin.getPluginKey()).thenReturn(MOCK_API_PLUGIN_NAME);
        when(mockApiPlugin.getCategoryType()).thenReturn(CategoryType.API);

        ApiEndpointStatusChangeEvent eventData =
            new ApiEndpointStatusChangeEvent(ApiEndpointStatusChangeEvent.ApiEndpointStatus.REACHABLE,
                                               ApiEndpointStatusChangeEvent.ApiEndpointStatus.UNKOWN);
        HubEvent<ApiEndpointStatusChangeEvent> hubEvent =
            HubEvent.create(ApiChannelEventName.API_ENDPOINT_STATUS_CHANGED, eventData);

        // Make believe that queries return response immediately
        doAnswer(invocation -> {
            // Mock the API emitting an ApiEndpointStatusChangeEvent event.
            Amplify.Hub.publish(HubChannel.API, hubEvent);
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Person>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Person>> data = new PaginatedResult<>(Collections.emptyList(), null);
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that subscriptions return response immediately
        doAnswer(invocation -> {
            int indexOfStartConsumer = 1;
            Consumer<String> onStart = invocation.getArgument(indexOfStartConsumer);
            GraphQLOperation<?> mockOperation = mock(GraphQLOperation.class);
            doAnswer(opAnswer -> {
                this.subscriptionCancelledCounter.incrementAndGet();
                return null;
            }).when(mockOperation).cancel();

            this.subscriptionStartedCounter.incrementAndGet();
            // Trigger the subscription start event.
            onStart.accept(RandomString.string());
            return mockOperation;
        }).when(mockApiPlugin).subscribe(
            any(GraphQLRequest.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Action.class)
        );
        mockApiCategory.addPlugin(mockApiPlugin);
        mockApiCategory.configure(new ApiCategoryConfiguration(), getApplicationContext());
        mockApiCategory.initialize(getApplicationContext());
        return mockApiCategory;
    }

    /**
     * Almost the same as mockApiCategoryWithGraphQlApi, but it calls the onError callback instead.
     *
     * @return A mock version of the API Category.
     * @throws AmplifyException Throw if an error happens when adding the plugin.
     */
    @SuppressWarnings("unchecked")
    private static ApiCategory mockApiPluginWithExceptions() throws AmplifyException {
        ApiCategory mockApiCategory = spy(ApiCategory.class);
        ApiPlugin<?> mockApiPlugin = mock(ApiPlugin.class);
        when(mockApiPlugin.getPluginKey()).thenReturn(MOCK_API_PLUGIN_NAME);
        when(mockApiPlugin.getCategoryType()).thenReturn(CategoryType.API);

        doAnswer(invocation -> {
            int indexOfErrorConsumer = 2;
            Consumer<ApiException> onError = invocation.getArgument(indexOfErrorConsumer);
            onError.accept(new ApiException("Fake exception thrown from the API.query method", "Just retry"));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        doAnswer(invocation -> {
            int indexOfErrorConsumer = 2;
            Consumer<ApiException> onError = invocation.getArgument(indexOfErrorConsumer);
            onError.accept(new ApiException("Fake exception thrown from the API.mutate method", "Just retry"));
            return null;
        }).when(mockApiPlugin).mutate(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        doAnswer(invocation -> {
            int indexOfErrorConsumer = 3;
            Consumer<ApiException> onError = invocation.getArgument(indexOfErrorConsumer);
            ApiException apiException =
                new ApiException("Fake exception thrown from the API.subscribe method", "Just retry");
            onError.accept(apiException);
            return null;
        }).when(mockApiPlugin).subscribe(
            any(GraphQLRequest.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Action.class)
        );
        mockApiCategory.addPlugin(mockApiPlugin);
        return mockApiCategory;
    }

    /**
     * Verify that the observe api returns itemChanged which matches the predicate.
     *
     * @throws JSONException        on failure to arrange plugin config.
     * @throws AmplifyException     on failure to arrange API plugin via Amplify facade.
     * @throws InterruptedException If interrupted while test observer awaits terminal result.
     */
    @Test
    public void observeWithMatchingPredicate() throws InterruptedException, AmplifyException, JSONException {
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                .modelProvider(modelProvider)
                .build();
        JSONObject dataStorePluginJson = new JSONObject()
                .put("syncIntervalInMinutes", 60);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        final CountDownLatch latch = new CountDownLatch(1);
        Person expectedResult = createPerson("Test", "Dummy I");
        final Person[] actualResult = {null};
        Consumer<DataStoreItemChange<Person>> onObserveResult = spy(new Consumer<DataStoreItemChange<Person>>() {
            @Override
            public void accept(@NonNull DataStoreItemChange<Person> value) {
                latch.countDown();
                actualResult[0] = value.item();
            }
        });
        awsDataStorePlugin.observe(Person.class,
            Person.FIRST_NAME.eq("Test"),
            value -> { },
            onObserveResult,
            error -> {
                LOG.error("Error: " + error);
            },
            () -> { }
        );
        synchronousDataStore.save(expectedResult);
        latch.await(TIMEOUT_MS, TimeUnit.SECONDS);
        verify(onObserveResult).accept(any());
        assertEquals(actualResult[0], expectedResult);
    }

    /**
     * Verify that the observe api is not invoke when the item changed does not match the predicate.
     *
     * @throws JSONException        on failure to arrange plugin config.
     * @throws AmplifyException     on failure to arrange API plugin via Amplify facade.
     * @throws InterruptedException If interrupted while test observer awaits terminal result.
     */
    @Test
    public void observeWithoutMatchingPredicate() throws InterruptedException, AmplifyException, JSONException {
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                .modelProvider(modelProvider)
                .build();
        JSONObject dataStorePluginJson = new JSONObject()
                .put("syncIntervalInMinutes", 60);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        Person expectedResult = createPerson("Test", "Dummy I");
        final Person[] actualResult = {null};
        Consumer<DataStoreItemChange<Person>> onObserveResult = spy(new Consumer<DataStoreItemChange<Person>>() {
            @Override
            public void accept(@NonNull DataStoreItemChange<Person> value) {
                actualResult[0] = value.item();
            }
        });
        awsDataStorePlugin.observe(Person.class,
            Person.FIRST_NAME.eq("NO MATCH"),
            value -> { },
            onObserveResult,
            error -> {
                LOG.error("Error: " + error);
            },
            () -> { }
        );
        synchronousDataStore.save(expectedResult);
        Thread.sleep(1000L);
        verify(onObserveResult, never()).accept(any());
    }

    private Person createPerson(String firstName, String lastName) {
        return Person.builder()
            .firstName(firstName)
            .lastName(lastName)
            .age(41)
            .build();
    }

    private static ArgumentMatcher<GraphQLRequest<ModelWithMetadata<Person>>> getMatcherFor(Person person) {
        return graphQLRequest -> {
            try {
                JSONObject payload = new JSONObject(graphQLRequest.getContent());
                String modelIdInRequest = payload.getJSONObject("variables").getJSONObject("input").getString("id");
                return person.getId().equals(modelIdInRequest);
            } catch (JSONException exception) {
                fail("Invalid GraphQLRequest payload." + exception.getMessage());
            }
            return false;
        };
    }
}
