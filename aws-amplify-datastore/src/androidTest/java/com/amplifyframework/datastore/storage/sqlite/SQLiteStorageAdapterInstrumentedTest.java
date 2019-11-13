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
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.storage.sqlite.model.Person;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the functionality of
 * {@link com.amplifyframework.datastore.DataStore#save(Model, ResultListener)} operation.
 */
public final class SQLiteStorageAdapterInstrumentedTest {

    private static final String TAG = "sqlite-instrumented-test";
    private static final long SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS = 1000;

    private Context context;
    private SQLiteStorageAdapter sqLiteStorageAdapter;

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     * @throws InterruptedException when the waiting for setUp is interrupted.
     */
    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        deleteDatabase();

        sqLiteStorageAdapter = SQLiteStorageAdapter.defaultInstance();

        final CountDownLatch waitForSetUp = new CountDownLatch(1);
        sqLiteStorageAdapter.setUp(context,
                Collections.singletonList(Person.class),
                new ResultListener<List<ModelSchema>>() {
                    @Override
                    public void onResult(List<ModelSchema> result) {
                        assertNotNull(result);
                        assertFalse(result.isEmpty());
                        waitForSetUp.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        fail(error.getMessage());
                    }
                });
        assertTrue(waitForSetUp.await(
                SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        deleteDatabase();
    }

    /**
     * Assert that save stores data in the SQLite database correctly.
     *
     * @throws ParseException when the date cannot be parsed.
     * @throws InterruptedException when the waiting for save is interrupted.
     */
    @SuppressWarnings("MagicNumber")
    @Test
    public void saveModelInsertsData() throws ParseException, InterruptedException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .build();
        saveModel(person);

        final Cursor cursor = sqLiteStorageAdapter.getQueryAllCursor("Person");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals("Alan",
                    cursor.getString(cursor.getColumnIndexOrThrow("firstName")));
            assertEquals("Turing",
                    cursor.getString(cursor.getColumnIndexOrThrow("lastName")));
            assertEquals(41,
                    cursor.getInt(cursor.getColumnIndexOrThrow("age")));
            assertEquals("Jun 23, 1912",
                    cursor.getString(cursor.getColumnIndexOrThrow("dob")));
        }
        cursor.close();
    }

    /**
     * Test querying the saved data in the SQLite database.
     *
     * @throws ParseException when the date cannot be parsed.
     * @throws InterruptedException when the waiting for save is interrupted.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithSingleItem() throws ParseException, InterruptedException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                .build();
        saveModel(person);

        final CountDownLatch waitForQuery = new CountDownLatch(1);
        sqLiteStorageAdapter.query(Person.class, new ResultListener<Iterator<Person>>() {
            @Override
            public void onResult(Iterator<Person> result) {
                try {
                    assertTrue(result.hasNext());
                    if (result.hasNext()) {
                        Person personQueried = result.next();
                        assertNotNull(personQueried);
                        Log.d(TAG, personQueried.toString());

                        assertEquals("Alan", person.getFirstName());
                        assertEquals("Turing", person.getLastName());
                        assertEquals(41, person.getAge());
                        assertEquals(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"),
                                person.getDob());
                        assertNotNull(person.getId());
                    }
                } catch (Exception exception) {
                    fail(exception.getMessage());
                }
                waitForQuery.countDown();
            }

            @Override
            public void onError(Throwable error) {
                fail(error.getMessage());
            }
        });

        assertTrue(waitForQuery.await(
                SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    /**
     * Test querying the saved data in the SQLite database.
     *
     * @throws ParseException when the date cannot be parsed.
     * @throws InterruptedException when the waiting for save is interrupted.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void querySavedDataWithMultipleItems() throws ParseException, InterruptedException {
        final Set<Person> savedModels = new HashSet<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Person person = Person.builder()
                    .firstName("firstNamePrefix:" + counter)
                    .lastName("lastNamePrefix:" + counter)
                    .age(counter)
                    .dob(SimpleDateFormat.getDateInstance().parse("Jun 23, 1912"))
                    .build();
            saveModel(person);
            savedModels.add(person);
        }

        final CountDownLatch waitForQuery = new CountDownLatch(numModels);
        sqLiteStorageAdapter.query(Person.class, new ResultListener<Iterator<Person>>() {
            @Override
            public void onResult(Iterator<Person> result) {
                assertNotNull(result);

                while (result.hasNext()) {
                    final Person person = result.next();
                    assertNotNull(person);
                    assertTrue("Unable to find expected item in the storage adapter.",
                            savedModels.contains(person));
                    waitForQuery.countDown();
                }
            }

            @Override
            public void onError(Throwable error) {
                fail(error.getMessage());
            }
        });

        assertTrue(waitForQuery.await(
                SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    private void saveModel(Person person) throws InterruptedException {
        final CountDownLatch waitForSave = new CountDownLatch(1);
        sqLiteStorageAdapter.save(person, new ResultListener<MutationEvent<Person>>() {
            @Override
            public void onResult(MutationEvent<Person> result) {
                assertEquals(person, result.data());
                waitForSave.countDown();
            }

            @Override
            public void onError(Throwable error) {
                assertNotNull(error);
                Log.e(TAG, error.toString());
                Log.e(TAG, error.getCause().getMessage());
                fail(error.getMessage());
            }
        });
        assertTrue(waitForSave.await(
                SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    private void deleteDatabase() {
        context.deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
