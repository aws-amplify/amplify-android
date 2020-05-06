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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.StorageItemChangeConverter;
import com.amplifyframework.datastore.storage.StorageItemChangeRecord;
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
 * for {@link StorageItemChangeRecord}s.
 */
public final class StorageItemChangeRecordIntegrationTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private StorageItemChangeConverter storageItemChangeConverter;
    private SynchronousStorageAdapter storageAdapter;

    /**
     * Prepare an instance of {@link LocalStorageAdapter}. Evaluate its
     * emitted collection of ModelSchema, to ensure that
     * {@link StorageItemChangeRecord} is among them.
     *
     * TODO: later, consider hiding system schema, such as the
     * StorageItemChangeRecord, from the callback. This schema might be
     * an implementation detail, that is working as a leaky abstraction.
     *
     * @throws AmplifyException On failure to initialize the storage adapter,
     *                          or on failure to load model schema into registry
     */
    @Before
    public void obtainLocalStorageAndValidateModelSchema() throws AmplifyException {
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        getApplicationContext().deleteDatabase(DATABASE_NAME);

        ModelProvider modelProvider = ModelProviderFactory.createProviderOf(BlogOwner.class);
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        LocalStorageAdapter localStorageAdapter =
            SQLiteStorageAdapter.forModels(modelSchemaRegistry, modelProvider);
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(localStorageAdapter);
        List<ModelSchema> initializationResults = storageAdapter.initialize(getApplicationContext());

        // Evaluate the returned set of ModelSchema. Ensure that there is one
        // for the StorageItemChangeRecord system class.
        assertTrue(
            Observable.fromIterable(initializationResults)
                .map(ModelSchema::getName)
                .toList()
                .blockingGet()
                .contains(StorageItemChangeRecord.class.getSimpleName())
        );
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException On failure to terminate use of storage adapter
     */
    @After
    public void terminateLocalStorageAdapter() throws DataStoreException {
        storageAdapter.terminate();
        getApplicationContext().deleteDatabase(DATABASE_NAME);
    }

    /**
     * The adapter must be able to save a StorageItemChangeRecord. When we query the adapter for that
     * same StorageItemChangeRecord, we will expect to find an exact replica of the one we
     * had saved.
     * @throws DataStoreException from storage item change, or on failure to mainuplate I/O to DataStore
     */
    @Test
    public void adapterCanSaveAndQueryChangeRecords() throws DataStoreException {
        final BlogOwner tonyDaniels = BlogOwner.builder()
            .name("Tony Daniels")
            .build();

        final StorageItemChange<BlogOwner> originalTonyCreation = StorageItemChange.<BlogOwner>builder()
            .item(tonyDaniels)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.CREATE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();

        // Save the creation mutation for Tony, as a Record object.
        StorageItemChangeRecord originalTonyCreationAsRecord =
            storageItemChangeConverter.toRecord(originalTonyCreation);
        storageAdapter.save(originalTonyCreationAsRecord);

        // Now, lookup what records we have in the storage.
        List<StorageItemChangeRecord> recordsInStorage =
            storageAdapter.query(StorageItemChangeRecord.class);

        // There should be 1, and it should be the original creation for Tony.
        assertEquals(1, recordsInStorage.size());
        StorageItemChangeRecord firstRecordFoundInStorage = recordsInStorage.get(0);
        assertEquals(originalTonyCreationAsRecord, firstRecordFoundInStorage);

        // After we convert back from record, we should get back a copy of
        // what we created above
        StorageItemChange<BlogOwner> reconstructedCreationOfTony =
            storageItemChangeConverter.fromRecord(firstRecordFoundInStorage);
        assertEquals(originalTonyCreation, reconstructedCreationOfTony);
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)}
     * is called to save a {@link StorageItemChangeRecord}, we should see an event emitted on the
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}'s item consumer.
     * @throws DataStoreException On failure to convert received value out of record format, or
     *                            on failure to manipulate data in/out of DataStore
     */
    @Test
    public void saveIsObservedForChangeRecord() throws DataStoreException {
        // Start watching observe() ...
        TestObserver<StorageItemChange<? extends Model>> saveObserver = storageAdapter.observe().test();

        // Save something ..
        StorageItemChange<BlogOwner> change = StorageItemChange.<BlogOwner>builder()
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .item(BlogOwner.builder()
                .name("Juan Gonzales")
                .build())
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.CREATE)
            .build();
        StorageItemChangeRecord thingWeSaved = storageItemChangeConverter.toRecord(change);

        // Wait for it to save...
        storageAdapter.save(thingWeSaved);

        // Assert that our observer got the item;
        // The observed change makes reference to an item. That item is of type StorageItemChangeRecord.
        // It should be identical to the item that we saved.
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
     * is called to save a {@link StorageItemChangeRecord}, we should expect to observe a change event
     * /containing/ that record within it. It will be received by the value consumer of
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}.
     *
     * Similarly, when we update the record that we had just saved, we should see an update
     * notification on the subscription consumer. The type will be StorageItemChange, and inside of it
     * will be a StorageItemChangeRecord.
     * @throws DataStoreException from storage item change, or on failure to manipulate I/O to DataStore
     */
    @Test
    public void updatesAreObservedForChangeRecords() throws DataStoreException {
        // Establish a subscription to listen for storage change records
        TestObserver<StorageItemChange<? extends Model>> storageObserver = storageAdapter.observe().test();

        // Create a record for Joe, and a change to save him into storage
        BlogOwner joeLastNameMisspelled = BlogOwner.builder()
            .name("Joe Sweeneyy")
            .build();
        StorageItemChange<BlogOwner> createJoeWrongLastName = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.CREATE)
            .item(joeLastNameMisspelled)
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChangeRecord createJoeWrongLastNameAsRecord =
            storageItemChangeConverter.toRecord(createJoeWrongLastName);

        // Save our saveJoeWrongLastName change item, as a record.
        storageAdapter.save(createJoeWrongLastNameAsRecord);

        // Now, suppose we have to update that change object. Maybe it contained a bad item payload.
        BlogOwner joeWithLastNameFix = BlogOwner.builder()
            .name("Joe Sweeney")
            .build();
        StorageItemChange<BlogOwner> createJoeCorrectLastName = StorageItemChange.<BlogOwner>builder()
            .changeId(createJoeWrongLastName.changeId().toString()) // Same ID
            .item(joeWithLastNameFix) // But with a patch to the item
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .type(StorageItemChange.Type.UPDATE) // We're still *creating Joe*, we're *updating this change*.
            .build();
        StorageItemChangeRecord createJoeCorrectLastNameAsRecord =
            storageItemChangeConverter.toRecord(createJoeCorrectLastName);

        // Save an update (same model type, same unique ID) to the change we saved previously.
        storageAdapter.save(createJoeCorrectLastNameAsRecord);

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
     * When an {@link StorageItemChangeRecord} is deleted from the DataStore, we will expect
     * to see an event on item consumer of {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}.
     * @throws DataStoreException from storage item change, or on failure to manipulate I/O to DataStore
     */
    @Test
    public void deletionIsObservedForChangeRecord() throws DataStoreException {
        // We are observing a stream of changes to models.
        // In this test, the <? extends Model> type happens to be StorageItemChangeRecord (a model.)
        TestObserver<StorageItemChange<? extends Model>> storageObserver = storageAdapter.observe().test();

        BlogOwner beatrice = BlogOwner.builder()
            .name("Beatrice Stone")
            .build();
        StorageItemChange<BlogOwner> createBeatrice = StorageItemChange.<BlogOwner>builder()
            .item(beatrice)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.CREATE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChangeRecord createBeatriceRecord =
            storageItemChangeConverter.toRecord(createBeatrice);

        storageAdapter.save(createBeatriceRecord);

        // Assert that we do observe the record being saved ...
        assertEquals(
            createBeatriceRecord,
            storageObserver.awaitCount(1)
                .values()
                .get(0)
                .item()
        );
        storageObserver.dispose();

        TestObserver<StorageItemChange<? extends Model>> deletionObserver = storageAdapter.observe().test();

        // The creation record above won't change, but we want to delete Beatrice's record, itself.
        storageAdapter.delete(createBeatriceRecord);

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
