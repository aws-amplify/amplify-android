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

import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.Time;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link Merger}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MergerTest {
    private static final long REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(2);

    private SynchronousStorageAdapter storageAdapter;
    private MutationOutbox mutationOutbox;
    private Merger merger;

    /**
     * Sets up the test. A {@link Merger} is being tested. To construct one, several
     * intermediary objects are needed. A reference is held to a {@link MutationOutbox},
     * to arrange state. A {@link SynchronousStorageAdapter} is crated to facilitate
     * arranging model data into the {@link InMemoryStorageAdapter} which backs the various
     * components.
     */
    @Before
    public void setup() {
        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);
        this.mutationOutbox = new PersistentMutationOutbox(inMemoryStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(inMemoryStorageAdapter);
        this.merger = new Merger(mutationOutbox, versionRepository, inMemoryStorageAdapter);
    }

    /**
     * Assume there is a item A in the store. Then, we try to merge
     * a mutation to delete item A. This should succeed. After the
     * merge, A should NOT be in the store anymore.
     * @throws DataStoreException On failure to arrange test data into store,
     *                            or on failure to query results for test assertions
     */
    @Test
    public void mergeDeletionForExistingItem() throws DataStoreException {
        // Arrange: A blog owner, and some metadata about it, are in the store.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata originalMetadata =
            new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        storageAdapter.save(blogOwner, originalMetadata);
        // Just to be sure, our arrangement worked, and that thing is in there, right? Good.
        assertEquals(Collections.singletonList(blogOwner), storageAdapter.query(BlogOwner.class));

        // Act: merge a model deletion.
        ModelMetadata deletionMetadata =
            new ModelMetadata(blogOwner.getId(), true, 2, Time.now());
        TestObserver<Void> observer =
            merger.merge(new ModelWithMetadata<>(blogOwner, deletionMetadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: the blog owner is no longer in the store.
        assertEquals(0, storageAdapter.query(BlogOwner.class).size());
    }

    /**
     * Assume there is NOT an item in the store. Then, we try to
     * merge a mutation to delete item A. This should succeed, since
     * there was no work to be performed (it was already deleted.) After
     * the merge, there should STILL be no matching item in the store.
     * @throws DataStoreException On failure to query results for assertions
     */
    @Test
    public void mergeDeletionForNotExistingItem() throws DataStoreException {
        // Arrange, to start, there are no items matching the incoming deletion request.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        // Note that storageAdapter.save(...) does NOT happen!
        // storageAdapter.save(blogOwner, new ModelMetadata(blogOwner.getId(), false, 1, Time.now()));

        // Act: try to merge a deletion that refers to an item not in the store
        ModelMetadata deletionMetadata =
            new ModelMetadata(blogOwner.getId(), true, 1, Time.now());
        TestObserver<Void> observer =
            merger.merge(new ModelWithMetadata<>(blogOwner, deletionMetadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: there is still nothing in the store.
        assertEquals(0, storageAdapter.query(BlogOwner.class).size());
    }

    /**
     * Assume there is NO item A. Then, we try to merge a save for a
     * item A. This should succeed, with A being in the store, at the end.
     * @throws DataStoreException On failure to query results for assertions
     */
    @Test
    public void mergeSaveForNotExistingItem() throws DataStoreException {
        // Arrange: nothing in the store, to start.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        // Note that storageAdapter.save(...) is NOT called!
        // storageAdapter.save(blogOwner, metadata);

        // Act: merge a creation for an item
        TestObserver<Void> observer = merger.merge(new ModelWithMetadata<>(blogOwner, metadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: the item & its associated metadata are now in the store.
        assertEquals(Collections.singletonList(blogOwner), storageAdapter.query(BlogOwner.class));
        assertEquals(Collections.singletonList(metadata), storageAdapter.query(ModelMetadata.class));
    }

    /**
     * Assume there is an item A in the store. We try to merge a save for A.
     * This should succeed, and it should be treated as an update. After the merge,
     * A should have the updates from the merge.
     * @throws DataStoreException On failure to arrange data into store
     */
    @Test
    public void mergeSaveForExistingItem() throws DataStoreException {
        // Arrange: an item is already in the store.
        BlogOwner originalModel = BlogOwner.builder()
            .name("Jameson The Original")
            .build();
        ModelMetadata originalMetadata =
            new ModelMetadata(originalModel.getId(), false, 1, Time.now());
        storageAdapter.save(originalModel, originalMetadata);

        // Act: merge a save.
        BlogOwner updatedModel = originalModel.copyOfBuilder()
            .name("Jameson The New and Improved")
            .build();
        ModelMetadata updatedMetadata =
            new ModelMetadata(originalMetadata.getId(), false, 2, Time.now());
        TestObserver<Void> observer = merger.merge(new ModelWithMetadata<>(updatedModel, updatedMetadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertComplete().assertNoErrors();

        // Assert: the *UPDATED* stuff is in the store, *only*.
        assertEquals(Collections.singletonList(updatedModel), storageAdapter.query(BlogOwner.class));
        assertEquals(Collections.singletonList(updatedMetadata), storageAdapter.query(ModelMetadata.class));
    }

    /**
     * When an item comes into the merger to be merged,
     * if there is a pending mutation in the outbox, for a model of the same ID,
     * then that item shall NOT be merged.
     * @throws DataStoreException On failure to arrange data into store
     */
    @Test
    public void itemIsNotMergedWhenOutboxHasPendingMutation() throws DataStoreException {
        // Arrange: some model with a well known ID exists on the system.
        // We pretend that the user has recently updated it via the DataStore update() API.
        String knownId = RandomString.string();
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .id(knownId)
            .build();
        ModelMetadata localMetadata =
            new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        storageAdapter.save(blogOwner, localMetadata);

        PendingMutation<BlogOwner> pendingMutation =
            PendingMutation.instance(blogOwner, BlogOwner.class, PendingMutation.Type.CREATE);
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(pendingMutation).test();
        enqueueObserver.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS);
        enqueueObserver.assertNoErrors().assertComplete();

        // Act: now, cloud sync happens, and the sync engine tries to apply an update
        // for the same model ID, into the store. According to the cloud, this same
        // item should be DELETED.
        ModelMetadata cloudMetadata = new ModelMetadata(knownId, true, 2, Time.now());
        TestObserver<Void> mergeObserver = merger.merge(new ModelWithMetadata<>(blogOwner, cloudMetadata)).test();
        mergeObserver.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS);
        mergeObserver.assertNoErrors().assertComplete();

        // Assert: the item is NOT deleted from the local store.
        // The original is still there.
        // Or in other words, the cloud data was NOT merged.
        final List<BlogOwner> blogOwnersInStorage = storageAdapter.query(BlogOwner.class);
        assertEquals(1, blogOwnersInStorage.size());
        assertEquals(blogOwner, blogOwnersInStorage.get(0));
    }

    /**
     * An incoming mutation whose model has a LOWER version than an already existing model
     * shall be rejected from the merger.
     * @throws DataStoreException On failure interacting with local store during test arrange/verify.
     */
    @Test
    public void itemWithLowerVersionIsNotMerged() throws DataStoreException {
        // Arrange a model and metadata into storage.
        BlogOwner existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build();
        ModelMetadata existingMetadata = new ModelMetadata(existingModel.getId(), false, 55, Time.now());
        storageAdapter.save(existingModel, existingMetadata);

        // Act: try to merge, but specify a LOWER version.
        BlogOwner incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build();
        ModelMetadata lowerVersionMetadata =
            new ModelMetadata(incomingModel.getId(), false, 33, Time.now());
        ModelWithMetadata<BlogOwner> modelWithLowerVersionMetadata =
            new ModelWithMetadata<>(existingModel, lowerVersionMetadata);
        TestObserver<Void> mergeObserver = merger.merge(modelWithLowerVersionMetadata).test();
        mergeObserver.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS);
        mergeObserver.assertNoErrors().assertComplete();

        // Assert: Joey is still the same old Joey.
        assertEquals(Collections.singletonList(existingModel), storageAdapter.query(BlogOwner.class));

        // And his metadata is the still the same.
        assertEquals(
            Collections.singletonList(existingMetadata),
            storageAdapter.query(ModelMetadata.class, Where.id(existingModel.getId()))
        );
    }

    /**
     * If the incoming change has the SAME version as the data currently in the DB, we refuse to update it.
     * The user may have updated the data locally via the DataStore API. So we would clobber it.
     * The version must be strictly HIGHER than the current version, in order for the merge to succeed.
     * @throws DataStoreException On failure to interact with storage during arrange/verify
     */
    @Test
    public void itemWithSameVersionIsNotMerged() throws DataStoreException {
        // Arrange a model and metadata into storage.
        BlogOwner existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build();
        ModelMetadata existingMetadata = new ModelMetadata(existingModel.getId(), false, 55, Time.now());
        storageAdapter.save(existingModel, existingMetadata);

        // Act: try to merge, but specify a LOWER version.
        BlogOwner incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build();
        ModelMetadata lowerVersionMetadata =
            new ModelMetadata(incomingModel.getId(), false, 33, Time.now());
        ModelWithMetadata<BlogOwner> modelWithLowerVersionMetadata =
            new ModelWithMetadata<>(incomingModel, lowerVersionMetadata);
        TestObserver<Void> mergeObserver = merger.merge(modelWithLowerVersionMetadata).test();
        mergeObserver.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS);
        mergeObserver.assertNoErrors().assertComplete();

        // Assert: Joey is still the same old Joey.
        List<BlogOwner> actualBlogOwners = storageAdapter.query(BlogOwner.class);
        assertEquals(1, actualBlogOwners.size());
        assertEquals(existingModel, actualBlogOwners.get(0));

        // And his metadata is the still the same.
        assertEquals(
            Collections.singletonList(existingMetadata),
            storageAdapter.query(ModelMetadata.class, Where.id(existingModel.getId()))
        );
    }

    /**
     * Gray-box, we know that "no version" evaluates to a version of 0.
     * So, this test should always behave like {@link #itemWithLowerVersionIsNotMerged()}.
     * But, it the inputs to the system are technically different, so it is
     * a distinct test in terms of system input/output.
     * @throws DataStoreException On failure to interact with storage during arrange/verification
     */
    @Test
    public void itemWithoutVersionIsNotMerged() throws DataStoreException {
        // Arrange a model and metadata into storage.
        BlogOwner existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build();
        ModelMetadata existingMetadata = new ModelMetadata(existingModel.getId(), false, 1, Time.now());
        storageAdapter.save(existingModel, existingMetadata);

        // Act: try to merge, but don't specify a version in the metadata being used to merge.
        BlogOwner incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build();
        ModelMetadata metadataWithoutVersion = new ModelMetadata(incomingModel.getId(), null, null, null);
        ModelWithMetadata<BlogOwner> incomingModelWithMetadata =
            new ModelWithMetadata<>(existingModel, metadataWithoutVersion);
        TestObserver<Void> mergeObserver = merger.merge(incomingModelWithMetadata).test();
        mergeObserver.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS);
        mergeObserver.assertNoErrors().assertComplete();

        // Assert: Joey is still the same old Joey.
        assertEquals(Collections.singletonList(existingModel), storageAdapter.query(BlogOwner.class));

        // And his metadata is the still the same.
        assertEquals(
            Collections.singletonList(existingMetadata),
            storageAdapter.query(ModelMetadata.class, Where.id(existingModel.getId()))
        );
    }
}
