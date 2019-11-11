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
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.MutationEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test the functionality of
 * {@link com.amplifyframework.datastore.DataStore#save(Model, ResultListener)} operation.
 */
public final class SQLiteStorageAdapterInstrumentedTest {

    private Context context;
    private SQLiteStorageAdapter sqLiteStorageAdapter;

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     * @throws InterruptedException when the waiting for setUp is interrupted.
     */
    @Before
    public void setUp() throws InterruptedException {
        deleteDatabase();

        context = ApplicationProvider.getApplicationContext();
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
                        waitForSetUp.countDown();
                    }
                });
        waitForSetUp.await();
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        deleteDatabase();
    }

    /**
     * Assert the construction of the SQLiteStorageHelper.
     * @throws ParseException when the date cannot be parsed.
     * @throws InterruptedException when the waiting for save is interrupted.
     */
    @SuppressWarnings("magicnumber")
    @Test
    public void saveModelInsertsData() throws ParseException, InterruptedException {
        final Person person = Person.builder()
                .firstName("Alan")
                .lastName("Turing")
                .age(41)
                .dob(SimpleDateFormat.getDateInstance(DateFormat.SHORT).parse("06/23/1912"))
                .build();
        final CountDownLatch waitForSave = new CountDownLatch(1);
        sqLiteStorageAdapter.save(person, new ResultListener<MutationEvent<Person>>() {
            @Override
            public void onResult(MutationEvent<Person> result) {
                assertEquals(person, result.data());
                waitForSave.countDown();
            }

            @Override
            public void onError(Throwable error) {
                fail(error.getMessage());
                waitForSave.countDown();
            }
        });
        waitForSave.await();

        final Cursor cursor = sqLiteStorageAdapter.getQueryAllCursor("Person");
        assertNotNull(cursor);
        if (cursor.moveToFirst()) {
            assertEquals("Alan",
                    cursor.getString(cursor.getColumnIndexOrThrow("firstName")));
            assertEquals("Turing",
                    cursor.getString(cursor.getColumnIndexOrThrow("lastName")));
            assertEquals(41,
                    cursor.getInt(cursor.getColumnIndexOrThrow("age")));
            assertEquals("06-23-1912",
                    cursor.getString(cursor.getColumnIndexOrThrow("dob")));
        }
        cursor.close();
    }

    private void deleteDatabase() {
        String databaseName = SQLiteStorageHelper
                .getInstance(context, Collections.emptySet())
                .getDatabaseName();
        context.deleteDatabase(databaseName);
    }
}
