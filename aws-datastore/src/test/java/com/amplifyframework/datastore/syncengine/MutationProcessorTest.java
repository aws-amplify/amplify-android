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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.Time;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link MutationProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MutationProcessorTest {
    private static final GsonStorageItemChangeConverter RECORD_CONVERTER = new GsonStorageItemChangeConverter();

    private AppSync appSync;
    private SynchronousStorageAdapter storageAdapter;
    private MutationProcessor mutationProcessor;

    @Before
    public void setup() {
        this.appSync = mock(AppSync.class);

        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);

        MutationOutbox mutationOutbox = new MutationOutbox(inMemoryStorageAdapter);
        Merger merger = new Merger(mutationOutbox, inMemoryStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(inMemoryStorageAdapter);
        this.mutationProcessor = new MutationProcessor(merger, versionRepository, mutationOutbox, appSync);
    }

    /**
     * Validates that the {@link MutationProcessor} will read form the {@link MutationOutbox},
     * and will publish any items there-in to the {@link AppSync}.
     * @throws DataStoreException On failure to arrange items in to the MutationOutbox
     */
    @Test
    public void canDrainMutationOutbox() throws DataStoreException {
        // These models should be processed, by the end of the test.
        // This is declared up top, so we can get the quantity for our accumulator to await
        final List<StorageItemChange.Record> changesWeExpectToProcessSuccessfully = Arrays.asList(
            Models.Tony.DELETION_RECORD,
            Models.Joe.CREATION_RECORD,
            Models.JoeBlog.CREATION_RECORD
        );
        int quantity = changesWeExpectToProcessSuccessfully.size();
        HubAccumulator publicationEventAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.PUBLISHED_TO_CLOUD, quantity);
        publicationEventAccumulator.start();

        // Arrange some local state, in the LocalStorageAdapter.
        // The "outbox" has three changes pending: 1 deletions, 2 creations.
        storageAdapter.save(
            // Tony has been deleted locally, so is not present. But, he still has
            // metadata, and there is a pending deletion record in the outbox, waiting
            // to be sync'd remotely.
            // Models.Tony.MODEL
            Models.Tony.MODEL_METADATA,
            Models.Tony.DELETION_RECORD,

            // Joe has been created locally, and is present in the local store.
            // There is a creation record for him, pending in the outbox, which
            // needs to be processed.
            Models.Joe.MODEL,
            Models.Joe.MODEL_METADATA,
            Models.Joe.CREATION_RECORD,

            // Joe's blog has also been created locally, and is present in the local store.
            // There is a creation record for him, pending in the ourbox, which still
            // needs to be processed.
            Models.JoeBlog.MODEL,
            Models.JoeBlog.MODEL_METADATA,
            Models.JoeBlog.CREATION_RECORD
        );

        // We expect that AppSync will be asked to create two models, and to delete one.
        // Mock successful responses for these invocations.
        AppSyncMocking.onCreate(appSync)
            .mockResponse(Models.Joe.MODEL)
            .mockResponse(Models.JoeBlog.MODEL);
        AppSyncMocking.onDelete(appSync)
            .mockResponse(Models.Tony.MODEL);

        // ACT! Try to start the mutation processor, to process the mutations stored above.
        mutationProcessor.startDrainingMutationOutbox();

        // Validate that we got "success" notifications out on Hub.
        assertEquals(
            // Expected change records
            changesWeExpectToProcessSuccessfully,
            // Look at the records we saw on the hub. Did we get all of them?
            takeRecordsFromAccumulator(publicationEventAccumulator)
        );
        publicationEventAccumulator.stop().clear();

        // Finally, we expect the mutation outbox to be empty, now, that the mutation
        // processor has drained it.
        assertEquals(0, storageAdapter.query(StorageItemChange.Record.class).size());
    }

    @SuppressWarnings("SameParameterValue")
    private List<StorageItemChange.Record> takeRecordsFromAccumulator(HubAccumulator accumulator) {
        final List<StorageItemChange.Record> changeRecords = new ArrayList<>();
        for (HubEvent<?> hubEvent : accumulator.takeAll()) {
            StorageItemChange<? extends Model> change = (StorageItemChange<? extends Model>) hubEvent.getData();
            if (change == null) {
                throw new IllegalStateException("Found null data in publication event: " + hubEvent);
            }
            StorageItemChange.Record record = change.toRecord(RECORD_CONVERTER);
            changeRecords.add(record);
        }
        return changeRecords;
    }

    /**
     * Some arranged data that can be used as expected data, in test.
     */
    static final class Models {
        private Models() {}

        /**
         * Tony is a BlogOwner. Tony has been deleted locally, and the mutation processor
         * is supposed to pick up the deletion change and act on it. Before that happens,
         * the model metadata shows deleted == false, since it hasn't been updated, yet.
         * {@link Tony#MODEL} is not expected to be in the local storage, at the time
         * the change is processed.
         */
        static final class Tony {
            static final BlogOwner MODEL = BlogOwner.builder()
                .name("Tony")
                .wea(RandomString.string())
                .build();

            static final ModelMetadata MODEL_METADATA =
                new ModelMetadata(Tony.MODEL.getId(), false, 1, Time.now());

            static final StorageItemChange.Record DELETION_RECORD = StorageItemChange.<BlogOwner>builder()
                .itemClass(BlogOwner.class)
                .item(Tony.MODEL)
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .type(StorageItemChange.Type.DELETE)
                .build()
                .toRecord(RECORD_CONVERTER);

            private Tony() {}
        }

        /**
         * Joe is present in the local store, and there is a creation event for him in the
         * mutation outbox, that still needs to be processed.
         */
        static final class Joe {
            static final BlogOwner MODEL = BlogOwner.builder()
                .name("Joe")
                .build();

            static final ModelMetadata MODEL_METADATA =
                new ModelMetadata(Joe.MODEL.getId(), false, 1, Time.now());

            static final StorageItemChange.Record CREATION_RECORD = StorageItemChange.<BlogOwner>builder()
                .itemClass(BlogOwner.class)
                .item(Joe.MODEL)
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .type(StorageItemChange.Type.CREATE)
                .build()
                .toRecord(RECORD_CONVERTER);

            private Joe() {}
        }

        /**
         * Joe also has a blog. Joe and his blog are expected to be found in the local
         * storage. Both Joe and his blog have items in the mutation outbox, waiting to be sync'd
         * with the cloud.
         */
        static final class JoeBlog {
            static final Blog MODEL = Blog.builder()
                .name("Joe's cool blog")
                .owner(Joe.MODEL)
                .build();

            static final ModelMetadata MODEL_METADATA =
                new ModelMetadata(JoeBlog.MODEL.getId(), false, 1, Time.now());

            static final StorageItemChange.Record CREATION_RECORD = StorageItemChange.<Blog>builder()
                .type(StorageItemChange.Type.CREATE)
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .itemClass(Blog.class)
                .item(JoeBlog.MODEL)
                .build()
                .toRecord(RECORD_CONVERTER);

            private JoeBlog() {}
        }
    }
}
