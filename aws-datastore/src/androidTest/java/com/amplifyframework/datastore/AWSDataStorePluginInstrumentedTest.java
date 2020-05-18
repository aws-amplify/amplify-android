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
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.datastore.syncengine.PendingMutation;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
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

import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the functions of {@link AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
@Ignore("This test is not reliably passing right now.")
public final class AWSDataStorePluginInstrumentedTest {
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
        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfiguration");

        // Setup an API
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(new AWSApiPlugin());
        apiCategory.configure(AmplifyConfiguration.fromConfigFile(context, configResourceId)
            .forCategoryType(CategoryType.API), context);

        api = SynchronousApi.delegatingTo(apiCategory);
        appSync = SynchronousAppSync.using(AppSyncClient.via(apiCategory));
        dataStore = SynchronousDataStore.delegatingTo(DataStoreCategoryConfigurator.begin()
            .api(apiCategory)
            .clearDatabase(true)
            .context(context)
            .modelProvider(AmplifyModelProvider.getInstance())
            .resourceId(configResourceId)
            .finish());
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
        HubAccumulator publishedMutationsAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(localCharley), 1).start();

        // Save Charley Crockett, a guy who has a blog, into the DataStore.
        dataStore.save(localCharley);

        // Wait for a Hub event telling us that our Charley model got published to the cloud.
        publishedMutationsAccumulator.await();

        // Try to get Charley from the backend.
        BlogOwner remoteCharley = api.get(BlogOwner.class, localCharley.getId());

        // A Charley is a Charley is a Charley, right?
        assertEquals(localCharley.getId(), remoteCharley.getId());
        assertEquals(localCharley.getName(), remoteCharley.getName());
    }

    /**
     * The sync engine should receive mutations for its managed models, through its
     * subscriptions. When we change a model remotely, the sync engine should respond
     * by processing the subscription event and saving the model locally.
     * @throws DataStoreException On failure to query the local data store for
     *                            local presence of arranged data (second step)
     * @throws AmplifyException On failure to arrange a {@link DataStoreCategory} via the
     *                          {@link DataStoreCategoryConfigurator}
     */
    @SuppressWarnings("unchecked") // Unwrapping hub event data
    @Test
    public void syncDownFromCloudIsWorking() throws AmplifyException {
        // Arrange two models up front, so we can know their IDs for other arrangments.
        // First is Jameson with a typo. We create him.
        // Second is Jameson with typo fixed -- we update the original with this record.
        BlogOwner originalModel = BlogOwner.builder()
            .name("Jameson Willlllliams")
            .build();
        BlogOwner updatedModel = originalModel.copyOfBuilder() // This uses the same model ID
            .name("Jameson Williams") // But with corrected name
            .build();

        // Now, start watching the Hub for notifications that we received and processed models
        // from the Cloud. Look specifically for events relating to the model with the above ID.
        // We expected 2: a creation, and an update.
        HubAccumulator receiptAcummulator =
            HubAccumulator.create(HubChannel.DATASTORE, receiptOf(originalModel), 2).start();

        // Act: externally in the Cloud, someone creates a BlogOwner,
        // that contains a misspelling in the last name
        GraphQLResponse<ModelWithMetadata<BlogOwner>> createResponse = appSync.create(originalModel);
        ModelMetadata originalMetadata = createResponse.getData().getSyncMetadata();
        assertNotNull(originalMetadata.getVersion());
        int originalVersion = originalMetadata.getVersion();

        // Act: externally, the BlogOwner in the Cloud is updated, to correct the entry's last name
        GraphQLResponse<ModelWithMetadata<BlogOwner>> updateResponse =
            appSync.update(updatedModel, originalVersion);
        ModelMetadata newMetadata = updateResponse.getData().getSyncMetadata();
        assertNotNull(newMetadata.getVersion());
        int newVersion = newMetadata.getVersion();
        assertEquals(originalVersion + 1, newVersion);

        // Wait for the events to show up on Hub.
        List<HubEvent<?>> seenEvents = receiptAcummulator.await();

        // Another HubEvent tells us that an update occurred in the Cloud;
        // the update was applied locally, to an existing BlogOwner.
        assertEquals(
            Arrays.asList(originalModel, updatedModel),
            Observable.fromIterable(seenEvents)
                .map(HubEvent::getData)
                .map(data -> (ModelWithMetadata<BlogOwner>) data)
                .map(ModelWithMetadata::getModel)
                .toList()
                .blockingGet()
        );

        // Jameson should be in the local DataStore, and last name should be updated.
        BlogOwner owner = dataStore.get(BlogOwner.class, originalModel.getId());
        assertEquals("Jameson Williams", owner.getName());
        assertEquals(originalModel.getId(), owner.getId());
    }

    private <T extends Model> HubEventFilter publicationOf(T model) {
        return event -> {
            DataStoreChannelEventName actualEventName = DataStoreChannelEventName.fromString(event.getName());
            if (!DataStoreChannelEventName.PUBLISHED_TO_CLOUD.equals(actualEventName)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            PendingMutation<T> pendingMutation = (PendingMutation<T>) event.getData();
            if (pendingMutation == null) {
                return false;
            } else if (!model.getClass().isAssignableFrom(pendingMutation.getMutatedItem().getClass())) {
                return false;
            }
            return model.getId().equals(pendingMutation.getMutatedItem().getId());
        };
    }

    private <T extends Model> HubEventFilter receiptOf(T model) {
        return event -> {
            DataStoreChannelEventName actualEventName = DataStoreChannelEventName.fromString(event.getName());
            if (!DataStoreChannelEventName.RECEIVED_FROM_CLOUD.equals(actualEventName)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            ModelWithMetadata<T> modelWithMetadata = (ModelWithMetadata<T>) event.getData();
            if (modelWithMetadata == null) {
                return false;
            } else if (!model.getClass().isAssignableFrom(modelWithMetadata.getModel().getClass())) {
                return false;
            }
            return model.getId().equals(modelWithMetadata.getModel().getId());
        };
    }
}
