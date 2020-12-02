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
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.SerializedModel;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;

/**
 * Tests that a model containing temporal types can be synced up and down
 * from the cloud, when used from a hybrid platform (Flutter).
 */
@Ignore(
    "Over time, this test will create a large DynamoDB table. Even if we delete the content " +
    "through the AppSyncClient utility, the database will have lots of tombstone'd rows. " +
    "These entries will be synced, the next time this test runs, and the DataStore initializes. " +
    "After several runs, that sync will grow large and timeout the test, before the test can " +
    "run any business logic. A manual workaround exists, by running this cleanup script: " +
    "https://gist.github.com/jamesonwilliams/c76169676cb99c51d997ef0817eb9278#quikscript-to-clear-appsync-tables"
)
public final class HybridTemporalSyncInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 30;

    private ModelSchema modelSchema;
    private SynchronousApi api;
    private SynchronousAppSync appSync;
    private SynchronousHybridBehaviors hybridBehaviors;

    /**
     * DataStore is configured with a real AppSync endpoint. API and AppSync clients
     * are used to arrange/validate state before/after exercising the DataStore. The {@link Amplify}
     * facade is intentionally *not* used, since we don't want to pollute the instrumentation
     * test process with global state. We need an *instance* of the DataStore.
     * @throws AmplifyException On failure to configure Amplify, API/DataStore categories.
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
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

        SchemaProvider schemaProvider = SchemaLoader.loadFromAssetsDirectory("schemas/meeting");
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
        hybridBehaviors = SynchronousHybridBehaviors.delegatingTo(plugin);

        // Get a handle to the Meeting model schema that we loaded into the DataStore in @Before.
        String modelName = Meeting.class.getSimpleName();
        modelSchema = schemaProvider.modelSchemas().get(modelName);
    }

    /**
     * It is possible to dispatch a model that contain temporal types. After publishing
     * such a model to the cloud, we can query AppSync and find it there.
     * @throws ApiException on failure to communicate with AppSync API in verification phase of test
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void temporalTypesAreSyncedUpToCloud() throws ApiException {
        // Prepare a SerializedModel that we will save to DataStore.
        Meeting meeting = createMeeting();
        Map<String, Object> sentData = toMap(meeting);
        SerializedModel sentModel = SerializedModel.builder()
            .serializedData(sentData)
            .modelSchema(modelSchema)
            .build();
        HubAccumulator publicationAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelSchema.getName(), sentModel.getId()), 1)
                .start();
        hybridBehaviors.save(sentModel);
        publicationAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Retrieve the model from AppSync.
        Meeting remoteMeeting = api.get(Meeting.class, sentModel.getId());

        // Inspect the fields of the data in AppSync, and prepare it into a map
        // that we can compare with what we sent. Are they the same? They should be.
        assertEquals(sentData, toMap(remoteMeeting));
    }

    /**
     * It is possible to receive a model with temporal types over a subscription.
     * After receiving such a model, we can query it locally and inspect its fields.
     * The temporal values should be the same as what was saved remotely.
     * @throws DataStoreException on failure to interact with AppSync
     */
    @Ignore("It passes. Not automating due to operational concerns as noted in class-level @Ignore.")
    @Test
    public void temporalTypesAreSyncedDownFromCloud() throws DataStoreException {
        // Save a meeting, remotely. Wait for it to show up locally.
        Meeting meeting = createMeeting();
        HubAccumulator receiptAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(meeting.getId()), 1)
                .start();
        appSync.create(meeting, modelSchema);
        receiptAccumulator.awaitFirst(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Look through the models that now exist locally.
        // One of them should be the thing we just saved to the backend.
        // Any others will have come in through the base sync in @Before.
        // When we find the clone, validate its fields.
        List<SerializedModel> clonedMeetings = hybridBehaviors.list(modelSchema.getName());
        SerializedModel clone = findById(clonedMeetings, meeting.getId());
        assertEquals(toMap(meeting), clone.getSerializedData());
    }

    private static SerializedModel findById(List<SerializedModel> haystackModels, String needleId) {
        for (SerializedModel serializedModel : haystackModels) {
            if (serializedModel.getId().equals(needleId)) {
                return serializedModel;
            }
        }
        throw new NoSuchElementException("No model found with id = " + needleId);
    }

    private static Meeting createMeeting() {
        return Meeting.builder()
            .name("A great meeting")
            .date(new Temporal.Date(new Date()))
            .dateTime(new Temporal.DateTime(new Date(), 0))
            .time(new Temporal.Time(new Date()))
            .timestamp(new Temporal.Timestamp(new Date()))
            .build();
    }

    private static Map<String, Object> toMap(Meeting meeting) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", meeting.getId());
        map.put("name", meeting.getName());
        map.put("date", meeting.getDate());
        map.put("dateTime", meeting.getDateTime());
        map.put("time", meeting.getTime());
        map.put("timestamp", meeting.getTimestamp());
        return map;
    }
}
