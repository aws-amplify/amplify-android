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
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.datastore.storage.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * Tests that the {@link SQLiteStorageAdapter} is able to serve as as repository
 * for our {@link StorageItemChange.Record}s.
 */
public final class StorageItemChangeRecordIntegrationTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private GsonStorageItemChangeConverter storageItemChangeConverter;
    private SynchronousStorageAdapter storageAdapter;

    /**
     * Prepare an instance of {@link LocalStorageAdapter}, and ensure that it will
     * return a ModelSchema for the {@link StorageItemChange.Record} type.
     * TODO: later, consider hiding this schema from the callback. This is sort
     *       of leaking an implementation detail to the customer of the API.
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

        // Evaluate the returned set of ModelSchema. Make sure that there is one
        // for the StorageItemChange.Record system class, and one for
        // the PersistentModelVersion.
        final List<String> actualNames = new ArrayList<>();
        for (ModelSchema modelSchema : initializationResults) {
            actualNames.add(modelSchema.getName());
        }
        Collections.sort(actualNames);

        final Set<Class<? extends Model>> systemModelClasses = SystemModelsProviderFactory.create().models();
        final List<String> expectedNames = Observable.fromIterable(systemModelClasses)
            .startWith(BlogOwner.class)
            .map(Class::getSimpleName)
            .toSortedList()
            .blockingGet();

        assertEquals(expectedNames, actualNames);
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
     * The adapter must be able to save a StorageItemChange.Record. When we query the adapter for that
     * same StorageItemChange.Record, we will expect to find an exact replica of the one we
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
        StorageItemChange.Record originalTonyCreationAsRecord =
            originalTonyCreation.toRecord(storageItemChangeConverter);
        storageAdapter.save(originalTonyCreationAsRecord);

        // Now, lookup what records we have in the storage.
        List<StorageItemChange.Record> recordsInStorage =
            storageAdapter.query(StorageItemChange.Record.class);

        // There should be 1, and it should be the original creation for Tony.
        assertEquals(1, recordsInStorage.size());
        StorageItemChange.Record firstRecordFoundInStorage = recordsInStorage.get(0);
        assertEquals(originalTonyCreationAsRecord, firstRecordFoundInStorage);

        // After we convert back from record, we should get back a copy of
        // what we created above
        StorageItemChange<BlogOwner> reconstructedCreationOfTony =
            firstRecordFoundInStorage.toStorageItemChange(storageItemChangeConverter);
        assertEquals(originalTonyCreation, reconstructedCreationOfTony);
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)}
     * is called for a {@link StorageItemChange.Record}, we should see this event on the
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)} method's `onNextItem` callback.
     * @throws DataStoreException On failure to convert received value out of record format, or
     *                            on failure to manipulate data in/out of DataStore
     */
    @Test
    public void saveIsObservedForChangeRecord() throws DataStoreException {
        // Start watching observe() ...
        TestObserver<StorageItemChange.Record> saveObserver = TestObserver.create();
        storageAdapter.observe().subscribe(saveObserver);

        // Save something ..
        StorageItemChange.Record record = StorageItemChange.<BlogOwner>builder()
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .item(BlogOwner.builder()
                .name("Juan Gonzales")
                .build())
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.CREATE)
            .build()
            .toRecord(storageItemChangeConverter);

        // Wait for it to save...
        storageAdapter.save(record);

        // Assert that our observer got the item;
        // The record we get back has the saved record inside of it, as the contained item field.
        assertEquals(
            record,
            saveObserver.awaitCount(1)
                .values()
                .get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );

        saveObserver.dispose();
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)}
     * is called for a {@link StorageItemChange.Record}, we should expect to observe a
     * record /containing/ that record within it, in a callback to the first consumer passed into
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)}.
     *
     * Similarly, when we update the record that we had just saved, we should see an update
     * record on the subscription consumer. The type will be StorageItemChange.Record and inside of it
     * will be a StorageItemChange.Record which itself contains a BlogOwner.
     * @throws DataStoreException from storage item change, or on failure to mainuplate I/O to DataStore
     */
    @Test
    public void updatesAreObservedForChangeRecords() throws DataStoreException {
        // Establish a subscription to listen for storage change records
        TestObserver<StorageItemChange.Record> storageObserver = TestObserver.create();
        storageAdapter.observe().subscribe(storageObserver);

        // Create a record for Joe, and a change to save him into storage
        BlogOwner joeLastNameMispelled = BlogOwner.builder()
            .name("Joe Sweeneyy")
            .build();
        StorageItemChange<BlogOwner> createJoeWrongLastName = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.CREATE)
            .item(joeLastNameMispelled)
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record createJoeWrongLastNameAsRecord =
            createJoeWrongLastName.toRecord(storageItemChangeConverter);

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
        StorageItemChange.Record createJoeCorrectLastNameAsRecord =
            createJoeCorrectLastName.toRecord(storageItemChangeConverter);

        // Save an update (same model type, same unique ID) to the change we saved previously.
        storageAdapter.save(createJoeCorrectLastNameAsRecord);

        // Our observer got the records to save Joe with wrong age, and also to save joe with right age
        List<StorageItemChange.Record> values = storageObserver.awaitCount(2).values();
        assertEquals(
            createJoeWrongLastNameAsRecord,
            values
                .get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        assertEquals(
            createJoeCorrectLastNameAsRecord,
            values
                .get(1)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        storageObserver.dispose();
    }

    /**
     * When an {@link StorageItemChange.Record} is deleted from the DataStore,
     * the first argument that was provided to
     * {@link LocalStorageAdapter#observe(Consumer, Consumer, Action)} will be called.
     * @throws DataStoreException from storage item change, or on failure to mainuplate I/O to DataStore
     */
    @Test
    public void deletionIsObservedForChangeRecord() throws DataStoreException {
        // What we are really observing are items of type StorageItemChange.Record that contain
        // StorageItemChange.Record of BlogOwner
        TestObserver<StorageItemChange.Record> storageObserver = TestObserver.create();
        storageAdapter.observe().subscribe(storageObserver);

        BlogOwner beatrice = BlogOwner.builder()
            .name("Beatrice Stone")
            .build();
        StorageItemChange<BlogOwner> createBeatrice = StorageItemChange.<BlogOwner>builder()
            .item(beatrice)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.CREATE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record createBeatriceRecord =
            createBeatrice.toRecord(storageItemChangeConverter);

        storageAdapter.save(createBeatriceRecord);

        // Assert that we do observe the record being saved ...
        assertEquals(
            createBeatriceRecord,
            storageObserver.awaitCount(1)
                .values()
                .get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        storageObserver.dispose();

        TestObserver<StorageItemChange.Record> deletionObserver = TestObserver.create();
        storageAdapter.observe().subscribe(deletionObserver);

        // The mutation record doesn't change, but we want to delete it, itself.
        storageAdapter.delete(createBeatriceRecord);

        assertEquals(
            createBeatriceRecord,
            deletionObserver
                .awaitCount(1)
                .values()
                .get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        deletionObserver.dispose();
    }
}
