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
import com.amplifyframework.testmodels.commentsblog.PostAuthorJoin;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.ModelAssert;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;

/**
 * Tests the functions of {@link AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
public final class BasicCloudSyncInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 30;

    private SynchronousApi api;
    private SynchronousAppSync appSync;
    private SynchronousDataStore dataStore;

    /**
     * Once, before any/all tests in this class, setup miscellaneous dependencies,
     * including synchronous API, AppSync, and DataStore interfaces. The API and AppSync instances
     * are used to arrange/validate data. The DataStore interface will delegate to an
     * {@link AWSDataStorePlugin}, which is the thing we're actually testing.
     * @throws AmplifyException On failure to read config, setup API or DataStore categories
     */
    @Before
    public void setup() throws AmplifyException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));

        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfiguration");

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
                .syncExpression(PostAuthorJoin.class, () -> PostAuthorJoin.CREATED_AT.gt(tenMinutesAgoDateTime))
                .build())
            .finish();
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    /**
     * Clear the DataStore after each test.  Without calling clear in between tests, all tests after the first will fail
     * with this error: android.database.sqlite.SQLiteReadOnlyDatabaseException: attempt to write a readonly database.
     * @throws DataStoreException On failure to clear DataStore.
     */
    @After
    public void teardown() throws DataStoreException {
        dataStore.clear();
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
     * Verify that updating an item shortly after creating it succeeds.  This can be tricky because the _version
     * returned in the response from the create request must be included in the input for the subsequent update request.
     * @throws DataStoreException On failure to save or query items from DataStore.
     * @throws ApiException On failure to query the API.
     */
    @Test
    public void updateAfterCreate() throws DataStoreException, ApiException {
        // Setup
        BlogOwner richard = BlogOwner.builder()
                .name("Richard")
                .build();
        BlogOwner updatedRichard = richard.copyOfBuilder()
                .name("Richard McClellan")
                .build();
        String modelName = BlogOwner.class.getSimpleName();

        // Expect two mutations to be published to AppSync.
        HubAccumulator richardAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, richard.getId()), 2)
                .start();

        // Create an item, then update it and save it again.
        dataStore.save(richard);
        dataStore.save(updatedRichard);

        // Verify that 2 mutations were published.
        richardAccumulator.await(30, TimeUnit.SECONDS);

        // Verify that the updatedRichard is saved in the DataStore.
        BlogOwner localRichard = dataStore.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, localRichard);

        // Verify that the updatedRichard is saved on the backend.
        BlogOwner remoteRichard = api.get(BlogOwner.class, richard.getId());
        ModelAssert.assertEqualsIgnoringTimestamps(updatedRichard, remoteRichard);
    }
}
