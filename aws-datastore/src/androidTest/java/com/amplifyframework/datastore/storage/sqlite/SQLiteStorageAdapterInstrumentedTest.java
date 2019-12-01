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
    private SQLiteStorageAdapter sqLiteStorageAdapter;

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
        sqLiteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);

        LatchedResultListener<List<ModelSchema>> setupListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);

        sqLiteStorageAdapter.initialize(context, setupListener);

        List<ModelSchema> modelSchemaList =
            setupListener.awaitTerminalEvent().assertNoError().getResult();
        assertNotNull(modelSchemaList);
        assertFalse(modelSchemaList.isEmpty());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @After
    public void tearDown() {
        sqLiteStorageAdapter.terminate();
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

        final Cursor cursor = sqLiteStorageAdapter.getQueryAllCursor("Person");
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

        final Cursor cursor = sqLiteStorageAdapter.getQueryAllCursor("Person");
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

        final Cursor cursor = sqLiteStorageAdapter.getQueryAllCursor("Car");
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
        sqLiteStorageAdapter.save(car, StorageItemChange.Initiator.DATA_STORE_API, carSaveListener);

        Throwable actualError = carSaveListener.awaitTerminalEvent().assertError().getError();
        assertNotNull(actualError);
        assertNotNull(actualError.getCause());
        assertNotNull(actualError.getCause().getMessage());
        assertTrue(actualError.getCause().getMessage().contains(expectedError));
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

        Set<Person> expectedPersons = new HashSet<>();
        expectedPersons.add(savedModels.get(1));
        expectedPersons.add(savedModels.get(4));
        expectedPersons.add(savedModels.get(5));
        expectedPersons.add(savedModels.get(6));

        Set<Person> actualPersons = new HashSet<>();
        while (result.hasNext()) {
            final Person person = result.next();
            assertNotNull(person);
            assertTrue("Unable to find expected item in the storage adapter.",
                    savedModels.contains(person));
            actualPersons.add(person);
        }

        assertEquals(expectedPersons, actualPersons);
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
        final Set<Person> savedModels = new HashSet<>();
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
        Set<Integer> ages = new HashSet<>();
        while (result.hasNext()) {
            final Person person = result.next();
            assertNotNull(person);
            assertTrue("Unable to find expected item in the storage adapter.",
                    savedModels.contains(person));
            ages.add(person.getAge());
        }
        assertEquals(2, ages.size());
        assertTrue(ages.contains(4));
        assertTrue(ages.contains(7));
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
        sqLiteStorageAdapter.save(model, StorageItemChange.Initiator.DATA_STORE_API, saveListener);
        return saveListener.awaitTerminalEvent().assertNoError().getResult()
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
        sqLiteStorageAdapter.query(modelClass, predicate, queryResultListener);
        return queryResultListener.awaitTerminalEvent().assertNoError().getResult();
    }

    private <T extends Model> T deleteModel(@NonNull T model) {
        LatchedResultListener<StorageItemChange.Record> deleteListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqLiteStorageAdapter.delete(model, StorageItemChange.Initiator.DATA_STORE_API, deleteListener);
        return deleteListener.awaitTerminalEvent().assertNoError().getResult()
                .<T>toStorageItemChange(new GsonStorageItemChangeConverter())
                .item();
    }
}
