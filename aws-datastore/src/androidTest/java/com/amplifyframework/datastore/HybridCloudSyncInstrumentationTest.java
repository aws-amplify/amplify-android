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
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.SerializedModel;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates the ability of the {@link AWSDataStorePlugin} to sync data up/down
 * from AppSync, when being used by means of "Hybrid-friendly" API invocations:
 *
 *   1. Saving a {@link SerializedModel} via
 *      {@link AWSDataStorePlugin#save(Model, Consumer, Consumer)};
 *   2. Deleting a {@link SerializedModel} via
 *      {@link AWSDataStorePlugin#delete(Model, Consumer, Consumer)};
 *   3. Querying for {@link SerializedModel} by means of
 *      {@link AWSDataStorePlugin#query(String, QueryOptions, Consumer, Consumer)}.
 *
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
@Ignore(
    "Over time, this test will create a large DynamoDB table. Even if we delete the content " +
    "through the AppSyncClient utility, the database will have lots of tombstone'd rows. " +
    "These entries will be synced, the next time this test runs, and the DataStore initializes. " +
    "After several runs, that sync will grow large and timeout the test, before the test can " +
    "run any business logic. A manual workaround exists, by running this cleanup script: " +
    "https://gist.github.com/jamesonwilliams/c76169676cb99c51d997ef0817eb9278#quikscript-to-clear-appsync-tables"
)
public final class HybridCloudSyncInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 30;

    private SchemaProvider schemaProvider;
    private SynchronousApi api;
    private SynchronousAppSync appSync;
    private SynchronousDataStore normalBehaviors;
    private SynchronousHybridBehaviors hybridBehaviors;

    /**
     * DataStore is configured with a real AppSync endpoint. API and AppSync clients
     * are used to arrange/validate state before/after exercising the DataStore. The {@link Amplify}
     * facade is intentionally *not* used, since we don't want to pollute the instrumentation
     * test process with global state. We need an *instance* of the DataStore.
     * @throws AmplifyException On failure to configure Amplify, API/DataStore categories.
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

        schemaProvider = SchemaLoader.loadFromAssetsDirectory("schemas/commentsblog");
        DataStoreCategory dataStoreCategory = DataStoreCategoryConfigurator.begin()
            .api(apiCategory)
            .clearDatabase(true)
            .context(context)
            .modelProvider(schemaProvider)
            .resourceId(configResourceId)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .finish();
        AWSDataStorePlugin plugin =
            (AWSDataStorePlugin) dataStoreCategory.getPlugin("awsDataStorePlugin");
        normalBehaviors = SynchronousDataStore.delegatingTo(dataStoreCategory);
        hybridBehaviors = SynchronousHybridBehaviors.delegatingTo(plugin);
    }

    /**
     * When we save {@link SerializedModel}s, we should find them in the cloud,
     * shortly there-after. Saving associated serialized models will work.
     * @throws AmplifyException For a variety of reasons, including failure to build schema,
     *                          or bad interaction with API or DataStore
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void associatedModelsAreSyncedUpToCloud() throws AmplifyException {
        // First up, we're going to save a "leaf" model, a BlogOwner.
        String ownerModelName = BlogOwner.class.getSimpleName();
        ModelSchema ownerSchema = schemaProvider.modelSchemas().get(ownerModelName);
        assertNotNull(ownerSchema);
        BlogOwner owner = BlogOwner.builder()
            .name("Guillermo Esteban")
            .build();
        Map<String, Object> ownerData = new HashMap<>();
        ownerData.put("id", owner.getId());
        ownerData.put("name", owner.getName());
        SerializedModel serializedOwner = SerializedModel.builder()
            .serializedData(ownerData)
            .modelSchema(ownerSchema)
            .build();

        // Setup an accumulator so we know when there has been a publication.
        HubAccumulator ownerAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(owner), 1)
                .start();
        hybridBehaviors.save(serializedOwner);
        ownerAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Validate that the Blog Owner was saved locally, and in the cloud.
        List<SerializedModel> allCurrentSerializedOwners = hybridBehaviors.list(ownerSchema.getName());
        assertTrue(allCurrentSerializedOwners.contains(serializedOwner));
        List<BlogOwner> allCurrentBlogOwners = normalBehaviors.list(BlogOwner.class);
        assertTrue(allCurrentBlogOwners.contains(owner));
        assertEquals(owner, api.get(BlogOwner.class, owner.getId()));

        // Now, we're going to save a type with a connection.

        // Build a blog, and its serialized form. Blog has association to a BlogOwner.
        Blog blog = Blog.builder()
            .name("A wonderful blog")
            .owner(owner)
            .build();
        Map<String, Object> blogData = new HashMap<>();
        blogData.put("id", blog.getId());
        blogData.put("name", blog.getName());
        blogData.put("owner", SerializedModel.builder()
            .serializedData(Collections.singletonMap("id", owner.getId()))
            .modelSchema(null)
            .build());
        String blogSchemaName = Blog.class.getSimpleName();
        ModelSchema blogSchema = schemaProvider.modelSchemas().get(blogSchemaName);
        assertNotNull(blogSchema);
        SerializedModel serializedBlog = SerializedModel.builder()
            .serializedData(blogData)
            .modelSchema(blogSchema)
            .build();

        // Save the blog
        HubAccumulator blogAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(blog), 1)
                .start();
        hybridBehaviors.save(serializedBlog);
        blogAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Validate that we find the blog locally, and on the remote system.
        List<SerializedModel> allCurrentSerializedBlogs = hybridBehaviors.list(blogSchema.getName());
        assertTrue(allCurrentSerializedBlogs.contains(serializedBlog));
        List<Blog> allCurrentBlogs = normalBehaviors.list(Blog.class);
        assertTrue(allCurrentBlogs.contains(blog));
        Blog foundBlog = api.get(Blog.class, blog.getId());
        assertEquals(blog, foundBlog);
        assertEquals(owner.getId(), foundBlog.getOwner().getId());
    }

    /**
     * When the cloud sees an update to its data, the new data should be reflected in the
     * local store. What's more, we should be able to query for the updated data by its model names,
     * and expect to see the result, that way. This should hold for associated models, too.
     * @throws AmplifyException For a variety of reasons, including failure to build schema,
     *                          or bad interaction with API or DataStore
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void associatedModelAreSyncedDownFromCloud() throws AmplifyException {
        // Create a BlogOwner on the remote system,
        // and wait for it to trickle back to the client.
        BlogOwner owner = BlogOwner.builder()
            .name("Agent Texas")
            .build();
        String ownerModelName = BlogOwner.class.getSimpleName();
        ModelSchema ownerSchema = schemaProvider.modelSchemas().get(ownerModelName);
        assertNotNull(ownerSchema);
        HubAccumulator ownerAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(owner), 1)
                .start();
        appSync.create(owner, ownerSchema);
        ownerAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Now, validate that we see the data locally, when we query for serialized models
        // and by Java BlogOwners.
        Map<String, Object> expectedOwnerData = new HashMap<>();
        expectedOwnerData.put("id", owner.getId());
        expectedOwnerData.put("name", owner.getName());
        List<SerializedModel> actualSerializedOwners = hybridBehaviors.list(ownerSchema.getName());
        assertTrue(actualSerializedOwners.contains(SerializedModel.builder()
            .serializedData(expectedOwnerData)
            .modelSchema(ownerSchema)
            .build()));
        assertTrue(normalBehaviors.list(BlogOwner.class).contains(owner));

        // Now, remotely save a model that has an association to the owner above.
        Blog blog = Blog.builder()
            .name("Blog about Texas")
            .owner(owner)
            .build();
        String blogModelName = Blog.class.getSimpleName();
        ModelSchema blogSchema = schemaProvider.modelSchemas().get(blogModelName);
        assertNotNull(blogSchema);
        HubAccumulator blogAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(blog), 1)
                .start();
        appSync.create(blog, blogSchema);
        blogAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Validate that we can find the newly associated model locally, now.
        Map<String, Object> expectedBlogData = new HashMap<>();
        expectedBlogData.put("id", blog.getId());
        expectedBlogData.put("name", blog.getName());
        expectedBlogData.put("owner", SerializedModel.builder()
            .serializedData(Collections.singletonMap("id", owner.getId()))
            .modelSchema(null)
            .build()
        );
        List<SerializedModel> expectedSerializedBlogs = hybridBehaviors.list(blogSchema.getName());
        assertTrue(expectedSerializedBlogs.contains(SerializedModel.builder()
            .serializedData(expectedBlogData)
            .modelSchema(blogSchema)
            .build()));
        assertTrue(normalBehaviors.list(Blog.class).contains(blog));
    }
}
