package com.amplifyframework.datastore;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryConfiguration;
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
import com.amplifyframework.testmodels.customprimarykey.AmplifyModelProvider;
import com.amplifyframework.testmodels.customprimarykey.Blog2;
import com.amplifyframework.testmodels.customprimarykey.BlogOwnerWithCustomPK;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class CascadeDeleteTest {

    private static final String MOCK_API_PLUGIN_NAME = "MockApiPlugin";
    private Context context;
    private ModelProvider modelProvider;
    BlogOwnerWithCustomPK blogOwnerWithCustomPK = createBlogOwner();
    Blog2 blog = createBlog(blogOwnerWithCustomPK);
    SynchronousDataStore synchronousDataStore;

    /**
     * Wire up dependencies for the SyncProcessor, and build one for testing.
     * @throws AmplifyException On failure to load models into registry
     */
    @Before
    public void setup() throws AmplifyException {
        this.context = getApplicationContext();
        modelProvider = spy(AmplifyModelProvider.getInstance());
        this.modelProvider = spy(AmplifyModelProvider.getInstance());
    }

    /**
     * When {@link Completable} completes,
     * then the local storage adapter should have all of the remote model state.
     * @throws AmplifyException On failure interacting with storage adapter
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws JSONException If unable to parse the JSON.
     */
    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    @Test
    public void cascadeDeleteChildModelWithCPKData() throws AmplifyException, JSONException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        // Arrange for the user-provided conflict handler to always request local retry.
        ApiCategory mockApiCategory = mockApiCategoryWithGraphQlApi();
        BlogOwnerWithCustomPK blogOwnerWithCustomPK = setupApiMock(latch, mockApiCategory);

        JSONObject dataStorePluginJson = new JSONObject()
                .put("syncIntervalInMinutes", 60);
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                .modelProvider(modelProvider)
                .apiCategory(mockApiCategory)
                .dataStoreConfiguration(DataStoreConfiguration.builder()
                        .build())
                .build();
        synchronousDataStore = SynchronousDataStore.delegatingTo(awsDataStorePlugin);
        awsDataStorePlugin.configure(dataStorePluginJson, context);
        awsDataStorePlugin.initialize(context);
        awsDataStorePlugin.start(() -> { }, (onError) -> { });

        // Trick the DataStore since it's not getting initialized as part of the Amplify.initialize call chain
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(InitializationStatus.SUCCEEDED));

        // Save person 1
        synchronousDataStore.save(blogOwnerWithCustomPK);
        //Person result1 = synchronousDataStore.get(Person.class, person1.getId());
        assertTrue(latch.await(7, TimeUnit.SECONDS));
        //assertEquals(person1, result1);
    }

    @SuppressWarnings("unchecked")
    private BlogOwnerWithCustomPK setupApiMock(CountDownLatch latch, ApiCategory mockApiCategory) {
        //Mock success on subscription.
        doAnswer(invocation -> {
            int indexOfStartConsumer = 1;
            Consumer<String> onStart = invocation.getArgument(indexOfStartConsumer);
            GraphQLOperation<?> mockOperation = mock(GraphQLOperation.class);
            doAnswer(opAnswer -> null).when(mockOperation).cancel();

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

        //When mutate is called to save blog owner save blog.
        doAnswer(invocation -> {
            //When mutate is called on the appsync for the second time success response is returned
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<BlogOwnerWithCustomPK>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(blogOwnerWithCustomPK.getId(),
                    false, 1, Temporal.Timestamp.now(),
                    "BlogOwnerWithCustomPK");
            ModelWithMetadata<BlogOwnerWithCustomPK> modelWithMetadata = new ModelWithMetadata<>(blogOwnerWithCustomPK,
                    modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            // latch makes sure success response is returned.
            synchronousDataStore.save(blog);
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).doAnswer(invocation -> {
            //When mutate is called on the appsync for the second time success response is returned
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<ModelWithMetadata<Blog2>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(blog.getId(), false, 1, Temporal.Timestamp.now(),
                    "Blog");
            ModelWithMetadata<Blog2> modelWithMetadata = new ModelWithMetadata<>(blog, modelMetadata);
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            // latch makes sure success response is returned.
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).when(mockApiCategory).mutate(any(), any(), any());

        // Setup to mimic successful sync
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            ModelMetadata modelMetadata = new ModelMetadata(blogOwnerWithCustomPK.getId(), false, 1,
                    Temporal.Timestamp.now(), "Person");
            ModelWithMetadata<BlogOwnerWithCustomPK> modelWithMetadata = new ModelWithMetadata<>(blogOwnerWithCustomPK,
                    modelMetadata);
            // Mock the API emitting an ApiEndpointStatusChangeEvent event.
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwnerWithCustomPK>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<BlogOwnerWithCustomPK>> data =
                    new PaginatedResult<>(Collections.singletonList(modelWithMetadata), null);
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            latch.countDown();
            return mock(GraphQLOperation.class);

        }).doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            ModelMetadata modelMetadata = new ModelMetadata(blog.getId(),
                    false, 1, Temporal.Timestamp.now(), "Person");
            ModelWithMetadata<Blog2> modelWithMetadata = new ModelWithMetadata<>(blog, modelMetadata);
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Blog2>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Blog2>> data =
                    new PaginatedResult<>(Collections.singletonList(modelWithMetadata), null);
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            latch.countDown();
            return mock(GraphQLOperation.class);
        }).when(mockApiCategory).query(any(), any(), any());
        return blogOwnerWithCustomPK;
    }

//    private static ArgumentMatcher<GraphQLRequest<ModelWithMetadata<Person>>> getMatcherFor(Person person) {
//        return graphQLRequest -> {
//            try {
//                JSONObject payload = new JSONObject(graphQLRequest.getContent());
//                String modelIdInRequest = payload.getJSONObject("variables").getJSONObject("input").getString("id");
//                return person.getId().equals(modelIdInRequest);
//            } catch (JSONException exception) {
//                fail("Invalid GraphQLRequest payload." + exception.getMessage());
//            }
//            return false;
//        };
//    }

    private BlogOwnerWithCustomPK createBlogOwner() {
        return BlogOwnerWithCustomPK.builder()
                .name("testBlogOwner")
                .wea("testWea")
                .build();
    }

    private Blog2 createBlog(BlogOwnerWithCustomPK owner) {
        return Blog2.builder()
                .name("testBlog")
                .owner(owner)
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
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<Blog2>>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            PaginatedResult<ModelWithMetadata<Blog2>> data = new PaginatedResult<>(Collections.emptyList(), null);
            onResponse.accept(new GraphQLResponse<>(data, Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));
        mockApiCategory.addPlugin(mockApiPlugin);
        mockApiCategory.configure(new ApiCategoryConfiguration(), getApplicationContext());
        mockApiCategory.initialize(getApplicationContext());
        return mockApiCategory;
    }
}
