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
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.aws.GsonVariablesSerializer;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.transformerV2.schemadrift.EnumDrift;
import com.amplifyframework.testmodels.transformerV2.schemadrift.SchemaDrift;
import com.amplifyframework.testmodels.transformerV2.schemadrift.SchemaDriftModelProvider;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SchemaDriftTest {
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
                .modelProvider(SchemaDriftModelProvider.getInstance())
                .resourceId(configResourceId)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .dataStoreConfiguration(DataStoreConfiguration.builder()
                        .syncExpression(SchemaDrift.class, () -> SchemaDrift.CREATED_AT.gt(tenMinutesAgoDateTime))
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
     * Perform DataStore.save.
     * Expected result: Model should be published to AppSync.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testSave() throws AmplifyException {
        dataStore.start();
        SchemaDrift localSchemaDrift = SchemaDrift.builder()
                .createdAt(new Temporal.DateTime(new Date(), 0))
                .enumValue(EnumDrift.ONE)
                .build();
        String modelName = SchemaDrift.class.getSimpleName();
        HubAccumulator publishedMutationsAccumulator =
                HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelName, localSchemaDrift.getId()), 1)
                        .start();

        dataStore.save(localSchemaDrift);

        // Wait for a Hub event telling us that our model got published to the cloud.
        publishedMutationsAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Retrieve from the backend.
        SchemaDrift remoteModel = api.get(SchemaDrift.class, localSchemaDrift.getId());
        assertEquals(remoteModel.getId(), localSchemaDrift.getId());
    }

    /**
     * Save a SchemaDrift model with enum value "THREE" by calling API directly with the
     * mutation request document since the code generated EnumDrift was modified to remove the
     * case so it wouldn't be possible otherwise due to type safety.
     *
     * Expected result: Model should be received as a subscription event and synced to local store.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testSyncEnumWithInvalidValue() throws AmplifyException {
        SchemaDrift directSchemaDrift = api.create(
                new SimpleGraphQLRequest<>(
                        Assets.readAsString("schema-drift-mutation.graphql"),
                        new HashMap<>(),
                        SchemaDrift.class,
                        new GsonVariablesSerializer()
                )
        );
        // Retrieve it directly from API
        SchemaDrift remoteModel = api.get(SchemaDrift.class, directSchemaDrift.getId());
        assertEquals(remoteModel.getId(), directSchemaDrift.getId());

        HubAccumulator receiptOfSchemaDrift =
                HubAccumulator.create(HubChannel.DATASTORE, receiptOf(directSchemaDrift.getId()), 1)
                        .start();

        // Start and sync the models from AppSync
        dataStore.start();

        // Ensure that the model was synced.
        receiptOfSchemaDrift.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Query for the model, expect that the enumValue is null.
        SchemaDrift getSchemaDrift = dataStore.get(SchemaDrift.class, directSchemaDrift.getId());
        assertNull(getSchemaDrift.getEnumValue());
    }
}
