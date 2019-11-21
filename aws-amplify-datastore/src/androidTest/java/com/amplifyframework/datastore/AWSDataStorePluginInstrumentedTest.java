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

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testmodels.AmplifyCliGeneratedModelProvider;

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

    private static final String TAG = AWSDataStorePluginInstrumentedTest.class.getSimpleName();
    private static final long DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS = 1000000;
    private static Context context;
    private static AWSDataStorePlugin awsDataStorePlugin;

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
     * Setup the Android application context.
     */
    @BeforeClass
    public static void configureAmplify() {
        context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration amplifyConfiguration = new AmplifyConfiguration();
        amplifyConfiguration.populateFromConfigFile(context);
        awsDataStorePlugin = AWSDataStorePlugin.singleton();
        Amplify.addPlugin(awsDataStorePlugin);
        Amplify.configure(amplifyConfiguration, context);
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @After
    public void tearDown() {
        awsDataStorePlugin.terminate();
        deleteDatabase();
    }

    /**
     * Test adding, configuring and setting up AWSDataStorePlugin.
     * @throws InterruptedException when initialize times out.
     */
    @Test
    public void testSetUp() throws InterruptedException {
        final CountDownLatch waitForSetUp = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<List<ModelSchema>> result = new AtomicReference<>();
        Amplify.DataStore.initialize(
                context,
                AmplifyCliGeneratedModelProvider.singletonInstance(),
                new ResultListener<List<ModelSchema>>() {
                    @Override
                    public void onResult(List<ModelSchema> modelSchemaList) {
                        result.set(modelSchemaList);
                        waitForSetUp.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, Log.getStackTraceString(throwable));
                        error.set(throwable);
                        waitForSetUp.countDown();
                    }
                });
        assertTrue(waitForSetUp.await(
                DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS));
        assertNotNull("Expecting a non-null ModelSchema list", result.get());
        assertFalse("Expecting a non-empty ModelSchema list", result.get().isEmpty());
        assertNull("Expecting no exception to be thrown from initialize", error.get());
    }

    private void deleteDatabase() {
        context.deleteDatabase("AmplifyDataStore.db");
    }
}
