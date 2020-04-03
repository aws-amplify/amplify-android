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

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.Time;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import edu.emory.mathcs.backport.java.util.Collections;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link Merger}.
 * NOTE: the tests show that the Merger is not currently working to spec.
 * At the moment, Merger just applies changes into the local store, without
 * any consideration to the {@link MutationOutbox}. TODO: fix this.
 */
public final class MergerTest {
    private static final long REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(2);

    private InMemoryStorageAdapter inMemoryStorageAdapter;
    private MutationOutbox mutationOutbox;
    private Merger merger;

    @Before
    public void setup() {
        this.inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.mutationOutbox = new MutationOutbox(inMemoryStorageAdapter);
        this.merger = new Merger(mutationOutbox, inMemoryStorageAdapter);
    }

    /**
     * Assume there is a record A in the store. Then, we try to merge
     * a mutation to delete record A. This should succeed. After the
     * merge, A should NOT be in the store anymore.
     * @throws DataStoreException On failure to arrange test data into store
     */
    @Test
    public void mergeDeletionForExistingRecord() throws DataStoreException {
        // Arrange: A blog owner, and some metadata about it, are in the store.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata originalMetadata =
            new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        putInStore(blogOwner, originalMetadata);
        // Just to be sure, our arrangement worked, and that thing is in there, right? Good.
        assertEquals(Collections.singletonList(blogOwner), findModels(BlogOwner.class));

        // Act: merge a deletion record against the model.
        ModelMetadata deletionMetadata =
            new ModelMetadata(blogOwner.getId(), true, 1, Time.now());
        TestObserver<Void> observer =
            merger.merge(new ModelWithMetadata<>(blogOwner, deletionMetadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: the blog owner is no longer in the store.
        assertEquals(0, findModels(BlogOwner.class).size());
    }

    /**
     * Assume there is NOT a record in the store. Then, we try to
     * merge a mutation to delete record A. This should succeed, since
     * there was no work to be performed (it was already deleted.) After
     * the merge, there should STILL be no matching record in the store.
     */
    @Test
    public void mergeDeletionForNotExistingRecord() {
        // Arrange, to start, there are no records matching the incoming deletion request.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        // Note that putInStore does NOT happen!
        // putInStore(blogOwner, new ModelMetadata(blogOwner.getId(), false, 1, Time.now()));

        // Act: merge a deletion record that refers to something not in the store
        ModelMetadata deletionMetadata =
            new ModelMetadata(blogOwner.getId(), true, 1, Time.now());
        TestObserver<Void> observer =
            merger.merge(new ModelWithMetadata<>(blogOwner, deletionMetadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: there is still nothing in the store.
        assertEquals(0, findModels(BlogOwner.class).size());
    }

    /**
     * Assume there is NO record A. Then, we try to merge a save for a
     * record A. This should succeed, with A being in the store, at the end.
     */
    @Test
    public void mergeSaveForNotExistingRecord() {
        // Arrange: nothing in the store, to start.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        // Note that putInStore is NOT called!
        // putInStore(blogOwner, metadata);

        // Act: merge a creation record
        TestObserver<Void> observer = merger.merge(new ModelWithMetadata<>(blogOwner, metadata)).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));
        observer.assertNoErrors().assertComplete();

        // Assert: the record & its associated metadata are now in the store.
        assertEquals(Collections.singletonList(blogOwner), findModels(BlogOwner.class));
        assertEquals(Collections.singletonList(metadata), findModels(ModelMetadata.class));
    }

    /**
     * Assume there is a record A in the store. We try to merge a save for A.
     * This should succeed, and it should be treated as an update. After the merge,
     * A should have the updates from the merge.
     * @throws DataStoreException On failure to arrange data into store
     */
    @Test
    public void mergeSaveForExistingRecord() throws DataStoreException {
        // Arrange: a record is already in the store.
        BlogOwner originalModel = BlogOwner.builder()
            .name("Jameson The Original")
            .build();
        ModelMetadata originalMetadata =
            new ModelMetadata(originalModel.getId(), false, 1, Time.now());
        putInStore(originalModel, originalMetadata);

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
        assertEquals(Collections.singletonList(updatedModel), findModels(BlogOwner.class));
        assertEquals(Collections.singletonList(updatedMetadata), findModels(ModelMetadata.class));
    }

    /**
     * When a record comes into the merger to be merged,
     * if there is a pending mutation in the outbox, for a model of the same ID,
     * then that record shall NOT be merged.
     * @throws DataStoreException On failure to arrange data into store
     */
    @Test
    public void recordIsNotMergedWhenOutboxHasPendingMutation() throws DataStoreException {
        // Arrange: some model with a well known ID exists on the system.
        // We pretend that the user has recently updated it via the DataStore update() API.
        String knownId = RandomString.string();
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .id(knownId)
            .build();
        ModelMetadata localMetadata =
            new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        putInStore(blogOwner, localMetadata);
        mutationOutbox.enqueue(StorageItemChange.<BlogOwner>builder()
            .changeId(knownId)
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .item(blogOwner)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.UPDATE)
            .build());

        // Act: now, cloud sync happens, and the sync engine tries to apply an update
        // for the same model ID, into the store. According to the cloud, this same
        // record should be DELETED.
        ModelMetadata cloudMetadata = new ModelMetadata(knownId, true, 2, Time.now());
        merger.merge(new ModelWithMetadata<>(blogOwner, cloudMetadata));

        // Assert: the record is NOT deleted from the local store.
        // The original is still there.
        // Or in other words, the cloud data was NOT merged.
        final List<BlogOwner> blogOwnersInStorage = findModels(BlogOwner.class);
        assertEquals(1, blogOwnersInStorage.size());
        assertEquals(blogOwner, blogOwnersInStorage.get(0));
    }

    /**
     * Saves a variable argument list of models into the {@link InMemoryStorageAdapter}.
     * @param models Models to save
     * @param <T> Type of models
     * @throws DataStoreException On failure to save
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    private final <T extends Model> void putInStore(final T... models) throws DataStoreException {
        for (T model : models) {
            Await.<StorageItemChange.Record, DataStoreException>result((onResult, onError) ->
                inMemoryStorageAdapter.save(model, StorageItemChange.Initiator.DATA_STORE_API, onResult, onError)
            );
        }
    }

    /**
     * Find models in the {@link InMemoryStorageAdapter} of a given class.
     * @param modelClass Class of model being looked up
     * @return A list of matching records; empty, if there are no matches
     */
    @SuppressWarnings({"SameParameterValue", "unchecked"})
    private <T extends Model> List<T> findModels(@NonNull Class<T> modelClass) {
        Objects.requireNonNull(modelClass);
        final List<T> results = new ArrayList<>();
        for (Model model : inMemoryStorageAdapter.items()) {
            if (model.getClass().isAssignableFrom(modelClass)) {
                results.add((T) model);
            }
        }
        return results;
    }
}
