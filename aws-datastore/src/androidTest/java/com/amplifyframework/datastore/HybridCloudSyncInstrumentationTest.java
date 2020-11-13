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
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;
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
    private static final int TIMEOUT_SECONDS = 10;

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

        DataStoreCategory dataStoreCategory = DataStoreCategoryConfigurator.begin()
            .api(apiCategory)
            .clearDatabase(true)
            .context(context)
            .modelProvider(SchemaProvider.from(AmplifyModelProvider.getInstance()))
            .resourceId(configResourceId)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .finish();
        AWSDataStorePlugin plugin =
            (AWSDataStorePlugin) dataStoreCategory.getPlugin("awsDataStorePlugin");
        normalBehaviors = SynchronousDataStore.delegatingTo(dataStoreCategory);
        hybridBehaviors = SynchronousHybridBehaviors.delegatingTo(plugin);
    }

    /**
     * When we save an {@link SerializedModel}, we should find that data in the cloud,
     * shortly there-after.
     * @throws AmplifyException For a variety of reasons, including failure to build schema,
     *                          or bad interaction with API or DataStore
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void serializedModelIsSyncedToCloud() throws AmplifyException {
        // Arrange the schema for the mode class. Note: the hybrid platform
        // has to build one piece by piece, they can't infer from BlogOwner.class
        // we generate the schema this way since it saves us lots of work, in this test.
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);

        // Some model -- we will use this to construct a SerializedModel that can be saved
        // and also to query for native models, the make sure that still works, after the save.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Guillermo Esteban")
            .build();

        // Create a serialized model based on the Java model's data
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("id", blogOwner.getId());
        serializedData.put("name", blogOwner.getName());
        SerializedModel serializedModel = SerializedModel.builder()
            .serializedData(serializedData)
            .modelSchema(schema)
            .build();

        // Setup an accumulator so we know when there has been a publication.
        // TODO: this says publicationOf(blogOwner), but it will need to say something without a type
        HubAccumulator publicationAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(blogOwner), 1)
                .start();

        // This is the main test action, where we save a serially model locally.
        hybridBehaviors.save(serializedModel);

        // Now, we wait for the above change to propagate to the backend.
        publicationAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // A few final steps. Let's validate that we can find the model locally
        // by searching for it by model name
        List<SerializedModel> allCurrentSerializedModels = hybridBehaviors.list(schema.getName());
        assertTrue(allCurrentSerializedModels.contains(serializedModel));

        // And that we can find it when searching using the Java-model-based API
        List<BlogOwner> allCurrentBlogOwners = normalBehaviors.list(BlogOwner.class);
        assertTrue(allCurrentBlogOwners.contains(blogOwner));

        // Lastly, let's just double check it's actually present in the AppSync backend.
        assertEquals(blogOwner, api.get(BlogOwner.class, blogOwner.getId()));
    }

    /**
     * When the cloud sees an update to its data, the new data should be reflected in the
     * local store. What's more, we should be able to query for the updated by its model name,
     * and expect to see the result, that way.
     * @throws AmplifyException For a variety of reasons, including failure to build schema,
     *                          or bad interaction with API or DataStore
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void modelSyncedDownFromCloudCanBeQueried() throws AmplifyException {
        // Arrange a model and a schema
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Agent Texas")
            .build();
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);

        HubAccumulator receiptAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(blogOwner), 1)
                .start();

        // Save the model to the backend.
        appSync.create(blogOwner, schema);

        // Wait for the client to receive it over its active subscription
        receiptAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Query the hybrid behaviors for the current hybrid models
        // (Later, we'll verify that the backend change is represented in these.)
        List<SerializedModel> allCurrentSerializedModels = hybridBehaviors.list(schema.getName());

        // Create a SerializedModel that has the same content as the
        // model that was published.
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("id", blogOwner.getId());
        serializedData.put("name", blogOwner.getName());

        // Expect to find a representation of the new model, in this list.
        assertTrue(allCurrentSerializedModels.contains(SerializedModel.builder()
            .serializedData(serializedData)
            .modelSchema(schema)
            .build()));

        // We should also be able to find the Java-language representation of the model
        // (There should be on difference in using the hybrid query vs. the native query.)
        List<BlogOwner> allCurrentBlogOwners = normalBehaviors.list(BlogOwner.class);
        assertTrue(allCurrentBlogOwners.contains(blogOwner));
    }
}
