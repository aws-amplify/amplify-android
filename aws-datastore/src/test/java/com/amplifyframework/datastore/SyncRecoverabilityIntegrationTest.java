/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.personcar.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.personcar.Car;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public final class SyncRecoverabilityIntegrationTest {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final String MOCK_API_PLUGIN_NAME = "MockApiPlugin";
    private Context context;
    private ModelProvider modelProvider;

    /**
     * Wire up dependencies for the SyncProcessor, and build one for testing.
     * @throws AmplifyException On failure to load models into registry
     */
    @Before
    public void setup() throws AmplifyException {
        this.context = getApplicationContext();
        modelProvider = spy(AmplifyCliGeneratedModelProvider.singletonInstance());
        this.modelProvider = spy(AmplifyCliGeneratedModelProvider.singletonInstance());
    }

    /**
     * When irrecoverable sync error is returned for a schema, datastore stops sync on that model and moves on
     * to the next schema the datStore doesn't go offline and finish processing next mutation.
     * @throws AmplifyException On failure interacting with storage adapter
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws JSONException If unable to parse the JSON.
     */
    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    @Test
    public void syncRecoversAfterIrrecoverableError() throws AmplifyException, JSONException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        // Arrange
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        Person person1 = setupApiMockForIrrecoverableErrorOnSync(latch, mockApiCategory);

        SynchronousDataStore synchronousDataStore = getSynchronousDataStore(mockApiCategory);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        // Save person 1 and make sure appsync is called which tells that dataStore is in API sync mode.
        synchronousDataStore.save(person1);
        Person result1 = synchronousDataStore.get(Person.class, person1.getId());
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(person1, result1);
    }

    /**
     * When irrecoverable sync error is returned for a schema, datastore stops sync on that model and moves on
     * to the next schema the datStore doesn't go offline and finish processing next mutation.
     * @throws AmplifyException On failure interacting with storage adapter
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws JSONException If unable to parse the JSON.
     */
    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    @Test
    public void syncRecoversRetryingMultipleTimesOnRecoverableError()
            throws AmplifyException, JSONException, InterruptedException {
        CountDownLatch syncLatch = new CountDownLatch(3);
        CountDownLatch mutationLatch = new CountDownLatch(1);

        // Arrange
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        Person person1 = createPerson("Test", "Dummy I");
        mockSuccessOnSubscriptions(mockApiCategory);

        mockSuccessfulMutation(mutationLatch, mockApiCategory, person1);

        mockSyncWithOneRecoverableErrorAndThenSuccess(syncLatch, mockApiCategory);

        SynchronousDataStore synchronousDataStore = getSynchronousDataStore(mockApiCategory);

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));
        assertTrue(syncLatch.await(60, TimeUnit.SECONDS));
        // Save person 1 and make sure appsync is called which tells that dataStore is in API sync mode.
        synchronousDataStore.save(person1);
        Person result1 = synchronousDataStore.get(Person.class, person1.getId());
        assertTrue(mutationLatch.await(60, TimeUnit.SECONDS));
        assertEquals(person1, result1);
    }

    @NonNull
    private SynchronousDataStore getSynchronousDataStore(ApiCategory mockApiCategory)
            throws JSONException, AmplifyException {
        JSONObject dataStorePluginJson = new JSONObject()
                .put("syncIntervalInMinutes", 60);
        DataStoreConfiguration config = DataStoreConfiguration.builder()
                .errorHandler(mock(DataStoreErrorHandler.class))
                .build();
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                .modelProvider(modelProvider)
                .apiCategory(mockApiCategory)
                .dataStoreConfiguration(config)
                .isSyncRetryEnabled(true)
                .build();
        SynchronousDataStore synchronousDataStore = SynchronousDataStore
                .delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        awsDataStorePlugin.start(() -> { }, (onError) -> { });
        return synchronousDataStore;
    }

    @SuppressWarnings("unchecked")
    private Person setupApiMockForIrrecoverableErrorOnSync(CountDownLatch latch, ApiCategory mockApiCategory) {
        Person person1 = createPerson("Test", "Dummy I");
        mockSuccessOnSubscriptions(mockApiCategory);

        mockSuccessfulMutation(latch, mockApiCategory, person1);

        mockSyncWithOneIrrecoverableErrorAndOneSuccess(latch, mockApiCategory);
        return person1;
    }

    private void mockSyncWithOneIrrecoverableErrorAndOneSuccess(CountDownLatch latch, ApiCategory mockApiCategory) {
        // Setup to failure for sync on first model and failure on second model
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 2;
            Consumer<AmplifyException> onError =
                    invocation.getArgument(indexOfResponseConsumer);
            onError.accept(new ApiException.NonRetryableException("", ""));
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Car car = Car.builder().build();
            ModelMetadata modelMetadata = new ModelMetadata(car.getId(), false, 1, Temporal.Timestamp.now(), "Person");
            ModelWithMetadata<Car> modelWithMetadata = new ModelWithMetadata<>(car, modelMetadata);
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Car>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Car>> data =
                    new PaginatedResult<>(Collections.singletonList(modelWithMetadata), null);
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).when(mockApiCategory).query(any(), any(), any());
    }

    private void mockSyncWithOneRecoverableErrorAndThenSuccess(CountDownLatch latch, ApiCategory mockApiCategory) {
        // Setup to failure for sync on first model and success on subsequent calls.
        Person person = Person.builder().firstName("test").lastName("test").build();
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 2;
            Consumer<ApiException> onError = invocation.getArgument(indexOfResponseConsumer);
            LOG.info("sync error countdown");
            onError.accept(new ApiException("", ""));
            latch.countDown();
            return mock(GraphQLOperation.class);
        })
                .doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            ModelMetadata modelMetadata = new ModelMetadata(person.getId(), false, 1,
                    Temporal.Timestamp.now(), "Person");
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person, modelMetadata);
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Person>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Person>> data =
                    new PaginatedResult<>(Collections.singletonList(modelWithMetadata), null);
            LOG.info("sync first success countdown");
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            latch.countDown();
            return mock(GraphQLOperation.class);
        })
                .doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Car car = Car.builder()
                    .owner(person).build();
            ModelMetadata modelMetadata = new ModelMetadata(car.getId(), false, 1,
                    Temporal.Timestamp.now(), "Person");
            ModelWithMetadata<Car> modelWithMetadata = new ModelWithMetadata<>(car, modelMetadata);
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Car>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Car>> data =
                    new PaginatedResult<>(Collections.singletonList(modelWithMetadata), null);
            latch.countDown();
            LOG.info("sync second success countdown");
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            return mock(GraphQLOperation.class);
        }).when(mockApiCategory).query(any(), any(), any());
    }

    // Varied types in Observable.fromArray(...).
    private void mockSuccessfulMutation(CountDownLatch latch, ApiCategory mockApiCategory, Person person1) {
        //When mutate is called on the appsync success is returned.
        doAnswer(invocation -> {
            //When mutate is called on the appsync for the second time success response is returned
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Person>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(person1.getId(), false, 1, Temporal.Timestamp.now(),
                    "Person");
            ModelWithMetadata<Person> modelWithMetadata = new ModelWithMetadata<>(person1, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            // latch makes sure success response is returned.
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).when(mockApiCategory).mutate(any(), any(), any());
    }

    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    private void mockSuccessOnSubscriptions(ApiCategory mockApiCategory) {
        //Mock success on subscription.
        doAnswer(invocation -> {
            int indexOfStartConsumer = 1;
            Consumer<String> onStart = invocation.getArgument(indexOfStartConsumer);
            GraphQLOperation<?> mockOperation = mock(GraphQLOperation.class);
            doAnswer(opAnswer -> {
                return null;
            }).when(mockOperation).cancel();

            // Trigger the subscription start event.
            onStart.accept(RandomString.string());
            return mockOperation;
        }).when(mockApiCategory).subscribe(
                any(GraphQLRequest.class),
                any(Consumer.class),
                any(Consumer.class),
                any(Consumer.class),
                any(Action.class)
        );
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

    private Person createPerson(String firstName, String lastName) {
        return Person.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
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
        mockApiCategory.addPlugin(mockApiPlugin);
        mockApiCategory.configure(new ApiCategoryConfiguration(), getApplicationContext());
        mockApiCategory.initialize(getApplicationContext());
        return mockApiCategory;
    }
}
