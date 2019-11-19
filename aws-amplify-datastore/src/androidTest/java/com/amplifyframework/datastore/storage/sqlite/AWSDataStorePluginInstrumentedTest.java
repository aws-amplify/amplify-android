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
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.testutils.model.AmplifyCliGeneratedModelProvider;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the functions of {@link com.amplifyframework.datastore.AWSDataStorePlugin}.
 */
public final class AWSDataStorePluginInstrumentedTest {

    private static final long DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS = 1000;

    private static Context context;

    /**
     * Setup the Android application context.
     */
    @BeforeClass
    public static void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        deleteDatabase();
    }

    /**
     * Test adding, configuring and setting up AWSDataStorePlugin.
     * @throws InterruptedException when setUp times out.
     */
    @Test
    public void testSetUp() throws InterruptedException {
        AWSDataStorePlugin awsDataStorePlugin = new AWSDataStorePlugin();
        Amplify.addPlugin(awsDataStorePlugin);
        Amplify.configure(context);

        final CountDownLatch waitForSetUp = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<List<ModelSchema>> result = new AtomicReference<>();
        Amplify.DataStore.setUp(
                context,
                AmplifyCliGeneratedModelProvider.getInstance(),
                new ResultListener<List<ModelSchema>>() {
                    @Override
                    public void onResult(List<ModelSchema> modelSchemaList) {
                        result.set(modelSchemaList);
                        waitForSetUp.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        waitForSetUp.countDown();
                    }
                });
        assertTrue(waitForSetUp.await(
                DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
        assertNotNull("Expecting a non-null ModelSchema list", result.get());
        assertFalse("Expecting a non-empty ModelSchema list", result.get().isEmpty());
        assertNull("Expecting no exception to be thrown from setUp", error.get());
    }

    private void deleteDatabase() {
        context.deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
