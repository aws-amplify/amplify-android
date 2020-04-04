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

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the functions of {@link AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
public final class AWSDataStorePluginInstrumentedTest {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore:test");

    private static SynchronousApi api;
    private static SynchronousAppSync appSync;
    private static SynchronousDataStore dataStore;

    /**
     * Enable strict mode for catching SQLite leaks.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Setup the Android application context.
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void beforeTests() throws AmplifyException {
        TestConfiguration.configureIfNotConfigured();
        api = SynchronousApi.delegatingToAmplify();
        appSync = SynchronousAppSync.defaultInstance();
        dataStore = SynchronousDataStore.singleton();
    }

    /**
     * Save a BlogOwner via DataStore, wait a bit, check API to see if the BlogOwner is there, remotely.
     * @throws DataStoreException On failure to save item into DataStore (first step)
     * @throws ApiException On failure to retrieve a valid response from API when checking
     *                      for remote presence of saved item
     */
    @Test
    public void blogOwnerSavedIntoDataStoreIsThenQueriableInRemoteAppSyncApi() throws DataStoreException, ApiException {
        // Arrange an ID that we can watch for.
        String expectedId = RandomString.string();

        // Start listening for model publication events on the Hub.
        HubAccumulator outboundModelEventAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE,
                NameAndIdFilter.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, expectedId)
            );
        outboundModelEventAccumulator.clear().start();

        // Save Charley Crockett, a guy who has a blog, into the DataStore.
        BlogOwner localCharley = BlogOwner.builder()
            .name("Charley Crockett")
            .id(expectedId)
            .build();
        dataStore.save(localCharley);

        // Wait for a Hub event telling us that our Charley model got published to the cloud.
        outboundModelEventAccumulator.takeOne();
        outboundModelEventAccumulator.stop().clear();

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
     */
    @Test
    public void blogOwnerCreatedAndUpdatedRemotelyIsFoundLocally() throws DataStoreException {
        // Arrange a stable ID for the model we create/update,
        // so that we can match it in an event accumulator, below.
        String expectedId = RandomString.string();

        // Now, start watching the Hub for notifications that we received and processed models
        // from the Cloud. Look specifically for events relating to the model with the above ID.
        HubAccumulator inboundModelEventAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE,
                NameAndIdFilter.create(DataStoreChannelEventName.RECEIVED_FROM_CLOUD, expectedId)
            );
        inboundModelEventAccumulator.clear().start();

        // Act: externally in the Cloud, someone creates a record for a blog owner,
        // with a misspelling in the last name
        BlogOwner originalModel = BlogOwner.builder()
            .name("Jameson Willlllliams")
            .id(expectedId)
            .build();
        GraphQLResponse<ModelWithMetadata<BlogOwner>> createResponse = appSync.create(originalModel);
        ModelMetadata originalMetadata = createResponse.getData().getSyncMetadata();
        assertNotNull(originalMetadata.getVersion());
        int originalVersion = originalMetadata.getVersion();

        // A hub event tells us that a model was created in the cloud;
        // this model was synced into our local store.
        inboundModelEventAccumulator.takeOne();
        inboundModelEventAccumulator.stop().clear().start();

        // Act: externally, the record in the Cloud is updated, to correct the entry's last name
        BlogOwner updatedModel = originalModel.copyOfBuilder() // This uses the same model ID
            .name("Jameson Williams") // But with corrected name
            .build();
        GraphQLResponse<ModelWithMetadata<BlogOwner>> updateResponse =
            appSync.update(updatedModel, originalVersion);
        ModelMetadata newMetadata = updateResponse.getData().getSyncMetadata();
        assertNotNull(newMetadata.getVersion());
        int newVersion = newMetadata.getVersion();
        assertEquals(originalVersion + 1, newVersion);

        // Another HubEvent tells us that an update occurred in the Cloud;
        // the update was applied locally, to an existing record.
        inboundModelEventAccumulator.takeOne();
        inboundModelEventAccumulator.stop().clear();

        // Jameson should be in the local DataStore, and last name should be updated.
        BlogOwner localOwner = dataStore.get(BlogOwner.class, originalModel.getId());
        assertEquals("Jameson Williams", localOwner.getName());
    }

    /**
     * A {@link HubEventFilter} which will return true if the received event
     * has a matching DataStore event name, and refers to a given model ID.
     * For example,
     *     NameAndIdFilter.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, "asdf-asdfasdf")
     * Would match event with PUBLISHED_TO_CLOUD as the name, and "asdf-asdfasdf"
     * as the ID of the thing that was published to the cloud.
     */
    private static final class NameAndIdFilter implements HubEventFilter {
        private final DataStoreChannelEventName expectedEventName;
        private final String expectedId;

        NameAndIdFilter(DataStoreChannelEventName expectedEventName, String expectedId) {
            this.expectedEventName = expectedEventName;
            this.expectedId = expectedId;
        }

        static NameAndIdFilter create(DataStoreChannelEventName expectedEventName, String expectedId) {
            Objects.requireNonNull(expectedEventName);
            Objects.requireNonNull(expectedId);
            return new NameAndIdFilter(expectedEventName, expectedId);
        }

        @Override
        public boolean filter(@NonNull HubEvent<?> hubEvent) {
            final DataStoreChannelEventName actualEventName;
            try {
                actualEventName = DataStoreChannelEventName.fromString(hubEvent.getName());
            } catch (IllegalArgumentException noSuchValue) {
                return false;
            }
            if (!expectedEventName.equals(actualEventName)) {
                return false;
            }

            String actualId = null;
            switch (actualEventName) {
                case PUBLISHED_TO_CLOUD:
                    StorageItemChange<? extends Model> storageItemChange =
                        ((StorageItemChange<? extends Model>) hubEvent.getData());
                    if (storageItemChange != null) {
                        actualId = storageItemChange.item().getId();
                    }
                    break;
                case RECEIVED_FROM_CLOUD:
                    ModelWithMetadata<? extends Model> modelWithMetadata =
                        ((ModelWithMetadata<? extends Model>) hubEvent.getData());
                    if (modelWithMetadata != null) {
                        actualId = modelWithMetadata.getModel().getId();
                    }
                    break;
                default:
                    break;
            }

            if (expectedId.equals(actualId)) {
                LOG.info("Found a match for id=" + expectedId);
                return true;
            } else {
                return false;
            }
        }
    }
}
