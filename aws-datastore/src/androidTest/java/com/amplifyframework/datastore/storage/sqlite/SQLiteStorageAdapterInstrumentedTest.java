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

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.personcar.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.personcar.Car;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.amplifyframework.core.model.query.predicate.QueryPredicateOperation.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterInstrumentedTest {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore:test");
    private static final long SQLITE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private Context context;
    private SQLiteStorageAdapter sqliteStorageAdapter;

    /**
     * Enable strict mode for catching SQLite leaks.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build());
    }

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);

        ModelProvider modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);

        LatchedResultListener<List<ModelSchema>> setupListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);

        sqliteStorageAdapter.initialize(context, setupListener);

        List<ModelSchema> setupResults = setupListener.awaitResult();

        List<Class<? extends Model>> expectedModels = new ArrayList<>(modelProvider.models());
        expectedModels.add(StorageItemChange.Record.class); // Internal
        expectedModels.add(PersistentModelVersion.class); // Internal
        assertEquals(expectedModels.size(), setupResults.size());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @After
    public void tearDown() {
        sqliteStorageAdapter.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Assert that save stores item in the SQLite database correctly.
     *
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelUpdatesData() {
        // Triggers an insert
        final Person raphael = Person.builder()
                .firstName("Raphael")
                .lastName("Kim")
                .age(23)
                .build();
        saveModel(raphael);

        // Triggers an update
        final Person realRaph = raphael.newBuilder()
                .firstName("Raph")
                .build();
        saveModel(realRaph);

        // Get the person record from the database
        List<Person> people = new ArrayList<>();
        Iterator<Person> iterator = queryModel(Person.class);
        while (iterator.hasNext()) {
            people.add(iterator.next());
        }
        assertEquals(1, people.size());
        Person possiblyRaph = people.get(0);

        assertEquals(realRaph, possiblyRaph);
    }

    /**
     * Assert that save stores data in the SQLite database correctly.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelInsertsData() throws ParseException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .relationship(MaritalStatus.single)
                .build();
        assertEquals(person, saveModel(person));

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("Person");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals("Alan",
                    cursor.getString(cursor.getColumnIndexOrThrow("first_name")));
            assertEquals("Turing",
                    cursor.getString(cursor.getColumnIndexOrThrow("last_name")));
            assertEquals(41,
                    cursor.getInt(cursor.getColumnIndexOrThrow("age")));
            assertEquals("Jun 23, 1912",
                    cursor.getString(cursor.getColumnIndexOrThrow("dob")));
        }
        cursor.close();
    }

    /**
     * Assert that save stores data in the SQLite database correctly
     * even if some optional values are null.
     *
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelWithNullsInsertsData() {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .build();
        assertEquals(person, saveModel(person));

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("Person");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals("Alan",
                    cursor.getString(cursor.getColumnIndexOrThrow("first_name")));
            assertEquals("Turing",
                    cursor.getString(cursor.getColumnIndexOrThrow("last_name")));
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("age")));
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("dob")));
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("relationship")));
        }
        cursor.close();
    }

    /**
     * Assert that save stores foreign key in the SQLite database correctly.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelWithValidForeignKey() throws ParseException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .relationship(MaritalStatus.single)
                .build();
        saveModel(person);

        final Car car = Car.builder()
                .vehicleModel("1940 Packard Six")
                .owner(person)
                .build();
        saveModel(car);

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("Car");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals("1940 Packard Six",
                    cursor.getString(cursor.getColumnIndexOrThrow("vehicle_model")));
            assertEquals(person.getId(),
                    cursor.getString(cursor.getColumnIndexOrThrow("carOwnerId")));
        }
        cursor.close();
    }

    /**
     * Assert that foreign key constraint is enforced.
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelWithInvalidForeignKey() throws ParseException {
        final String expectedError = "FOREIGN KEY constraint failed";

        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .relationship(MaritalStatus.single)
                .build();
        saveModel(person);

        final Car car = Car.builder()
                .vehicleModel("1940 Packard Six")
                .owner(Person.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .build())
                .build();

        LatchedResultListener<StorageItemChange.Record> carSaveListener =
                LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.save(car, StorageItemChange.Initiator.DATA_STORE_API, carSaveListener);

        Throwable actualError = carSaveListener.awaitError();
        assertNotNull(actualError.getCause());
        assertNotNull(actualError.getCause().getMessage());
        assertTrue(actualError.getCause().getMessage().contains(expectedError));
    }

    /**
     * Test save with SQL injection.
     */
    @Test
    public void saveModelWithMaliciousInputs() {
        final Person person = Person.builder()
                .firstName("Jane'); DROP TABLE Person; --")
                .lastName("Doe")
                .build();
        saveModel(person);

        Iterator<Person> result = queryModel(Person.class);
        assertTrue(result.hasNext());
        assertEquals(person, result.next());
    }

    /**
     * Test querying the saved item in the SQLite database.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithSingleItem() throws ParseException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                .relationship(MaritalStatus.single)
                .build();
        assertEquals(person, saveModel(person));

        Iterator<Person> result = queryModel(Person.class);
        assertNotNull(result);
        assertTrue(result.hasNext());
        Person queriedPerson = result.next();
        assertNotNull(queriedPerson);
        LOG.debug(queriedPerson.toString());
        assertEquals(person, queriedPerson);
    }

    /**
     * Test querying the saved item in the SQLite database.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithMultipleItems() throws ParseException {
        final Set<Person> savedModels = new HashSet<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Person person = Person.builder()
                    .firstName("firstNamePrefix:" + counter)
                    .lastName("lastNamePrefix:" + counter)
                    .age(counter)
                    .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                    .relationship(MaritalStatus.single)
                    .build();
            saveModel(person);
            savedModels.add(person);
        }

        Iterator<Person> result = queryModel(Person.class);
        int count = 0;
        while (result.hasNext()) {
            final Person person = result.next();
            assertNotNull(person);
            assertTrue("Unable to find expected item in the storage adapter.",
                    savedModels.contains(person));
            count++;
        }
        assertEquals(numModels, count);
    }

    /**
     * Test that querying the saved item with a foreign key
     * also populates that instance variable with object.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithForeignKey() throws ParseException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .relationship(MaritalStatus.single)
                .build();

        final Car car = Car.builder()
                .vehicleModel("1940 Packard Six")
                .owner(person)
                .build();

        saveModel(person);
        saveModel(car);

        Iterator<Car> result = queryModel(Car.class);
        assertNotNull(result);
        assertTrue(result.hasNext());

        final Person queriedCarOwner = result.next().getOwner();
        assertNotNull(queriedCarOwner);
        assertEquals(person.getId(), queriedCarOwner.getId());
        assertEquals(person.getFirstName(), queriedCarOwner.getFirstName());
        assertEquals(person.getLastName(), queriedCarOwner.getLastName());
        assertEquals(person.getAge(), queriedCarOwner.getAge());
        assertEquals(person.getDob(), queriedCarOwner.getDob());
        assertEquals(person.getRelationship(), queriedCarOwner.getRelationship());
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithNumericalPredicates() throws ParseException {
        final List<Person> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Person person = Person.builder()
                    .firstName("firstNamePrefix:" + counter)
                    .lastName("lastNamePrefix:" + counter)
                    .age(counter)
                    .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                    .relationship(MaritalStatus.single)
                    .build();
            saveModel(person);
            savedModels.add(person);
        }

        // 1, 4, 5, 6
        QueryPredicate predicate = Person.AGE.ge(4).and(Person.AGE.lt(7))
                .or(Person.AGE.eq(1).and(Person.AGE.ne(7)));
        Iterator<Person> result = queryModel(Person.class, predicate);

        Set<Person> expectedPeople = new HashSet<>();
        expectedPeople.add(savedModels.get(1));
        expectedPeople.add(savedModels.get(4));
        expectedPeople.add(savedModels.get(5));
        expectedPeople.add(savedModels.get(6));

        Set<Person> actualPeople = new HashSet<>();
        while (result.hasNext()) {
            final Person person = result.next();
            assertNotNull(person);
            assertTrue("Unable to find expected item in the storage adapter.",
                    savedModels.contains(person));
            actualPeople.add(person);
        }

        assertEquals(expectedPeople, actualPeople);
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     *
     * @throws ParseException when the date cannot be parsed.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithStringPredicates() throws ParseException {
        final List<Person> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Person person = Person.builder()
                    .firstName(counter + "-first")
                    .lastName(counter + "-last")
                    .age(counter)
                    .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                    .relationship(MaritalStatus.single)
                    .build();
            saveModel(person);
            savedModels.add(person);
        }

        // 4, 7
        QueryPredicate predicate = Person.FIRST_NAME.beginsWith("4")
                .or(Person.LAST_NAME.beginsWith("7"))
                .or(Person.LAST_NAME.beginsWith("9"))
                .and(not(Person.AGE.gt(8)));
        Iterator<Person> result = queryModel(Person.class, predicate);

        Set<Person> expectedPeople = new HashSet<>();
        expectedPeople.add(savedModels.get(4));
        expectedPeople.add(savedModels.get(7));

        Set<Person> actualPeople = new HashSet<>();
        while (result.hasNext()) {
            final Person person = result.next();
            assertNotNull(person);
            assertTrue("Unable to find expected item in the storage adapter.",
                    savedModels.contains(person));
            actualPeople.add(person);
        }
        assertEquals(expectedPeople, actualPeople);
    }

    /**
     * Test querying with predicate condition on connected model.
     */
    @Test
    public void querySavedDataWithPredicatesOnForeignKey() {
        final Person person = Person.builder()
                .firstName("Jane")
                .lastName("Doe")
                .build();
        saveModel(person);

        final Car car = Car.builder()
                .vehicleModel("Toyota Prius")
                .owner(person)
                .build();
        saveModel(car);

        QueryPredicate predicate = Person.FIRST_NAME.eq("Jane");
        Iterator<Car> result = queryModel(Car.class, predicate);
        assertTrue(result.hasNext());
        assertEquals(car, result.next());
    }

    /**
     * Test query with SQL injection.
     */
    @Test
    public void queryWithMaliciousPredicates() {
        final Person jane = Person.builder()
                .firstName("Jane")
                .lastName("Doe")
                .build();
        saveModel(jane);

        QueryPredicate predicate = Person.FIRST_NAME.eq("Jane; DROP TABLE Person; --");
        Iterator<Person> resultOfMaliciousQuery = queryModel(Person.class, predicate);
        assertFalse(resultOfMaliciousQuery.hasNext());

        Iterator<Person> resultAfterMaliciousQuery = queryModel(Person.class);
        assertTrue(resultAfterMaliciousQuery.hasNext());
        assertEquals(jane, resultAfterMaliciousQuery.next());
    }

    /**
     * Assert that save stores item in the SQLite database correctly.
     *
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void deleteModelDeletesData() {
        // Triggers an insert
        final Person raphael = Person.builder()
                .firstName("Raphael")
                .lastName("Kim")
                .age(23)
                .build();
        saveModel(raphael);

        // Triggers a delete
        deleteModel(raphael);

        // Get the person record from the database
        Iterator<Person> iterator = queryModel(Person.class);
        assertFalse(iterator.hasNext());
    }

    private <T extends Model> T saveModel(@NonNull T model) {
        LatchedResultListener<StorageItemChange.Record> saveListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.save(model, StorageItemChange.Initiator.DATA_STORE_API, saveListener);
        return saveListener.awaitResult()
            .<T>toStorageItemChange(new GsonStorageItemChangeConverter())
            .item();
    }

    private <T extends Model> Iterator<T> queryModel(@NonNull Class<T> modelClass) {
        return queryModel(modelClass, null);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Model> Iterator<T> queryModel(@NonNull Class<T> modelClass,
                                                     @Nullable QueryPredicate predicate) {
        LatchedResultListener<Iterator<T>> queryResultListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.query(modelClass, predicate, queryResultListener);
        return queryResultListener.awaitResult();
    }

    private <T extends Model> T deleteModel(@NonNull T model) {
        LatchedResultListener<StorageItemChange.Record> deleteListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.delete(model, StorageItemChange.Initiator.DATA_STORE_API, deleteListener);
        return deleteListener.awaitResult()
                .<T>toStorageItemChange(new GsonStorageItemChangeConverter())
                .item();
    }
}
