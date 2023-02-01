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
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.events.ModelSyncedEvent;
import com.amplifyframework.datastore.events.SyncQueriesStartedEvent;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Author;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.ForEach;
import com.amplifyframework.util.Time;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_ISLA;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_JAMESON;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DELETED_DRUM_POST;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DRUM_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link SyncProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class SyncProcessorTest {
    private static final long OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static final long BASE_SYNC_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);
    private static final List<String> SYSTEM_MODEL_NAMES =
            ForEach.inCollection(SystemModelsProviderFactory.create().models(), Class::getSimpleName);

    private AppSync appSync;
    private ModelProvider modelProvider;
    private SynchronousStorageAdapter storageAdapter;

    private SyncProcessor syncProcessor;
    private int errorHandlerCallCount;
    private int modelCount;
    private RetryHandler requestRetry;
    private boolean isSyncRetryEnabled = true;

    /**
     * Wire up dependencies for the SyncProcessor, and build one for testing.
     * @throws AmplifyException On failure to load models into registry
     */
    @Before
    public void setup() throws AmplifyException {
        final LinkedHashSet<Class<? extends Model>> modelClasses = new LinkedHashSet<>();
        modelClasses.addAll(SystemModelsProviderFactory.create().models());
        modelClasses.addAll(AmplifyModelProvider.getInstance().models());
        this.modelProvider = SimpleModelProvider.instance(UUID.randomUUID().toString(), modelClasses);
        modelCount = modelProvider.models().size();

        this.appSync = mock(AppSync.class);
        this.errorHandlerCallCount = 0;
        this.requestRetry = new RetryHandler();

        initSyncProcessor(10_000);
    }

    private void initSyncProcessor(int syncMaxRecords) throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.clear();
        schemaRegistry.register(modelProvider.models());

        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);

        final SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(inMemoryStorageAdapter);
        final MutationOutbox mutationOutbox = new PersistentMutationOutbox(inMemoryStorageAdapter);
        final VersionRepository versionRepository = new VersionRepository(inMemoryStorageAdapter);
        final Merger merger = new Merger(mutationOutbox, versionRepository, inMemoryStorageAdapter);

        DataStoreConfigurationProvider dataStoreConfigurationProvider = () -> DataStoreConfiguration
                .builder()
                .syncInterval(BASE_SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .syncMaxRecords(syncMaxRecords)
                .syncPageSize(1_000)
                .errorHandler(dataStoreException -> errorHandlerCallCount++)
                .syncExpression(BlogOwner.class, () -> BlogOwner.NAME.beginsWith("J"))
                .syncExpression(Author.class, QueryPredicates::none)
                .build();

        QueryPredicateProvider queryPredicateProvider = new QueryPredicateProvider(dataStoreConfigurationProvider);
        queryPredicateProvider.resolvePredicates();

        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .schemaRegistry(schemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .queryPredicateProvider(queryPredicateProvider)
            .retryHandler(requestRetry)
            .isSyncRetryEnabled(isSyncRetryEnabled)
            .build();
    }

    /**
     * During a base sync, there are a series of events that should be emitted.
     * This test verifies that these events are published via Amplify Hub depending
     * on actions takes for each available model.
     * @throws DataStoreException Not expected.
     * @throws InterruptedException Not expected.
     */
    @Test
    public void dataStoreHubEventsTriggered() throws DataStoreException, InterruptedException {
        // Arrange - BEGIN
        int expectedModelCount = Arrays.asList(Post.class, BlogOwner.class).size();
        // Collects one syncQueriesStarted event.
        //The count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        HubAccumulator syncStartAccumulator =
            createAccumulator(syncQueryStartedForModels(modelCount - 1), 1);
        // Collects one syncQueriesReady event.
        HubAccumulator syncQueryReadyAccumulator =
            createAccumulator(forEvent(DataStoreChannelEventName.SYNC_QUERIES_READY), 1);
        // Collects one modelSynced event for each model.
        HubAccumulator modelSyncedAccumulator =
            createAccumulator(forEvent(DataStoreChannelEventName.MODEL_SYNCED), expectedModelCount);

        // Add a couple of seed records so they can be deleted/updated.
        storageAdapter.save(DRUM_POST.getModel());
        storageAdapter.save(BLOGGER_ISLA.getModel());

        // Mock sync query results for a couple of models.
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(Post.class, DELETED_DRUM_POST)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_ISLA, BLOGGER_JAMESON);

        // Start the accumulators.
        syncQueryReadyAccumulator.start();
        syncStartAccumulator.start();
        modelSyncedAccumulator.start();

        TestObserver<ModelWithMetadata<? extends Model>> hydrationObserver = TestObserver.create();
        // Arrange - END

        // Act: kickoff sync.
        syncProcessor.hydrate().subscribe(hydrationObserver);

        // Check - BEGIN
        // Verify that sync completes.
        assertTrue(hydrationObserver.await(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Verify that syncQueriesStarted was emitted once.
        assertEquals(1, syncStartAccumulator.await((int) OP_TIMEOUT_MS, TimeUnit.MILLISECONDS).size());
        // Verify that syncQueriesReady was emitted once.
        assertEquals(1, syncQueryReadyAccumulator.await((int) OP_TIMEOUT_MS, TimeUnit.MILLISECONDS).size());

        // Get the list of modelSynced events captured.
        List<HubEvent<?>> hubEvents = modelSyncedAccumulator.await((int) OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Verify that [number of events] = [number of models]
        assertEquals(expectedModelCount, hubEvents.size());

        ModelSyncedEvent expectedBlogOwnerCounts = new ModelSyncedEvent("BlogOwner", true, 1, 1, 0);
        ModelSyncedEvent expectedPostCounts = new ModelSyncedEvent("Post", true, 0, 0, 1);

        // For each event (excluding system models), verify the desired count.
        for (HubEvent<?> event : hubEvents) {
            ModelSyncedEvent eventData = (ModelSyncedEvent) event.getData();
            assertTrue(eventData.isFullSync());
            assertFalse(eventData.isDeltaSync());
            String eventModel = eventData.getModel();
            switch (eventModel) {
                case "BlogOwner":
                    // One BlogOwner added and one updated.
                    assertEquals(expectedBlogOwnerCounts, eventData);
                    break;
                case "Post":
                    // One post deleted.
                    assertEquals(expectedPostCounts, eventData);
                    break;
                default:
                    // Exclude system models
                    if (!SYSTEM_MODEL_NAMES.contains(eventModel)) {
                        ModelSyncedEvent otherCounts = new ModelSyncedEvent(eventModel, true, 0, 0, 0);
                        assertEquals(otherCounts, eventData);
                    }
            }
        }
        // Check - END
    }

    /**
     * When {@link SyncProcessor#hydrate()}'s {@link Completable} completes,
     * then the local storage adapter should have all of the remote model state.
     * @throws AmplifyException On failure interacting with storage adapter
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @SuppressWarnings("unchecked") // Varied types in Observable.fromArray(...).
    @Test
    public void localStorageAdapterIsHydratedFromRemoteModelState() throws AmplifyException, InterruptedException {
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
        assertTrue(hydrationObserver.await(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
        //The count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        assertEquals(
            itemsInStorage.toString(),
            2 + 3 + modelProvider.models().size() - 1,
            itemsInStorage.size()
        );
        assertEquals(
            // Expect the 4 items for the bloggers (2 models and their metadata)
            Observable.fromArray(BLOGGER_ISLA, BLOGGER_JAMESON)
                .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                // And also the one metadata record for a deleted post
                .startWithItem(DELETED_DRUM_POST.getSyncMetadata())
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(storageAdapter.query(modelProvider))
                .filter(item -> !LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );

        //The models to be synced should be one less than total model because Author model has sync expression =
        // QueryPredicates.none()
        Iterator<Class<? extends Model>> modelIterator = modelProvider.models().iterator();
        Set<Class<? extends Model>> modelsToBeSynced = new HashSet<>();
        while (modelIterator.hasNext()) {
            Class<? extends Model> model = modelIterator.next();
            if (model != Author.class) {
                modelsToBeSynced.add(model);
            }
        }

        // Assert that there is a list sync time for every model managed by the system.
        assertEquals(
            Observable.fromIterable(modelsToBeSynced)
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
     * @throws AmplifyException On failure to query items in storage
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void hydrationContinuesEvenIfOneItemFailsToSync() throws AmplifyException, InterruptedException {
        // The local store does NOT have the drum post to start (or, for that matter, any items.)
        // inMemoryStorageAdapter.items().add(DRUM_POST.getModel());

        // Arrange some responses from AppSync
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(Post.class, DELETED_DRUM_POST)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the local store.
        TestObserver<Void> hydrationObserver = syncProcessor.hydrate().test();

        // Assert that hydration completed without error.
        assertTrue(hydrationObserver.await(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Local storage should have a metadata record for the deletion, but no entry for the drum post. +1
        // There should be an entry and a metadata for Jameson the BlogOwner. +2.
        // Plus an entry for last sync time for every model type.
        List<? extends Model> storageItems = storageAdapter.query(modelProvider);

        //The count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        assertEquals(
            storageItems.toString(),
            1 + 2 + (modelProvider.models().size() - 1),
            storageItems.size()
        );

        // Consider the model instances and their metadata.
        assertEquals(
            // Expect a metadata for Jameson & deleted drum post, and an entry for Jameson
            Observable.just(BLOGGER_JAMESON)
                .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                .startWithItem(DELETED_DRUM_POST.getSyncMetadata())
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
        //The count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        Iterator<Class<? extends Model>> modelIterator = modelProvider.models().iterator();
        Set<Class<? extends Model>> modelsToBeSynced = new HashSet<>();
        while (modelIterator.hasNext()) {
            Class<? extends Model> model = modelIterator.next();
            if (model != Author.class) {
                modelsToBeSynced.add(model);
            }
        }

        assertEquals(
            modelProvider.models().size() - 1,
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
            .containsAll(Observable.fromIterable(modelsToBeSynced)
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
     * @throws AmplifyException On failure to build GraphQLRequest for sync query
     */
    @Test
    public void baseSyncRequestedIfLastSyncBeyondInterval() throws AmplifyException {
        // Arrange: add LastSyncMetadata for the types, indicating that they
        // were sync'd too long ago. That is, longer ago than the base sync interval.
        long longAgoTimeMs = Time.now() - (TimeUnit.MINUTES.toMillis(BASE_SYNC_INTERVAL_MINUTES) * 2);
        Observable.fromIterable(modelProvider.modelNames())
            .map(modelName -> LastSyncMetadata.baseSyncedAt(modelName, longAgoTimeMs))
            .blockingForEach(storageAdapter::save);

        // Arrange: return some content from the fake AppSync endpoint
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the store.
        assertTrue(syncProcessor.hydrate().blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert: the sync time that was passed to AppSync should have been `null`.
        //The class count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        int modelClassCount = modelProvider.models().size() - 1;
        @SuppressWarnings("unchecked") // ignore GraphQLRequest.class not being a parameterized type.
        ArgumentCaptor<GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>>> requestCaptor =
                ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(appSync, times(modelClassCount)).sync(
            requestCaptor.capture(),
            any(),
            any()
        );
        List<GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>>> capturedValues =
                requestCaptor.getAllValues();
        assertEquals(modelClassCount, capturedValues.size());
        for (GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> capturedValue : capturedValues) {
            assertNull(capturedValue.getVariables().get("lastSync"));
        }
    }

    /**
     * When a sync is requested, the last sync time should be considered.
     * If the last sync time is after (nowMs - baseSyncIntervalMs) - that is,
     * if the last sync time is within the base sync interval, then a DELTA sync
     * will be performed.
     * @throws AmplifyException On failure to build GraphQLRequest for sync query
     */
    @Test
    public void deltaSyncRequestedIfLastSyncIsRecent() throws AmplifyException {
        // Arrange: add LastSyncMetadata for the types, indicating that they
        // were sync'd very recently (within the interval.)
        long recentTimeMs = Time.now();
        Observable.fromIterable(modelProvider.modelNames())
            .map(modelName -> LastSyncMetadata.deltaSyncedAt(modelName, recentTimeMs))
            .blockingForEach(storageAdapter::save);

        // Arrange: return some content from the fake AppSync endpoint
        AppSyncMocking.sync(appSync)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the store.
        assertTrue(syncProcessor.hydrate().blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert: the sync time that was passed to AppSync should have been `null`.
        //The class count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        int modelClassCount = modelProvider.models().size() - 1;
        @SuppressWarnings("unchecked") // ignore GraphQLRequest.class not being a parameterized type.
        ArgumentCaptor<GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>>> requestCaptor =
                ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(appSync, times(modelClassCount)).sync(
            requestCaptor.capture(),
            any(),
            any()
        );
        final List<GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>>> capturedValues =
                requestCaptor.getAllValues();
        assertEquals(modelClassCount, capturedValues.size());
        for (GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> capturedValue : capturedValues) {
            assertEquals(recentTimeMs, capturedValue.getVariables().get("lastSync"));
        }
    }

    /**
     * Verify that the syncExpressions from the DataStoreConfiguration are applied to the sync request.
     * @throws AmplifyException On failure interacting with storage adapter
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void syncExpressionsAppliedOnSyncRequest() throws AmplifyException, InterruptedException {
        // Arrange some responses from AppSync
        AppSyncMocking.sync(appSync)
                .mockSuccessResponse(Post.class, DELETED_DRUM_POST)
                .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON);

        // Act: hydrate the local store.
        TestObserver<Void> hydrationObserver = syncProcessor.hydrate().test();

        // Assert that hydration completed without error.
        assertTrue(hydrationObserver.await(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Mock the AppSync interface, and verify that it gets calls with the expected predicate
        ModelSchema blogOwnerSchema = ModelSchema.fromModelClass(BlogOwner.class);
        verify(appSync, times(1)).buildSyncRequest(blogOwnerSchema, null, 1_000, BlogOwner.NAME.beginsWith("J"));
    }

    /**
     * Verify that the user-provided onError callback (if specified) is invoked if initial sync fails.
     * @throws DataStoreException On failure to build GraphQLRequest for sync query.
     */
    @Test
    public void userProvidedErrorCallbackInvokedOnFailure() throws DataStoreException {
        // Arrange: mock failure when invoking hydrate on the mock object.
        AppSyncMocking.sync(appSync)
            .mockFailure(new DataStoreException
                    .IrRecoverableException("Something timed out during sync.", "This was intentional"));

        // Act: call hydrate.
        assertTrue(
                syncProcessor.hydrate()
                        .onErrorComplete()
                        .blockingAwait(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        );

        // Assert: sync process failed the first time the api threw an error
        assertEquals(1, errorHandlerCallCount);
    }


    /**
     * Verify that retry is called on appsync failure when syncRetry is set to true.
     *
     * @throws AmplifyException On failure to build GraphQLRequest for sync query.
     */
    @Test
    public void retriedOnAppSyncFailure() throws AmplifyException {
        // Arrange: mock failure when invoking hydrate on the mock object.
        requestRetry = mock(RetryHandler.class);
        when(requestRetry.retry(any(), any())).thenReturn(Single.error(
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "")));

        initSyncProcessor(10_000);
        AppSyncMocking.sync(appSync)
                .mockFailure(new DataStoreException("Something timed out during sync.", ""));

        // Act: call hydrate.
        syncProcessor.hydrate()
                .test(false)
                .assertNotComplete();
        verify(requestRetry, times(1)).retry(any(), any());

    }

    /**
     * Verify that retry is called on appsync failure when syncRetry is set to true.
     *
     * @throws AmplifyException On failure to build GraphQLRequest for sync query.
     */
    @Test
    public void shouldNotRetryOnAppSyncFailureWhenSynRetryIsSetToFalse() throws AmplifyException {
        // Arrange: mock failure when invoking hydrate on the mock object.
        requestRetry = mock(RetryHandler.class);
        isSyncRetryEnabled = false;
        when(requestRetry.retry(any(), any())).thenReturn(Single.error(
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "")));

        initSyncProcessor(10_000);
        AppSyncMocking.sync(appSync)
                .mockFailure(new DataStoreException("Something timed out during sync.", ""));

        // Act: call hydrate.
        syncProcessor.hydrate()
                .test(false)
                .assertNotComplete();
        verify(requestRetry, times(0)).retry(any(), any());

    }

    /**
     * Verify that retry is called on appsync failure and when dispose in called midway no exception is thrown.
     *
     * @throws AmplifyException On failure to build GraphQLRequest for sync query.
     */
    @Test
    public void retryHandlesHydrateSubscriptionDispose() throws AmplifyException {
        // Arrange: mock failure when invoking hydrate
        requestRetry = spy(RetryHandler.class);
        initSyncProcessor(10_000);
        AppSyncMocking.sync(appSync)
                .mockFailure(new DataStoreException("Something timed out during sync.", ""));

        // Act: call hydrate.
        TestObserver<Void> testObserver = syncProcessor.hydrate()
                .test(false);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                testObserver.dispose();
            }
        }, 10000);
        verify(requestRetry, times(1)).retry(any(), any());
    }

    /**
     * Validate that all records are synced, via pagination.
     * @throws AmplifyException on error building sync request for next page.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void modelWithMultiplePagesSyncsAllPages() throws AmplifyException, InterruptedException {
        syncAndExpect(5, 10);
    }

    /**
     * Validate that sync stops after retrieving syncMaxRecords results, even if there are more pages available.
     * @throws AmplifyException on error building sync request for next page.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void syncStopsAfterMaxRecords() throws AmplifyException, InterruptedException {
        syncAndExpect(10, 5);
    }

    /**
     * Validate the sync can handle 100 of pages.  Even with a recursive, functional algorithm, this should pass.
     * @throws AmplifyException on error building sync request for next page.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void syncCanHandle100Pages() throws AmplifyException, InterruptedException {
        syncAndExpect(100, 10000);
    }

    /**
     * Validate that sync can handle 1000 more pages.  This fails with a StackOverflowError if sync is implemented
     * recursively, because each call to the sync method is saved on the stack before execution
     * begins.  The solution is to use an iterative algorithm.
     * @throws AmplifyException on error building sync request for next page.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void syncCanHandle1000Pages() throws AmplifyException, InterruptedException {
        syncAndExpect(1000, 10000);
    }

    private void syncAndExpect(int numPages, int maxSyncRecords) throws AmplifyException, InterruptedException {
        initSyncProcessor(maxSyncRecords);
        // Arrange a subscription to the storage adapter. We're going to watch for changes.
        // We expect to see content here as a result of the SyncProcessor applying updates.
        final TestObserver<StorageItemChange<? extends Model>> adapterObserver = storageAdapter.observe().test();

        // Arrange: return some responses for the sync() call on the RemoteModelState
        AppSyncMocking.SyncConfigurator configurator = AppSyncMocking.sync(appSync);
        List<ModelWithMetadata<BlogOwner>> expectedResponseItems = new ArrayList<>();

        String token = null;
        for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
            String nextToken = pageIndex < numPages - 1 ? RandomString.string() : null;
            ModelWithMetadata<BlogOwner> randomBlogOwner = randomBlogOwnerWithMetadata();
            configurator.mockSuccessResponse(BlogOwner.class, token, nextToken, randomBlogOwner);
            if (expectedResponseItems.size() < maxSyncRecords) {
                expectedResponseItems.add(randomBlogOwner);
            }
            token = nextToken;
        }

        // Act: Call hydrate, and await its completion - assert it completed without error
        TestObserver<ModelWithMetadata<? extends Model>> hydrationObserver = TestObserver.create();
        syncProcessor.hydrate().subscribe(hydrationObserver);

        // Wait 2 seconds, or 1 second per 100 pages, whichever is greater
        long timeoutMs = Math.max(OP_TIMEOUT_MS, TimeUnit.SECONDS.toMillis(numPages / 100));
        assertTrue(hydrationObserver.await(timeoutMs, TimeUnit.MILLISECONDS));
        hydrationObserver.assertNoErrors();
        hydrationObserver.assertComplete();

        // Since hydrate() completed, the storage adapter observer should see some values.
        // There should be a total of four changes on storage adapter
        // A model and a metadata save for each of the two BlogOwner-type items
        // Additionally, there should be 4 last sync time records, one for each of the
        // models managed by the system.
        adapterObserver.awaitCount(expectedResponseItems.size() * 2 + 4);

        // Validate the changes emitted from the storage adapter's observe().
        assertEquals(
                // Expect items as described above.
                Observable.fromIterable(expectedResponseItems)
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
        // There should be 2 BlogOwners, and 2 MetaData records.
        List<? extends Model> itemsInStorage = storageAdapter.query(modelProvider);
        //The class count should be one less than total model count because Author model has sync expression =
        // QueryPredicates.none()
        assertEquals(
                itemsInStorage.toString(),
                expectedResponseItems.size() * 2 + (modelProvider.models().size() - 1),
                itemsInStorage.size()
        );
        assertEquals(
                // Expect the 4 items for the bloggers (2 models and their metadata)
                Observable.fromIterable(expectedResponseItems)
                        .flatMap(blogger -> Observable.fromArray(blogger.getModel(), blogger.getSyncMetadata()))
                        .toList()
                        .map(HashSet::new)
                        .blockingGet(),
                Observable.fromIterable(storageAdapter.query(modelProvider))
                        .filter(item -> !LastSyncMetadata.class.isAssignableFrom(item.getClass()))
                        .toList()
                        .map(HashSet::new)
                        .blockingGet()
        );
        adapterObserver.dispose();
        hydrationObserver.dispose();
    }

    private static ModelWithMetadata<BlogOwner> randomBlogOwnerWithMetadata() {
        BlogOwner blogOwner = BlogOwner.builder()
                .name(RandomString.string())
                .id(RandomString.string())
                .build();
        Temporal.Timestamp randomTimestamp = new Temporal.Timestamp(new Random().nextLong(), TimeUnit.SECONDS);
        return new ModelWithMetadata<>(blogOwner,
                new ModelMetadata(blogOwner.getId(), null, new Random().nextInt(), randomTimestamp)
        );
    }

    private static HubAccumulator createAccumulator(HubEventFilter eventFilter, int times) {
        return HubAccumulator.create(HubChannel.DATASTORE, eventFilter, times);
    }

    private static HubEventFilter forEvent(DataStoreChannelEventName eventName) {
        return hubEvent -> eventName.toString().equals(hubEvent.getName());
    }

    @SuppressWarnings("unchecked")
    private HubEventFilter syncMetricsEmittedFor(Class<? extends Model>... models) {
        List<String> modelNames = ForEach.inCollection(Arrays.asList(models), Class::getSimpleName);

        return hubEvent -> {
            if (!(hubEvent.getData() instanceof ModelSyncedEvent)) {
                return false;
            }
            ModelSyncedEvent hubEventData = (ModelSyncedEvent) hubEvent.getData();
            return forEvent(DataStoreChannelEventName.MODEL_SYNCED).filter(hubEvent) &&
                modelNames.contains(hubEventData.getModel());
        };
    }

    private static HubEventFilter syncQueryStartedForModels(int modelCount) {
        return hubEvent -> {
            return forEvent(DataStoreChannelEventName.SYNC_QUERIES_STARTED).filter(hubEvent) &&
                hubEvent.getData() instanceof SyncQueriesStartedEvent &&
                ((SyncQueriesStartedEvent) hubEvent.getData()).getModels().length == modelCount;
        };
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
            return left.getPrimaryKeyString().compareTo(right.getPrimaryKeyString());
        }
    }
}
