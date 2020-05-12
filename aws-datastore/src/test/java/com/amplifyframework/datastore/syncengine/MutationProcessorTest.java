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
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
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

import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link MutationProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MutationProcessorTest {
    private static final PendingMutation.Converter RECORD_CONVERTER = new GsonPendingMutationConverter();

    private AppSync appSync;
    private SynchronousStorageAdapter storageAdapter;
    private MutationProcessor mutationProcessor;

    /**
     * Creates a {@link MutationProcessor} to test. This requires several dependencies:
     *  - A fake {@link AppSync} is used, to mock responses when mutations are posted.
     *  - A {@link SynchronousStorageAdapter} is created and held in reference, to facilitate
     *    arranging data into and out storage. (Storage is to house several different components).
     */
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
     * and will publish any items there-in to the {@link AppSync} instance.
     * @throws DataStoreException On failure to arrange items in to the MutationOutbox
     */
    @Test
    public void canDrainMutationOutbox() throws DataStoreException {
        // Start accumulating publication events for the records we expect to see.
        final List<PendingMutation<? extends Model>> expectedToPublish = Arrays.asList(
            Models.Tony.DELETION,
            Models.Joe.CREATION,
            Models.JoeBlog.CREATION
        );
        DataStoreChannelEventName eventName = DataStoreChannelEventName.PUBLISHED_TO_CLOUD;
        HubAccumulator publicationEventAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, eventName, expectedToPublish.size()).start();

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
            // There is a creation record for him, pending in the outbox, which still
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
            // Expected mutations
            expectedToPublish,
            // Look at the records we saw on the hub. Did we get all of them?
            Observable.fromIterable(publicationEventAccumulator.await())
                .map(HubEvent::getData)
                .toSortedList()
                .blockingGet()
        );

        // Finally, we expect the mutation outbox to be empty, now, that the mutation
        // processor has drained it.
        List<PendingMutation.PersistentRecord> pendingMutationRecordsStillInStorage =
            storageAdapter.query(PendingMutation.PersistentRecord.class);
        assertEquals(0, pendingMutationRecordsStillInStorage.size());
    }

    /**
     * Some arranged data that can be used as expected data, in test.
     */
    static final class Models {
        private Models() {}

        /**
         * Tony is a BlogOwner. Tony has been deleted locally, and the mutation processor
         * is supposed to pick up the deletion mutation and act on it. Before that happens,
         * the model metadata shows deleted == false, since it hasn't been updated, yet.
         * {@link Tony#MODEL} is not expected to be in the local storage, at the time
         * the mutation is processed.
         */
        static final class Tony {
            static final BlogOwner MODEL = BlogOwner.builder()
                .name("Tony")
                .wea(RandomString.string())
                .build();

            static final ModelMetadata MODEL_METADATA =
                new ModelMetadata(Tony.MODEL.getId(), false, 1, Time.now());

            static final PendingMutation<BlogOwner> DELETION =
                PendingMutation.deletion(Tony.MODEL, BlogOwner.class);

            static final PendingMutation.PersistentRecord DELETION_RECORD =
                 RECORD_CONVERTER.toRecord(DELETION);

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

            static final PendingMutation<BlogOwner> CREATION =
                PendingMutation.creation(Joe.MODEL, BlogOwner.class);

            static final PendingMutation.PersistentRecord CREATION_RECORD =
                RECORD_CONVERTER.toRecord(CREATION);

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

            static final PendingMutation<Blog> CREATION =
                PendingMutation.creation(JoeBlog.MODEL, Blog.class);

            static final PendingMutation.PersistentRecord CREATION_RECORD =
                RECORD_CONVERTER.toRecord(CREATION);

            private JoeBlog() {}
        }
    }
}
