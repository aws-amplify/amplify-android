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

package com.amplifyframework.datastore.syncengine;

import android.util.Range;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.util.Time;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_ISLA;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_JAMESON;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DELETED_DRUM_POST;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DRUM_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link SyncProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class SyncProcessorTest {
    private static final long OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static final long BASE_SYNC_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);

    private AppSync appSync;
    private ModelProvider modelProvider;
    private SynchronousStorageAdapter storageAdapter;

    private SyncProcessor syncProcessor;
    private int errorHandlerCallCount;

    /**
     * Wire up dependencies for the SyncProcessor, and build one for testing.
     * @throws AmplifyException On failure to load models into registry
     */
    @Before
    public void setup() throws AmplifyException {
        this.modelProvider =
            CompoundModelProvider.of(SystemModelsProviderFactory.create(), AmplifyModelProvider.getInstance());

        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        this.appSync = mock(AppSync.class);
        this.errorHandlerCallCount = 0;
        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);

        final SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(inMemoryStorageAdapter);
        final MutationOutbox mutationOutbox = new PersistentMutationOutbox(inMemoryStorageAdapter);
        final VersionRepository versionRepository = new VersionRepository(inMemoryStorageAdapter);
        final Merger merger = new Merger(mutationOutbox, versionRepository, inMemoryStorageAdapter);

        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration
            .builder()
            .syncIntervalInMinutes(BASE_SYNC_INTERVAL_MINUTES)
            .dataStoreErrorHandler(dataStoreException -> errorHandlerCallCount++)
            .build();

        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .modelSchemaRegistry(modelSchemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(() -> dataStoreConfiguration)
            .build();
    }

    /**
     * When {@link SyncProcessor#hydrate()}'s {@link Completable} completes,
     * then the local storage adapter should have all of the remote model state.
     * @throws DataStoreException On failure interacting with storage adapter
     */
    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    @Test
    public void localStorageAdapterIsHydratedFromRemoteModelState() throws DataStoreException {
        // Arrange: drum post is already in the adapter before hydration.
        storageAdapter.save(DRUM_POST.getModel());

        // Arrange a subscription to the storage adapter. We're going to watch for changes.
        // We expect to see content here as a result of the SyncProcessor applying updates.
        final TestObserver<StorageItemChange<? extends Model>> adapterObserver = storageAdapter.observe().test();
        // Arrange: return some responses for the sync() call on the RemoteModelState
        AppSyncMocking.sync(appSync)
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
        // Additionally, there should be 6 last sync time records, one for each of the
        // models managed by the system.
        adapterObserver.awaitCount(6 + 6);

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
                // Ignore the sync time records for a moment.
                .map(StorageItemChange::item)
                .filter(item -> !LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                .toSortedList(SortByModelId::compare)
                .blockingGet()
        );

        // Lastly: validate the current contents of the storage adapter.
        // There should be 2 BlogOwners, 0 Posts, and 3 MetaData records:
        //   - two for the BlogOwner,
        //   - and 1 for the deleted drum post
        List<? extends Model> itemsInStorage = storageAdapter.query(modelProvider);
        assertEquals(
            itemsInStorage.toString(),
            2 + 3 + modelProvider.models().size(),
            itemsInStorage.size()
        );
        assertEquals(
            // Expect the 4 items for the bloggers (2 models and their metadata)
            Observable.fromArray(BLOGGER_ISLA, BLOGGER_JAMESON)
                .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                // And also the one metadata record for a deleted post
                .startWith(DELETED_DRUM_POST.getSyncMetadata())
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(storageAdapter.query(modelProvider))
                .filter(item -> !LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );

        // Assert that there is a list sync time for every model managed by the system.
        assertEquals(
            Observable.fromIterable(modelProvider.models())
                .map(Class::getSimpleName)
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(storageAdapter.query(modelProvider))
                .filter(item -> LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                .map(item -> (LastSyncMetadata) item)
                .map(LastSyncMetadata::getModelClassName)
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );

        adapterObserver.dispose();
        hydrationObserver.dispose();
    }

    /**
     * Suppose that the remote AppSync endpoint has a deleted record, and tells the client to
     * delete a record. But suppose the client is sync'ing for the first time. So, the client
     * doesn't actually need to delete this record. The deletion attempt will fail. But, that's
     * okay. Generally speaking, we want to do a "best effort" to apply the remote updates.
     * @throws DataStoreException On failure to query items in storage
     */
    @Test
    public void hydrationContinuesEvenIfOneItemFailsToSync() throws DataStoreException {
        // The local store does NOT have the drum post to start (or, for that matter, any items.)
        // inMemoryStorageAdapter.items().add(DRUM_POST.getModel());

        // Arrange some responses from AppSync
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(Post.class, DELETED_DRUM_POST)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the local store.
        TestObserver<Void> hydrationObserver = syncProcessor.hydrate().test();

        // Assert that hydration completed without error.
        assertTrue(hydrationObserver.awaitTerminalEvent(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Local storage should have a metadata record for the deletion, but no entry for the drum post. +1
        // There should be an entry and a metadata for Jameson the BlogOwner. +2.
        // Plus an entry for last sync time for every model type.
        List<? extends Model> storageItems = storageAdapter.query(modelProvider);
        assertEquals(
            storageItems.toString(),
            1 + 2 + modelProvider.models().size(),
            storageItems.size()
        );

        // Consider the model instances and their metadata.
        assertEquals(
            // Expect a metadata for Jameson & deleted drum post, and an entry for Jameson
            Observable.just(BLOGGER_JAMESON)
                .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                .startWith(DELETED_DRUM_POST.getSyncMetadata())
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(storageItems)
                // Ignore the sync time records, for this check.
                .filter(item -> !LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );

        // Now, find the sync time records.
        Observable<LastSyncMetadata> actualSyncTimeRecords = Observable.fromIterable(storageItems)
            .filter(item -> LastSyncMetadata.class.isAssignableFrom(item.getClass()))
            .map(model -> (LastSyncMetadata) model)
            .sorted(SortByModelId::compare)
            .toSortedList(SortByModelId::compare)
            .flatMapObservable(Observable::fromIterable);

        assertEquals(
            modelProvider.models().size(),
            actualSyncTimeRecords.toList()
                .blockingGet()
                .size()
        );

        // Make sure that all of the system provided models have sync time records in storage, now.
        assertTrue(actualSyncTimeRecords
            .map(LastSyncMetadata::getModelClassName)
            .toList() // Get a list of the actual records,
            .map(HashSet::new) // Put them into a hash set.
            .blockingGet()
            .containsAll(Observable.fromIterable(modelProvider.models())
                .map(Class::getSimpleName)
                .toList()
                .map(HashSet::new)
                .blockingGet()
            )
        );

        actualSyncTimeRecords.blockingForEach(record ->
            assertTrue(RecentTimeWindow.contains(record.getLastSyncTime()))
        );

        // Cleanup!
        hydrationObserver.dispose();
    }

    /**
     * When a sync is requested, the last sync time should be considered.
     * If the last sync time is before (nowMs - baseSyncIntervalMs), then a base
     * sync will be performed.
     */
    @Test
    public void baseSyncRequestedIfLastSyncBeyondInterval() {
        // Arrange: add LastSyncMetadata for the types, indicating that they
        // were sync'd too long ago. That is, longer ago than the base sync interval.
        long longAgoTimeMs = Time.now() - (TimeUnit.MINUTES.toMillis(BASE_SYNC_INTERVAL_MINUTES) * 2);
        Observable.fromIterable(modelProvider.models())
            .map(modelClass -> LastSyncMetadata.lastSyncedAt(modelClass, longAgoTimeMs))
            .blockingForEach(storageAdapter::save);

        // Arrange: return some content from the fake AppSync endpoint
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the store.
        assertTrue(syncProcessor.hydrate().blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert: the sync time that was passed to AppSync should have been `null`.
        ArgumentCaptor<Long> lastSyncTimeCaptor = ArgumentCaptor.forClass(Long.class);
        int modelClassCount = modelProvider.models().size();
        verify(appSync, times(modelClassCount)).sync(
            any(),
            lastSyncTimeCaptor.capture(),
            any(),
            any()
        );
        List<Long> capturedValues = lastSyncTimeCaptor.getAllValues();
        assertEquals(modelClassCount, capturedValues.size());
        for (Long capturedValue : capturedValues) {
            assertNull(capturedValue);
        }
    }

    /**
     * When a sync is requested, the last sync time should be considered.
     * If the last sync time is after (nowMs - baseSyncIntervalMs) - that is,
     * if the last sync time is within the base sync interval, then a DELTA sync
     * will be performed.
     */
    @Test
    public void deltaSyncRequestedIfLastSyncIsRecent() {
        // Arrange: add LastSyncMetadata for the types, indicating that they
        // were sync'd very recently (within the interval.)
        long recentTimeMs = Time.now();
        Observable.fromIterable(modelProvider.models())
            .map(modelClass -> LastSyncMetadata.lastSyncedAt(modelClass, recentTimeMs))
            .blockingForEach(storageAdapter::save);

        // Arrange: return some content from the fake AppSync endpoint
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the store.
        assertTrue(syncProcessor.hydrate().blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert: the sync time that was passed to AppSync should have been `null`.
        ArgumentCaptor<Long> lastSyncTimeCaptor = ArgumentCaptor.forClass(Long.class);
        int modelClassCount = modelProvider.models().size();
        verify(appSync, times(modelClassCount)).sync(
            any(),
            lastSyncTimeCaptor.capture(),
            any(),
            any()
        );
        final List<Long> capturedValues = lastSyncTimeCaptor.getAllValues();
        assertEquals(modelClassCount, capturedValues.size());
        for (long capturedValue : capturedValues) {
            assertEquals(recentTimeMs, capturedValue);
        }
    }

    /**
     * Verify that the user-provided onError callback (if specified) is invoked if initial sync fails.
     */
    @Test
    public void userProvidedErrorCallbackInvokedOnFailure() {
        // Arrange: mock failure when invoking hydrate on the mock object.
        AppSyncMocking.sync(appSync)
            .mockFailure(new DataStoreException("Something timed out during sync.", "Nothing to do."));

        // Act: call hydrate.
        assertTrue(
            syncProcessor.hydrate()
                .onErrorComplete()
                .blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        );

        // Assert: sync process failed the first time the api threw an error
        assertEquals(1, errorHandlerCallCount);
    }

    static final class RecentTimeWindow {
        private static final long ACCEPTABLE_DRIFT_MS = TimeUnit.SECONDS.toMillis(1);

        private RecentTimeWindow() {}

        /**
         * Checks if a timestamp refers to "recent" time.
         * @param millisSinceEpoch A timestamp, in milliseconds since UNIX epoch
         * @return True if this was a recent event, false otherwise
         */
        static boolean contains(long millisSinceEpoch) {
            long low = Time.now() - ACCEPTABLE_DRIFT_MS;
            long high = Time.now() + ACCEPTABLE_DRIFT_MS;
            return Range.create(low, high).contains(millisSinceEpoch);
        }
    }

    /**
     * Utilities for sorting collections of models, by their IDs.
     */
    public static final class SortByModelId {
        private SortByModelId() {}

        /**
         * A comparator implementation to sort models by ID.
         * @param left A model
         * @param right Another model
         * @return negative if left is first, positive if right is; zero if equivalent
         */
        static int compare(Model left, Model right) {
            return left.getId().compareTo(right.getId());
        }
    }
}
