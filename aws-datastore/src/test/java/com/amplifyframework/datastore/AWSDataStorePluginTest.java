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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousDataStore;
import com.amplifyframework.util.Time;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.publicationOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginTest {
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
        ShadowLog.stream = System.out;
        this.context = getApplicationContext();
        modelProvider = spy(SimpleModelProvider.withRandomVersion(BlogOwner.class));
        subscriptionCancelledCounter = new AtomicInteger();
        subscriptionStartedCounter = new AtomicInteger();
        modelCount = modelProvider.models().size();
    }

    /**
     * Configuring and initializing the plugin succeeds without freezing or
     * crashing the calling thread. Basic. 🙄
     * @throws AmplifyException Not expected; on failure to configure of initialize plugin.
     */
    @Test
    public void configureAndInitializeInLocalMode() throws AmplifyException {
        //Configure DataStore with an empty config (All defaults)
        ApiCategory emptyApiCategory = spy(ApiCategory.class);
        AWSDataStorePlugin standAloneDataStorePlugin = new AWSDataStorePlugin(modelProvider, emptyApiCategory);
        standAloneDataStorePlugin.configure(new JSONObject(), context);
        standAloneDataStorePlugin.initialize(context);
        assertSyncProcessorNotStarted();
    }

    /**
     * Configuring and initialization the plugin when in API sync mode succeeds without
     * freezing or crashing the the calling thread.
     * @throws JSONException on failure to arrange plugin config
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void configureAndInitializeInApiMode() throws JSONException, AmplifyException {
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi(mock(ApiPlugin.class));
        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = new AWSDataStorePlugin(modelProvider, mockApiCategory);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        assertRemoteSubscriptionsStarted();
    }

    /**
     * Simulate a situation where the user has added the API plugin, but it's
     * either not pushed or exceptions occur while trying to start up the sync processes.
     * The outcome is that the local store should still be available and the
     * host app should not crash.
     * @throws JSONException If an exception occurs while building the JSON configuration.
     * @throws AmplifyException If an exception occurs setting up the mock API
     */
    @Test
    public void configureAndInitializeInApiModeWithoutApi() throws JSONException, AmplifyException {
        ApiCategory mockApiCategory = mockApiPluginWithExceptions();
        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = new AWSDataStorePlugin(modelProvider, mockApiCategory);
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        BlogOwner blogOwner1 = createBlogOwner("Test", "Dummy I");
        synchronousDataStore.save(blogOwner1);
        assertNotNull(blogOwner1.getId());
        BlogOwner blogOwner1FromDb = synchronousDataStore.get(BlogOwner.class, blogOwner1.getId());
        assertEquals(blogOwner1, blogOwner1FromDb);
    }

    /**
     * Verify that when the clear method is called, the following happens
     * - All remote synchronization processes are stopped
     * - The database is deleted.
     * - On the next interaction with the DataStore, the synchronization processes are restarted.
     * @throws JSONException on failure to arrange plugin config
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @SuppressWarnings("unchecked")
    @Test
    public void clearStopsSyncUntilNextInteraction() throws AmplifyException, JSONException {
        ApiPlugin<?> mockApiPlugin = mock(ApiPlugin.class);
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi(mockApiPlugin);

        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = new AWSDataStorePlugin(modelProvider, mockApiCategory);
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        assertRemoteSubscriptionsStarted();

        BlogOwner blogOwner1 = createBlogOwner("Test", "Dummy I");
        BlogOwner blogOwner2 = createBlogOwner("Test", "Dummy II");

        when(mockApiPlugin.mutate(any(), any(), any())).thenAnswer(invocation -> {
            String data = new JSONObject()
                .put("id", blogOwner1.getId())
                .put("name", blogOwner1.getName())
                .put("_deleted", false)
                .put("_version", 1)
                .put("_lastSyncedAt", Time.now())
                .toString();
            GraphQLResponse<String> response = new GraphQLResponse<>(data, Collections.emptyList());
            ((Consumer<GraphQLResponse<String>>) invocation.getArgument(1)).accept(response);
            return /* void */ null;
        });
        HubAccumulator blogOwner1Accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(blogOwner1), 1).start();
        synchronousDataStore.save(blogOwner1);
        BlogOwner result1 = synchronousDataStore.get(BlogOwner.class, blogOwner1.getId());
        assertEquals(blogOwner1, result1);
        blogOwner1Accumulator.await();

        synchronousDataStore.clear();
        assertRemoteSubscriptionsCancelled();

        when(mockApiPlugin.mutate(any(), any(), any())).thenAnswer(invocation -> {
            String data = new JSONObject()
                .put("id", blogOwner2.getId())
                .put("name", blogOwner2.getName())
                .put("_deleted", false)
                .put("_version", 1)
                .put("_lastSyncedAt", Time.now())
                .toString();
            GraphQLResponse<String> response = new GraphQLResponse<>(data, Collections.emptyList());
            ((Consumer<GraphQLResponse<String>>) invocation.getArgument(1)).accept(response);
            return /* void */ null;
        });
        HubAccumulator blogOwner2Accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(blogOwner2), 1).start();
        synchronousDataStore.save(blogOwner2);
        BlogOwner result2 = synchronousDataStore.get(BlogOwner.class, blogOwner2.getId());
        assertEquals(blogOwner2, result2);
        blogOwner2Accumulator.await();

        verify(mockApiCategory, times(2)).mutate(Mockito.any(), Mockito.any(), Mockito.any());
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
     * Check that there were no interactions between the SyncProcessor
     * and the model provider. This is used to verify that the synchronization
     * processes don't start if there's no API configured.
     */
    private void assertSyncProcessorNotStarted() {
        boolean syncProcessorNotInvoked = mockingDetails(modelProvider)
            .getInvocations()
            .stream()
            .noneMatch(invocation -> invocation.getLocation().getSourceFile().contains("SyncProcessor"));
        assertTrue(syncProcessorNotInvoked);
    }

    @SuppressWarnings("unchecked")
    private ApiCategory mockApiCategoryWithGraphQlApi(ApiPlugin<?> mockApiPlugin) throws AmplifyException {
        ApiCategory mockApiCategory = spy(ApiCategory.class);
        when(mockApiPlugin.getPluginKey()).thenReturn(MOCK_API_PLUGIN_NAME);
        when(mockApiPlugin.getCategoryType()).thenReturn(CategoryType.API);

        // Make believe that queries return response immediately
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<Iterable<String>>> onResponse = invocation.getArgument(indexOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>(Collections.emptyList(), Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that mutations return response immediately
        doAnswer(invocation -> null)
            .when(mockApiPlugin)
            .mutate(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

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
        return mockApiCategory;
    }

    /**
     * Almost the same as mockApiCategoryWithGraphQlApi, but it calls the onError callback instead.
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

    @SuppressWarnings("SameParameterValue")
    private BlogOwner createBlogOwner(String firstName, String lastName) {
        return BlogOwner.builder()
            .name(String.format(Locale.US, "%s, %s", firstName, lastName))
            .build();
    }
}
