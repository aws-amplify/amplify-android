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
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginTest {
    private static final long OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private Context context;
    private ModelProvider modelProvider;

    /**
     * Sets up the test. The {@link SimpleModelProvider} is spy'd, so that
     * we can check if the SyncProcessor queries it. If it does, that means
     * the SyncProcessor is running. Otherwise, either the SyncProcessor is *not*
     * running, or it is running but not functioning as we expect it to.
     */
    @Before
    public void setup() {
        this.context = getApplicationContext();
        modelProvider = spy(SimpleModelProvider.builder()
            .version(RandomString.string())
            .addModel(Person.class)
            .build());
    }

    /**
     * Configuring and initializing the plugin succeeds without freezing or
     * crashing the calling thread. Basic. 🙄
     * @throws AmplifyException Not expected; on failure to configure of initialize plugin
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
     * @throws DataStoreException on failure to configure
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void configureAndInitializeInApiMode() throws JSONException, AmplifyException {
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        JSONObject dataStorePluginJson = new JSONObject()
            .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = new AWSDataStorePlugin(modelProvider, mockApiCategory);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        assertSyncProcessorStarted();
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
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        Person person1 = createPerson("Test", "Dummy I");
        Throwable exception = Completable.fromSingle(single -> { // Save a record to local store
            awsDataStorePlugin.save(person1, itemSaved -> {
                assertNotNull(itemSaved.item().getId());
                assertEquals(person1.getLastName(), itemSaved.item().getLastName());
                single.onSuccess(true);
            }, single::onError);
        }).andThen(
            Completable.fromSingle(single -> { // Verify the record has been saved
                awsDataStorePlugin.query(Person.class, results -> {
                    Person actualPerson = results.next();
                    assertNotNull(actualPerson);
                    assertFalse(results.hasNext()); // We should only have one result.
                    assertEquals(person1, actualPerson);
                    single.onSuccess(true);
                }, single::onError);
            })
        ).doOnError(error -> {
            fail(error.getMessage());
        }).blockingGet(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (exception != null) {
            throw new AmplifyException("Unexpected exception.", exception, "Look at the stacktrace.");
        }
    }

    private void assertSyncProcessorStarted() {
        boolean syncProcessorInvoked = mockingDetails(modelProvider)
            .getInvocations()
            .stream()
            .anyMatch(invocation -> invocation.getLocation().getSourceFile().contains("SyncProcessor"));

        assertTrue(syncProcessorInvoked);
    }

    private void assertSyncProcessorNotStarted() {
        boolean syncProcessorNotInvoked = mockingDetails(modelProvider)
            .getInvocations()
            .stream()
            .noneMatch(invocation -> invocation.getLocation().getSourceFile().contains("SyncProcessor"));

        assertTrue(syncProcessorNotInvoked);
    }

    @SuppressWarnings("unchecked")
    private static ApiCategory mockApiCategoryWithGraphQlApi() throws AmplifyException {
        ApiCategory mockApiCategory = spy(ApiCategory.class);
        ApiPlugin<?> mockApiPlugin = mock(ApiPlugin.class);
        when(mockApiPlugin.getPluginKey()).thenReturn("MockApiPlugin");

        // Make believe that queries return response immediately
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<Iterable<String>>> onResponse = invocation.getArgument(indexOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>(Collections.emptyList(), Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that mutations return response immediately
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<String>> onResponse = invocation.getArgument(indexOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>("{}", Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).mutate(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that subscriptions return response immediately
        doAnswer(invocation -> {
            int indexOfStartConsumer = 2;
            Consumer<String> onResponse = invocation.getArgument(indexOfStartConsumer);
            onResponse.accept(RandomString.string());
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
     * Almost the same as mockApiCategoryWithGraphQlApi, but it calls the onError callback instead.
     * @return A mock version of the API Category.
     * @throws AmplifyException Throw if an error happens when adding the plugin.
     */
    @SuppressWarnings("unchecked")
    private static ApiCategory mockApiPluginWithExceptions() throws AmplifyException {
        ApiCategory mockApiCategory = spy(ApiCategory.class);
        ApiPlugin<?> mockApiPlugin = mock(ApiPlugin.class);
        when(mockApiPlugin.getPluginKey()).thenReturn("MockApiPlugin");

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

    private Person createPerson(String firstName, String lastName) {
        return Person.builder()
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
}
