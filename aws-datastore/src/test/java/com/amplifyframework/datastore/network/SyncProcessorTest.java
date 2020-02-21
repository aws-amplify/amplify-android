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

package com.amplifyframework.datastore.network;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static com.amplifyframework.datastore.network.TestModelWithMetadataInstances.BLOGGER_ISLA;
import static com.amplifyframework.datastore.network.TestModelWithMetadataInstances.BLOGGER_JAMESON;
import static com.amplifyframework.datastore.network.TestModelWithMetadataInstances.DELETED_DRUM_POST;
import static com.amplifyframework.datastore.network.TestModelWithMetadataInstances.DRUM_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link SyncProcessor}.
 */
public final class SyncProcessorTest {
    private static final long OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    private StorageItemChange.StorageItemChangeFactory storageRecordDeserializer;
    private AppSyncEndpoint appSyncEndpoint;
    private InMemoryStorageAdapter inMemoryStorageAdapter;

    private SyncProcessor syncProcessor;

    /**
     * Wire up dependencies for the SyncProcessor, and build one for testing.
     */
    @Before
    public void setup() {
        this.storageRecordDeserializer = new GsonStorageItemChangeConverter();
        this.inMemoryStorageAdapter = InMemoryStorageAdapter.create();

        final ModelProvider modelProvider = ModelProviderFactory.including(Post.class, BlogOwner.class);
        this.appSyncEndpoint = mock(AppSyncEndpoint.class);
        final RemoteModelState remoteModelState = new RemoteModelState(appSyncEndpoint, modelProvider);

        this.syncProcessor = new SyncProcessor(remoteModelState, inMemoryStorageAdapter);
    }

    /**
     * When {@link SyncProcessor#hydrate()}'s {@link Completable} completes,
     * then the local storage adapter should have all of the remote model state.
     */
    @SuppressWarnings({"unchecked", "checkstyle:MagicNumber"})
    @Test
    public void localStorageAdapterIsHydratedFromRemoteModelState() {
        // Arrange: drum post is already in the adapter before hydration.
        inMemoryStorageAdapter.items().add(DRUM_POST.getModel());

        // Arrange a subscription to the storage adapter. We're going to watch for changes.
        // We expect to see content here as a result of the SyncProcessor applying updates.
        final TestObserver<StorageItemChange<? extends Model>> adapterObserver = TestObserver.create();
        Observable.<StorageItemChange.Record>create(
            emitter ->
                inMemoryStorageAdapter.observe(emitter::onNext, emitter::onError, emitter::onComplete)
            )
            .map(record -> record.toStorageItemChange(storageRecordDeserializer))
            .subscribe(adapterObserver);

        // Arrange: return some responses for the sync() call on the RemoteModelState
        MockAppSyncEndpoint.configure(appSyncEndpoint)
            .mockSuccessResponse(Post.class, DELETED_DRUM_POST)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_ISLA, BLOGGER_JAMESON);

        // Act: Call hydrate, and await its completion - assert it completed without error
        TestObserver<ModelWithMetadata<? extends Model>> hydrationObserver = TestObserver.create();
        syncProcessor.hydrate().subscribe(hydrationObserver);
        assertTrue(hydrationObserver.awaitTerminalEvent(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Since hydrate() completed, the storage adapter observer should see some values.
        // There should be a total of six changes on storage adapter
        // A model and a metadata save for each of the two BlogOwner-type items,
        // and a model deletion and a metadata save for the deleted post about drums.
        adapterObserver.awaitCount(6);

        // Validate the changes emitted from the storage adapter's observe().
        assertEquals(
            // Expect 6 items as described above.
            Observable.fromArray(DELETED_DRUM_POST, BLOGGER_JAMESON, BLOGGER_ISLA)
                // flatten each item into two items, the item's model, and the item's metadata
                .flatMap(modelWithMutation ->
                    Observable.fromArray(modelWithMutation.getModel(), modelWithMutation.getSyncMetadata()))
                // Get the items into a Single<List>, where the list is sorted by model ID
                .toSortedList(SortByModelId::compare)
                // Resolve the single into a success/error
                .blockingGet(),
            // Actually...
            Observable.fromIterable(adapterObserver.values())
                .map(StorageItemChange::item)
                .toSortedList(SortByModelId::compare)
                .blockingGet()
        );

        // Lastly: validate the current contents of the storage adapter.
        // There should be 2 BlogOwners, 0 Posts, and 3 MetaData records - two for the BlogOwner,
        // and 1 for the deleted drum post
        assertEquals(5, inMemoryStorageAdapter.items().size());
        assertEquals(
            // Expect the 4 items for the bloggers (2 models and their metadata)
            Observable.fromArray(BLOGGER_ISLA, BLOGGER_JAMESON)
                .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                // And also the one metadata record for a deleted post
                .startWith(DELETED_DRUM_POST.getSyncMetadata())
                .toSortedList(SortByModelId::compare)
                .blockingGet(),
            Observable.fromIterable(inMemoryStorageAdapter.items())
                .toSortedList(SortByModelId::compare)
                .blockingGet()
        );

        adapterObserver.dispose();
        hydrationObserver.dispose();
    }

    static final class SortByModelId {
        @SuppressWarnings("checkstyle:all") private SortByModelId() {}

        static int compare(Model left, Model right) {
            return left.getId().compareTo(right.getId());
        }
    }
}
