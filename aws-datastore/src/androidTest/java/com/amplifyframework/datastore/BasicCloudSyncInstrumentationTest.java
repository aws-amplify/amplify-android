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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Author;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.ModelAssert;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the functions of {@link AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
public final class BasicCloudSyncInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 60;

    private static SynchronousApi api;
    private static SynchronousAppSync appSync;
    private static SynchronousDataStore dataStore;

    /**
     * Once, before any/all tests in this class, setup miscellaneous dependencies,
     * including synchronous API, AppSync, and DataStore interfaces. The API and AppSync instances
     * are used to arrange/validate data. The DataStore interface will delegate to an
     * {@link AWSDataStorePlugin}, which is the thing we're actually testing.
     * @throws AmplifyException On failure to read config, setup API or DataStore categories
     */
    @BeforeClass
    public static void setup() throws AmplifyException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));

        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfigurationupdated");

        // Setup an API
        CategoryConfiguration apiCategoryConfiguration =
            AmplifyConfiguration.fromConfigFile(context, configResourceId)
                .forCategoryType(CategoryType.API);
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(new AWSApiPlugin());
        apiCategory.configure(apiCategoryConfiguration, context);

        // To arrange and verify state, we need to access the supporting AppSync API
        api = SynchronousApi.delegatingTo(apiCategory);
        appSync = SynchronousAppSync.using(AppSyncClient.via(apiCategory));

        long tenMinutesAgo = new Date().getTime() - TimeUnit.MINUTES.toMillis(10);
        Temporal.DateTime tenMinutesAgoDateTime = new Temporal.DateTime(new Date(tenMinutesAgo), 0);
        DataStoreCategory dataStoreCategory = DataStoreCategoryConfigurator.begin()
            .api(apiCategory)
            .clearDatabase(true)
            .context(context)
            .modelProvider(AmplifyModelProvider.getInstance())
            .resourceId(configResourceId)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .dataStoreConfiguration(DataStoreConfiguration.builder()
                .syncExpression(BlogOwner.class, () -> BlogOwner.CREATED_AT.gt(tenMinutesAgoDateTime))
                .syncExpression(Blog.class, () -> Blog.CREATED_AT.gt(tenMinutesAgoDateTime))
                .syncExpression(Post.class, () -> Post.CREATED_AT.gt(tenMinutesAgoDateTime))
                .syncExpression(Comment.class, () -> Comment.CREATED_AT.gt(tenMinutesAgoDateTime))
                .syncExpression(Author.class, () -> Author.CREATED_AT.gt(tenMinutesAgoDateTime))
                .build())
            .finish();
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    /**
     * Clear the DataStore after each test.  Without calling clear in between tests, all tests after the first will fail
     * with this error: android.database.sqlite.SQLiteReadOnlyDatabaseException: attempt to write a readonly database.
     * @throws DataStoreException On failure to clear DataStore.
     */
    @AfterClass
    public static void teardown() throws DataStoreException {
        if (dataStore != null) {
            try {
                dataStore.clear();
            } catch (Exception error) {
                // ok to ignore since problem encountered during tear down of the test.
            }
        }
    }

    /**
     * Save a BlogOwner via DataStore, wait a bit, check API to see if the BlogOwner is there, remotely.
     * @throws DataStoreException On failure to save item into DataStore (first step)
     * @throws ApiException On failure to retrieve a valid response from API when checking
     *                      for remote presence of saved item
     * @throws AmplifyException On failure to arrange a {@link DataStoreCategory} via the
     *                          {@link DataStoreCategoryConfigurator}
     */
    @Test
    public void syncUpToCloudIsWorking() throws AmplifyException {
        // Start listening for model publication events on the Hub.
        BlogOwner localCharley = BlogOwner.builder()
            .name("Charley Crockett")
            .build();
        String modelName = BlogOwner.class.getSimpleName();
        HubAccumulator publishedMutationsAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, localCharley.getId()), 1)
                .start();

        // Save Charley Crockett, a guy who has a blog, into the DataStore.
        dataStore.save(localCharley);

        // Wait for a Hub event telling us that our Charley model got published to the cloud.
        publishedMutationsAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Try to get Charley from the backend.
        BlogOwner remoteCharley = api.get(BlogOwner.class, localCharley.getId());

        // A Charley is a Charley is a Charley, right?
        assertEquals(localCharley.getId(), remoteCharley.getId());
        assertEquals(localCharley.getName(), remoteCharley.getName());
    }

    /**
     * The sync engine should receive mutations for its managed models, through its
     * subscriptions. When we create a model remotely, the sync engine should respond
     * by processing the subscription event and saving the model locally.
     * @throws DataStoreException On failure to query the local data store for
     *                            local presence of arranged data (second step)
     * @throws AmplifyException On failure to arrange a {@link DataStoreCategory} via the
     *                          {@link DataStoreCategoryConfigurator}
     */
    @Test
    public void syncDownFromCloudIsWorking() throws AmplifyException {
        // This model will get saved to the cloud.
        BlogOwner jameson = BlogOwner.builder()
            .name("Jameson Williams")
            .createdAt(new Temporal.DateTime(new Date(), 0))
            .build();

        // Start watching locally, to see if it shows up on the client.
        HubAccumulator receiptAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(jameson.getId()), 1)
                .start();

        // Act: create the model in the cloud
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        GraphQLResponse<ModelWithMetadata<BlogOwner>> createResponse = appSync.create(jameson, schema);
        ModelMetadata metadata = createResponse.getData().getSyncMetadata();
        assertEquals(Integer.valueOf(1), metadata.getVersion());

        // Wait for the events to show up on Hub.
        receiptAccumulator.awaitFirst(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Jameson should be in the local DataStore.
        BlogOwner owner = dataStore.get(BlogOwner.class, jameson.getId());
        assertEquals("Jameson Williams", owner.getName());
        assertEquals(jameson.getId(), owner.getId());
    }

    /**
     * Verify that updating an item shortly after creating it succeeds locally.
     *
     * Note: If this test periodically fails, consider the immediate update save may still be in process since
     * we are only waiting on 1 hub event. Because we call back to back, I haven't seen this happen yet.
     *
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createThenUpdate() throws DataStoreException, ApiException {
        // Setup
        BlogOwner richard = BlogOwner.builder()
                .name("Richard")
                .build();
        BlogOwner updatedRichard = richard.copyOfBuilder()
                .name("Richard McClellan")
                .build();
        String modelName = BlogOwner.class.getSimpleName();

        // Expect at least 1 mutation to be published to AppSync.
        HubAccumulator richardAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, richard.getId()), 1)
                .start();

        // Create an item, then update it and save it again.
        dataStore.save(richard);
        dataStore.save(updatedRichard);

        // Verify that at least 1 mutations was published.
        richardAccumulator.await(60, TimeUnit.SECONDS);

        // Verify that the updatedRichard is saved in the DataStore.
        BlogOwner localRichard = dataStore.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, localRichard);

        // Verify that the updatedRichard is saved on the backend.
        BlogOwner remoteRichard = api.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, remoteRichard);
    }

    /**
     * Verify that updating an item shortly after creating it succeeds. This can be tricky because the _version
     * returned in the response from the create request must be included in the input for the subsequent update request.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createWaitThenUpdate() throws DataStoreException, ApiException {
        // Setup
        BlogOwner richard = BlogOwner.builder()
                                .name("Richard")
                                .build();
        BlogOwner updatedRichard = richard.copyOfBuilder()
                                       .name("Richard McClellan")
                                       .build();
        String modelName = BlogOwner.class.getSimpleName();

        HubAccumulator accumulator1 =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, richard.getId()), 1);
        HubAccumulator accumulator2 =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, richard.getId()), 1);

        // Create an item, then update it and save it again.
        accumulator1.start();
        dataStore.save(richard);

        // Verify first save published
        accumulator1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Update item and save
        accumulator2.start();
        dataStore.save(updatedRichard);

        // Verify update published
        accumulator2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the updatedRichard is saved in the DataStore.
        BlogOwner localRichard = dataStore.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, localRichard);

        // Verify that the updatedRichard is saved on the backend.
        BlogOwner remoteRichard = api.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, remoteRichard);
    }

    /**
     * Verify that updating a different field of an item immediately after creating it succeeds.
     *
     * Note: If this test periodically fails, consider the immediate update save may still be in process since
     * we are only waiting on 1 hub event. Because we call back to back, I haven't seen this happen yet.
     *
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createThenUpdateDifferentField() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                .name("Richard")
                .build();
        BlogOwner updatedOwner = owner.copyOfBuilder()
                .wea("pon")
                .build();
        String modelName = BlogOwner.class.getSimpleName();

        // Expect at least 1 mutation to be published to AppSync.
        HubAccumulator accumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1)
                        .start();

        // Create an item, then update it with different field and save it again.
        dataStore.save(owner);
        dataStore.save(updatedOwner);

        // Verify that mutation(s) were published.
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the updatedOwner is saved in the DataStore.
        BlogOwner localOwner = dataStore.get(BlogOwner.class, owner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, localOwner);

        // Verify that the updatedOwner is saved on the backend.
        BlogOwner remoteOwner = api.get(BlogOwner.class, owner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, remoteOwner);
    }

    /**
     * Verify that updating a different field of an item succeeds, after verifying the initial item has been published.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createWaitThenUpdateDifferentField() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                              .name("Richard")
                              .build();
        BlogOwner updatedOwner = owner.copyOfBuilder()
                                     .wea("pon")
                                     .build();
        String modelName = BlogOwner.class.getSimpleName();

        HubAccumulator accumulator1 =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1);
        HubAccumulator accumulator2 =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1);

        // Create an item, then
        accumulator1.start();
        dataStore.save(owner);

        // Verify save published
        accumulator1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Update item with different field and save it again.
        accumulator2.start();
        dataStore.save(updatedOwner);

        // Verify update save published
        accumulator2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the updatedOwner is saved in the DataStore.
        BlogOwner localOwner = dataStore.get(BlogOwner.class, owner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, localOwner);

        // Verify that the updatedOwner is saved on the backend.
        BlogOwner remoteOwner = api.get(BlogOwner.class, owner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, remoteOwner);
    }

    /**
     * Verify that updating a different field of the last created shortly after creating two items succeeds.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Ignore("Test passes locally but fails inconsistently on CI. Ignoring the test pending further investigation.")
    @Test
    public void create1ThenCreate2ThenUpdate2() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                .name("Jean")
                .build();
        BlogOwner anotherOwner = BlogOwner.builder()
                .name("Richard")
                .build();
        BlogOwner updatedOwner = anotherOwner.copyOfBuilder()
                .wea("pon")
                .build();
        String modelName = BlogOwner.class.getSimpleName();

        // Expect two mutations to be published to AppSync.
        HubAccumulator accumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, anotherOwner.getId()), 2)
                        .start();

        // Create an item, then update it with different field and save it again.
        dataStore.save(owner);
        dataStore.save(anotherOwner);
        dataStore.save(updatedOwner);

        // Verify that 2 mutations were published.
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the updatedOwner is saved in the DataStore.
        BlogOwner localOwner = dataStore.get(BlogOwner.class, anotherOwner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, localOwner);

        // Verify that the updatedOwner is saved on the backend.
        BlogOwner remoteOwner = api.get(BlogOwner.class, anotherOwner.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedOwner, remoteOwner);
    }

    /**
     * Verify that creating a new item, then immediately deleting succeeds.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Ignore("Test passes locally but fails on CI. Ignoring pending investigation.")
    @Test
    public void createThenDelete() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                .name("Jean")
                .build();

        dataStore.save(owner);
        dataStore.delete(owner);

        // Verify that the owner is deleted from the local data store.
        assertThrows(NoSuchElementException.class, () -> dataStore.get(BlogOwner.class, owner.getId()));
    }

    /**
     * Verify that creating a new item, waiting for it to post, then immediately delete succeeds.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createWaitThenDelete() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                .name("Jean")
                .build();
        String modelName = BlogOwner.class.getSimpleName();

        HubAccumulator accumulator1 =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1);
        HubAccumulator accumulator2 =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1);

        accumulator1.start();
        dataStore.save(owner);
        accumulator1.awaitFirst(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        accumulator2.start();
        dataStore.delete(owner);
        accumulator2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify that the owner is deleted from the local data store.
        assertThrows(NoSuchElementException.class, () -> dataStore.get(BlogOwner.class, owner.getId()));
    }

    /**
     * The test is to create a new Post with Comment, reassign Comment to a different Post.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createPost1WithCommentThenReassignCommentToPost2() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder().name("Owner").build();
        Blog blog = Blog.builder().name("MyBlog").owner(owner).build();
        Post firstPost = Post.builder().title("First Post").status(PostStatus.ACTIVE).rating(3).blog(blog).build();
        Post secondPost = Post.builder().title("Second Post").status(PostStatus.ACTIVE).rating(5).blog(blog).build();
        Comment comment = Comment.builder().content("Some comment").post(firstPost).build();
        String modelName = Comment.class.getSimpleName();

        // Save first post and comment. Then verify that first post and comment were saved.
        HubAccumulator accumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, comment.getId()), 1)
                        .start();
        dataStore.save(owner);
        dataStore.save(blog);
        dataStore.save(firstPost);
        dataStore.save(comment);
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        Comment localComment = dataStore.get(Comment.class, comment.getId());
        Assert.assertEquals(comment.getPost().getId(), localComment.getPost().getId());
        Comment remoteComment = api.get(Comment.class, comment.getId());
        Assert.assertEquals(comment.getPost().getId(), remoteComment.getPost().getId());

        // Reassign comment to second post, save and sync
        Comment commentCopy = comment.copyOfBuilder().post(secondPost).build();
        accumulator = HubAccumulator.create(HubChannel.DATASTORE, 
                publicationOf(modelName, commentCopy.getId()), 1).start();
        dataStore.save(secondPost);
        dataStore.save(commentCopy);
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Verify that comment was reassigned
        Comment currentLocalComment = dataStore.get(Comment.class, comment.getId());
        Assert.assertEquals(secondPost.getId(), currentLocalComment.getPost().getId());
        Comment currentRemoteComment = api.get(Comment.class, comment.getId());
        Assert.assertEquals(secondPost.getId(), currentRemoteComment.getPost().getId());
    }

    /**
     * The test is to test consecutive updates with predicate.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createWaitThenUpdate10TimesWithPredicate() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder()
                .name("Blogger")
                .wea("ryt")
                .build();
        String modelName = BlogOwner.class.getSimpleName();
        QueryPredicate predicate = BlogOwner.WEA.beginsWith("r");

        // Setup 10 updates
        List<String> weas = Arrays.asList("ron", "rth", "rer", "rly", "ren", "rel", "ral", "rec", "rin", "reh");
        List<BlogOwner> owners = new ArrayList<>();
        for (int i = 0; i < weas.size(); i++) {
            BlogOwner updatedOwner = owner.copyOfBuilder().wea(weas.get(i)).build();
            owners.add(updatedOwner);
        }

        HubAccumulator accumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1)
                        .start();

        // Create an item.
        dataStore.save(owner);

        // Wait for the sync.
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Make 10 consecutive updates with predicate
        HubAccumulator updateAccumulator =
                HubAccumulator.create(HubChannel.DATASTORE,
                        publicationOf(modelName, owner.getId()), 10).start();
        for (int i = 0; i < weas.size(); i++) {
            dataStore.save(owners.get(i), predicate);
        }
        updateAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        BlogOwner lastUpdate = owners.get(owners.size() - 1);
        BlogOwner localOwner = dataStore.get(BlogOwner.class, lastUpdate.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(lastUpdate, localOwner);
        BlogOwner remoteOwner = api.get(BlogOwner.class, lastUpdate.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(lastUpdate, remoteOwner);
    }

    /**
     * Create new item, then immediately update a different field. 
     * Wait for sync round trip. Then update the first field.
     *
     * Note: If this test periodically fails, consider the immediate update save may still be in process since
     * we are only waiting on 1 hub event. Because we call back to back, I haven't seen this happen yet.
     *
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void createItemThenUpdateThenWaitThenUpdate() throws DataStoreException, ApiException {
        // Setup
        BlogOwner owner = BlogOwner.builder().name("ownerName").build();
        BlogOwner updatedOwner = owner.copyOfBuilder().wea("pon").build();
        String modelName = BlogOwner.class.getSimpleName();

        // Expect at least 1 update (2 is possible)
        HubAccumulator accumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, owner.getId()), 1)
                        .start();
        // Create new and then immediately update
        dataStore.save(owner);
        dataStore.save(updatedOwner);
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Update the field
        BlogOwner diffFieldUpdated = updatedOwner.copyOfBuilder().name("ownerUpdatedName").build();
        accumulator = HubAccumulator.create(HubChannel.DATASTORE, 
                publicationOf(modelName, diffFieldUpdated.getId()), 1).start();
        dataStore.save(diffFieldUpdated);
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        BlogOwner localOwner = dataStore.get(BlogOwner.class, diffFieldUpdated.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(diffFieldUpdated, localOwner);
        BlogOwner remoteOwner = api.get(BlogOwner.class, diffFieldUpdated.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(diffFieldUpdated, remoteOwner);
    }
}
