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

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.testmodels.Person;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;

/**
 * Tests that the {@link SQLiteStorageAdapter} is able to serve as as repository
 * for our {@link StorageItemChange.Record}s.
 */
@SuppressWarnings("rawtypes")
public final class StorageItemChangeRecordIntegrationTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private GsonStorageItemChangeConverter storageItemChangeConverter;
    private LocalStorageAdapter localStorageAdapter;

    /**
     * Prepare an instance of {@link LocalStorageAdapter}, and ensure that it will
     * return a ModelSchema for the {@link StorageItemChange.Record} type.
     * TODO: later, consider hiding this schema from the callback. This is sort
     *       of leaking an implementation detail to the customer of the API.
     */
    @Before
    public void obtainLocalStorageAndValidateModelSchema() {
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);

        LatchedResultListener<List<ModelSchema>> schemaListener = LatchedResultListener.instance();
        ModelProvider personProvider = ModelProviderFactory.createProviderOf(Person.class);
        this.localStorageAdapter = SQLiteStorageAdapter.forModels(personProvider);
        localStorageAdapter.initialize(ApplicationProvider.getApplicationContext(), schemaListener);

        // Evaluate the returned set of ModelSchema. Make sure that there is one
        // for the StorageItemChange.Record system class.
        List<ModelSchema> schema = schemaListener.awaitTerminalEvent().assertNoError().getResult();
        assertEquals(
            "Wanted 2 schema, but got " + schema,
            2, schema.size()
        );
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @After
    public void terminateLocalStorageAdapter() {
        localStorageAdapter.terminate();
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);
    }

    /**
     * The adapter must be able to save a StorageItemChange.Record. When we query the adapter for that
     * same StorageItemChange.Record, we will expect to find an exact replica of the one we
     * had saved.
     */
    @SuppressWarnings("checkstyle:MagicNumber") // Tony's age is randomly chosen as 45 for no reason.
    @Test
    public void adapterCanSaveAndQueryChangeRecords() {
        final Person tony = Person.builder()
            .firstName("Tony")
            .lastName("Daniels")
            .age(45)
            .build();
        final StorageItemChange<Person> originalSaveForTony = StorageItemChange.<Person>builder()
            .item(tony)
            .itemClass(Person.class)
            .type(StorageItemChange.Type.SAVE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();

        // Save the creation mutation for tony, as a Record object.
        StorageItemChange.Record saveForTonyAsRecord =
            originalSaveForTony.toRecord(storageItemChangeConverter);
        save(saveForTonyAsRecord);

        // Now, lookup what records we have in the storage.
        List<StorageItemChange.Record> foundRecords = query();

        // There should be 1, the save for the insertionForTony.
        // and it should be identical to the thing we tried to save.
        assertEquals(1, foundRecords.size());
        StorageItemChange.Record firstResultRecord = foundRecords.get(0);
        assertEquals(saveForTonyAsRecord, firstResultRecord);

        // After we convert back from record, we should get back a copy of
        // what we created above
        StorageItemChange<Person> reconstructedSaveForTony =
            firstResultRecord.toStorageItemChange(storageItemChangeConverter);
        assertEquals(originalSaveForTony, reconstructedSaveForTony);
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)}
     * is called for a {@link StorageItemChange.Record}, we should see this event on the
     * {@link LocalStorageAdapter#observe()} method's {@link Observable}.
     */
    @Test
    public void saveIsObservedForChangeRecord() {

        // Start watching observe() ...
        TestObserver<StorageItemChange.Record> observer = TestObserver.create();
        localStorageAdapter.observe().subscribe(observer);

        // Save something ..
        StorageItemChange.Record record = StorageItemChange.<Person>builder()
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .item(Person.builder()
                .firstName("Juan")
                .lastName("Gonzales")
                .build())
            .itemClass(Person.class)
            .type(StorageItemChange.Type.SAVE)
            .build()
            .toRecord(new GsonStorageItemChangeConverter());

        // Wait for it to save...
        LatchedResultListener<StorageItemChange.Record> listener = LatchedResultListener.instance();
        localStorageAdapter.save(record, StorageItemChange.Initiator.SYNC_ENGINE, listener);
        listener.awaitTerminalEvent().assertNoError().assertResult();

        // Assert that our observer got the item;
        // The record we get back has the saved record inside of it, as the contained item field.
        observer.awaitCount(1);
        assertEquals(
            record,
            observer.values().get(0)
                .toStorageItemChange(new GsonStorageItemChangeConverter())
                .item()
        );
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)}
     * is called for a {@link StorageItemChange.Record}, we should expect to observe a
     * record /containing/ that record within it, via the {@link Observable} returned from
     * {@link LocalStorageAdapter#observe()}.
     *
     * Similarly, when we update the record that we had just saved, we should see an update
     * record on the observable. The type will be StorageItemChange.Record and inside of it
     * will be a StorageItemChange.Record which itself contains a Person.
     *
     */
    @Ignore("update operations are not currently implemented! TODO: validate this, once available")
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void updatesAreObservedForChangeRecords() {
        // Establish a subscription to listen for storage change records
        TestObserver<StorageItemChange.Record> recordObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(recordObserver);

        // Create a record for Joe, and a change to save him into storage
        Person joeWrongAge = Person.builder()
            .firstName("Joe")
            .lastName(("Sweeney"))
            .age(39)
            .build();
        StorageItemChange<Person> saveJoeWrongAge = StorageItemChange.<Person>builder()
            .type(StorageItemChange.Type.SAVE)
            .item(joeWrongAge)
            .itemClass(Person.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record saveJoeWrongAgeRecord =
            saveJoeWrongAge.toRecord(storageItemChangeConverter);

        // Save our saveJoeWrongAge change, as a record.
        save(saveJoeWrongAgeRecord);

        // Now, suppose we have to update that change object. Maybe it contained a bad item payload.
        Person joeWithCorrectAge = Person.builder()
            .firstName(joeWrongAge.getFirstName())
            .lastName(joeWrongAge.getLastName())
            .age(41) // Joe is actually 41, not 39, oops.
            .id(joeWrongAge.getId())
            .build();
        StorageItemChange<Person> saveJoeCorrectAge = StorageItemChange.<Person>builder()
            .changeId(saveJoeWrongAge.changeId().toString()) // Same ID
            .item(joeWithCorrectAge) // But with a patch to the item
            .itemClass(Person.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .type(StorageItemChange.Type.SAVE) // We're still saving Joe, we're updating this change.
            .build();
        StorageItemChange.Record saveJoeCorrectAgeRecord =
            saveJoeCorrectAge.toRecord(storageItemChangeConverter);

        // Save an update (same model type, same unique ID) to the thing we saved before.
        save(saveJoeCorrectAgeRecord);

        // Our observer got the records to save Joe with wrong age, and also to save joe with right age
        recordObserver.awaitCount(2);
        recordObserver.assertNoErrors();
        recordObserver.assertValues(saveJoeWrongAgeRecord, saveJoeCorrectAgeRecord);
    }

    /**
     * When an {@link StorageItemChange.Record} is deleted from the DataStore, the
     * {@link Observable} returned by {@link LocalStorageAdapter#observe()} shall
     * emit that same change record.
     */
    @Ignore("delete() is not currently implemented! Validate this test when it is.")
    @Test
    public void deletionIsObservedForChangeRecord() {
        // What we are really observing are items of type StorageItemChange.Record that contain
        // StorageItemChange.Record of Person
        TestObserver<StorageItemChange.Record> saveObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(saveObserver);

        Person beatrice = Person.builder()
            .firstName("Beatrice")
            .lastName("Stone")
            .build();
        StorageItemChange<Person> saveBeatrice = StorageItemChange.<Person>builder()
            .item(beatrice)
            .itemClass(Person.class)
            .type(StorageItemChange.Type.SAVE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record saveBeatriceRecord =
            saveBeatrice.toRecord(storageItemChangeConverter);

        save(saveBeatriceRecord);

        // Assert that we do observe the record being saved ...
        saveObserver.awaitCount(1);
        saveObserver.assertNoErrors();
        assertEquals(
            saveBeatriceRecord,
            saveObserver.values().get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        saveObserver.dispose();

        TestObserver<StorageItemChange.Record> deletionObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(deletionObserver);

        // The mutation record doesn't change, but we want to delete it, itself.
        delete(saveBeatriceRecord);

        deletionObserver.awaitCount(1);
        deletionObserver.assertNoErrors();
        deletionObserver.assertValue(saveBeatriceRecord);
        deletionObserver.dispose();
    }

    private void save(StorageItemChange.Record storageItemChangeRecord) {
        // The thing we are saving is a StorageItemChange.Record.
        // The fact that it is getting saved means it gets wrapped into another
        // StorageItemChange.Record, which itself contains the original StorageItemChange.Record.
        LatchedResultListener<StorageItemChange.Record> saveResultListener = LatchedResultListener.instance();

        localStorageAdapter.save(storageItemChangeRecord,
            StorageItemChange.Initiator.SYNC_ENGINE, saveResultListener);

        final StorageItemChange.Record result =
            saveResultListener.awaitTerminalEvent().assertNoError().getResult();

        final StorageItemChange convertedResult = result.toStorageItemChange(storageItemChangeConverter);

        // Peel out the item from the save result - the item inside is the thing we tried to save,
        // e.g., the mutation to create person
        // It should be identical to the thing that we tried to save.
        assertEquals(storageItemChangeRecord, convertedResult.item());
    }

    private List<StorageItemChange.Record> query() {
        // Okay, now we're going to do a query, then await & stash the query results.
        LatchedResultListener<Iterator<StorageItemChange.Record>> queryResultsListener =
            LatchedResultListener.instance();

        // TODO: if/when there is a form of query() which shall accept QueryPredicate, use that instead.
        localStorageAdapter.query(StorageItemChange.Record.class, queryResultsListener);

        Iterator<StorageItemChange.Record> queryResultsIterator =
            queryResultsListener.awaitTerminalEvent().assertNoError().getResult();

        final List<StorageItemChange.Record> storageItemChangeRecords = new ArrayList<>();
        while (queryResultsIterator.hasNext()) {
            storageItemChangeRecords.add(queryResultsIterator.next());
        }
        return storageItemChangeRecords;
    }

    private void delete(StorageItemChange.Record record) {
        // The thing we are deleting is a StorageItemChange.Record, which is wrapping
        // a StorageItemChange.Record, which is wrapping a Person.
        LatchedResultListener<StorageItemChange.Record> recordDeletionListener =
            LatchedResultListener.instance();

        localStorageAdapter.delete(record, StorageItemChange.Initiator.SYNC_ENGINE, recordDeletionListener);

        final StorageItemChange.Record result =
            recordDeletionListener.awaitTerminalEvent().assertNoError().assertResult().getResult();

        // Peel out the inner record out from the save result -
        // the record inside is the thing we tried to save,
        // that is, the record to change a person
        // That interior record should be identical to the thing that we tried to save.
        StorageItemChange<StorageItemChange.Record> recordOfDeletion =
            result.toStorageItemChange(storageItemChangeConverter);
        assertEquals(record, recordOfDeletion.item());

        // The record of the record itself has type DELETE, corresponding to our call to delete().
        assertEquals(StorageItemChange.Type.DELETE, recordOfDeletion.type());
    }
}
