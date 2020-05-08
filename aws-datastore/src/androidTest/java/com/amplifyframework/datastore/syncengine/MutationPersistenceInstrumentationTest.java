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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the {@link SQLiteStorageAdapter} is able to serve as as repository
 * for {@link PendingMutation.PersistentRecord}s.
 */
public final class MutationPersistenceInstrumentationTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private PendingMutation.Converter converter;
    private SynchronousStorageAdapter storage;

    /**
     * Prepare an instance of {@link LocalStorageAdapter}. Evaluate its
     * emitted collection of ModelSchema, to ensure that
     * {@link PendingMutation.PersistentRecord} is among them.
     *
     * TODO: later, consider hiding system schema, such as the
     * {@link PendingMutation.PersistentRecord}, from the callback. This schema might be
     * an implementation detail, that is working as a leaky abstraction.
     *
     * @throws AmplifyException On failure to initialize the storage adapter,
     *                          or on failure to load model schema into registry
     */
    @Before
    public void obtainLocalStorageAndValidateModelSchema() throws AmplifyException {
        this.converter = new GsonPendingMutationConverter();
        getApplicationContext().deleteDatabase(DATABASE_NAME);

        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        LocalStorageAdapter localStorageAdapter =
            SQLiteStorageAdapter.forModels(modelSchemaRegistry, modelProvider);
        this.storage = SynchronousStorageAdapter.delegatingTo(localStorageAdapter);
        List<ModelSchema> initializationResults = storage.initialize(getApplicationContext());

        // Evaluate the returned set of ModelSchema. Ensure that there is one
        // for the PendingMutation.PersistentRecord system class.
        assertTrue(
            Observable.fromIterable(initializationResults)
                .map(ModelSchema::getName)
                .toList()
                .blockingGet()
                .contains(PendingMutation.PersistentRecord.class.getSimpleName())
        );
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException On failure to terminate use of storage adapter
     */
    @After
    public void terminateLocalStorageAdapter() throws DataStoreException {
        storage.terminate();
        getApplicationContext().deleteDatabase(DATABASE_NAME);
    }

    /**
     * The adapter must be able to save a {@link PendingMutation.PersistentRecord}.
     * When we query the adapter for that same {@link PendingMutation.PersistentRecord},
     * we will expect to find an exact replica of the one we had saved.
     * @throws DataStoreException from storage item change, or on failure to manipulate I/O to DataStore
     */
    @Test
    public void adapterCanSaveAndQueryPersistentRecords() throws DataStoreException {
        final BlogOwner tonyDaniels = BlogOwner.builder()
            .name("Tony Daniels")
            .build();

        final PendingMutation<BlogOwner> originalTonyCreation =
            PendingMutation.creation(tonyDaniels, BlogOwner.class);

        // Save the creation mutation for Tony, as a PersistentRecord object.
        PendingMutation.PersistentRecord originalTonyCreationAsRecord =
            converter.toRecord(originalTonyCreation);
        storage.save(originalTonyCreationAsRecord);

        // Now, lookup what PersistentRecord we have in the storage.
        List<PendingMutation.PersistentRecord> recordsInStorage =
            storage.query(PendingMutation.PersistentRecord.class);

        // There should be 1, and it should be the original creation for Tony.
        assertEquals(1, recordsInStorage.size());
        PendingMutation.PersistentRecord firstRecordFoundInStorage = recordsInStorage.get(0);
        assertEquals(originalTonyCreationAsRecord, firstRecordFoundInStorage);

        // After we convert back from PersistentRecord, we should get back a copy of
        // what we created above
        PendingMutation<BlogOwner> reconstructedCreationOfTony =
            converter.fromRecord(firstRecordFoundInStorage);
        assertEquals(originalTonyCreation, reconstructedCreationOfTony);
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)}
     * is called to save a {@link PendingMutation.PersistentRecord}, we should see an event emitted on the
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}'s item consumer.
     * @throws DataStoreException On failure to convert received value out of record format, or
     *                            on failure to manipulate data in/out of DataStore
     */
    @Test
    public void saveIsObservedForPersistentRecord() throws DataStoreException {
        // Start watching observe().
        // The storage adapter emits StorageItemChange.
        TestObserver<StorageItemChange<? extends Model>> saveObserver = storage.observe().test();

        // Save something ..
        BlogOwner juan = BlogOwner.builder()
            .name("Juan Gonzales")
            .build();
        PendingMutation<BlogOwner> change = PendingMutation.creation(juan, BlogOwner.class);
        PendingMutation.PersistentRecord thingWeSaved = converter.toRecord(change);

        // Wait for it to save...
        storage.save(thingWeSaved);

        // Assert that our observer got the item;
        // The observed change makes reference to an item. That referred item is our
        // PendingMutation.PersistentRecord. It should be identical to the item that we saved.
        assertEquals(
            thingWeSaved,
            saveObserver.awaitCount(1)
                .values()
                .get(0)
                .item()
        );

        saveObserver.dispose();
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)}
     * is called to save a {@link PendingMutation.PersistentRecord}, we should expect to observe a change event
     * /containing/ that record within it. It will be received by the value consumer of
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}.
     *
     * Similarly, when we update the {@link PendingMutation.PersistentRecord} that we had just saved,
     * we should see an update notification on the subscription consumer. The type will be
     * StorageItemChange, and inside of it ill be a reference to the {@link PendingMutation.PersistentRecord}.
     *
     * @throws DataStoreException from storage item change, or on failure to manipulate I/O to DataStore
     */
    @Test
    public void updatesAreObservedForPersistentRecords() throws DataStoreException {
        // Establish a subscription to listen for storage change records
        TestObserver<StorageItemChange<? extends Model>> storageObserver = storage.observe().test();

        // Create a record for Joe, and a change to save him into storage
        BlogOwner joeLastNameMisspelled = BlogOwner.builder()
            .name("Joe Sweeneyy")
            .build();
        PendingMutation<BlogOwner> createJoeWrongLastName =
            PendingMutation.creation(joeLastNameMisspelled, BlogOwner.class);

        // Save our saveJoeWrongLastName change item, as a PersistentRecord.
        PendingMutation.PersistentRecord createJoeWrongLastNameAsRecord =
            converter.toRecord(createJoeWrongLastName);
        storage.save(createJoeWrongLastNameAsRecord);

        // Now, suppose we have to update that pending mutation. Maybe it contained a bad item payload.
        BlogOwner joeWithLastNameFix = BlogOwner.builder()
            .name("Joe Sweeney")
            .id(joeLastNameMisspelled.getId())
            .build();
        PendingMutation<BlogOwner> createJoeCorrectLastName =
            PendingMutation.creation(joeWithLastNameFix, BlogOwner.class);

        // Save an update (same model type, same unique ID) to the mutation we saved previously.
        PendingMutation.PersistentRecord createJoeCorrectLastNameAsRecord =
            converter.toRecord(createJoeCorrectLastName);
        storage.save(createJoeCorrectLastNameAsRecord);

        // Our observer got the records to save Joe with wrong age, and also to save joe with right age
        assertEquals(
            Arrays.asList(createJoeWrongLastNameAsRecord, createJoeCorrectLastNameAsRecord),
            Observable.fromIterable(storageObserver.awaitCount(2).values())
                .map(StorageItemChange::item)
                .toList()
                .blockingGet()
        );
        storageObserver.dispose();
    }

    /**
     * When an {@link PendingMutation.PersistentRecord} is deleted from the DataStore, we will expect
     * to see an event on item consumer of {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}.
     * @throws DataStoreException from storage item change, or on failure to manipulate I/O to DataStore
     */
    @Test
    public void deletionIsObservedForPersistentRecord() throws DataStoreException {
        // We are observing a stream of changes to models.
        // In this test, the <? extends Model> type happens to be PersistentRecord (itself, implementing Model.)
        TestObserver<StorageItemChange<? extends Model>> storageObserver = storage.observe().test();

        BlogOwner beatrice = BlogOwner.builder()
            .name("Beatrice Stone")
            .build();
        PendingMutation<BlogOwner> createBeatrice = PendingMutation.creation(beatrice, BlogOwner.class);
        PendingMutation.PersistentRecord createBeatriceRecord = converter.toRecord(createBeatrice);
        storage.save(createBeatriceRecord);

        // Assert that we do observe the PersistentRecord being saved ...
        assertEquals(
            createBeatriceRecord,
            storageObserver.awaitCount(1)
                .values()
                .get(0)
                .item()
        );
        storageObserver.dispose();

        TestObserver<StorageItemChange<? extends Model>> deletionObserver = storage.observe().test();

        // Try to delete Beatrice's record.
        storage.delete(createBeatriceRecord);

        // Should receive a notification of the deletion on the observer.
        // The notification refers to the deleted item, in its contents.
        assertEquals(
            createBeatriceRecord,
            deletionObserver.awaitCount(1)
                .values()
                .get(0)
                .item()
        );

        deletionObserver.dispose();
    }
}
