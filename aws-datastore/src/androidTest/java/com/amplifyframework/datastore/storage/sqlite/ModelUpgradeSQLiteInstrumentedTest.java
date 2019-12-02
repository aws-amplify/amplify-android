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
import android.os.StrictMode;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.personcar.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.personcar.RandomVersionModelProvider;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} with model update operations.
 */
public final class ModelUpgradeSQLiteInstrumentedTest {
    private static final long SQLITE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private Context context;
    private SQLiteStorageAdapter sqliteStorageAdapter;
    private AmplifyCliGeneratedModelProvider modelProvider;
    private RandomVersionModelProvider modelProviderThatUpgradesVersion;

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

        modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();
        modelProviderThatUpgradesVersion = RandomVersionModelProvider.singletonInstance();
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException On failure to terminate adapter
     */
    @After
    public void tearDown() throws DataStoreException {
        sqliteStorageAdapter.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Asserts if the model version change updates the new version in local storage.
     * @throws DataStoreException On failure to terminate adapter
     */
    @Test
    public void modelVersionStoredCorrectlyBeforeAndAfterUpgrade() throws DataStoreException {
        // Initialize StorageAdapter with models
        LatchedResultListener<List<ModelSchema>> setupListener =
                LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);
        sqliteStorageAdapter.initialize(context, setupListener);

        // Assert if initialize succeeds.
        List<ModelSchema> modelSchemaList =
                setupListener.awaitTerminalEvent().awaitResult();
        assertNotNull(modelSchemaList);
        assertFalse(modelSchemaList.isEmpty());

        // Assert if version is stored correctly
        String expectedVersion = modelProvider.version();
        PersistentModelVersion persistentModelVersion =
                PersistentModelVersion
                        .fromLocalStorage(sqliteStorageAdapter)
                        .blockingGet()
                        .next();
        String actualVersion = persistentModelVersion.getVersion();
        assertEquals(expectedVersion, actualVersion);

        // Terminate storage adapter and create a new storage adapter with
        // a model provider that upgrades version to mimic restartability with
        // version update.
        sqliteStorageAdapter.terminate();
        sqliteStorageAdapter = null;
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProviderThatUpgradesVersion);

        // Now, initialize storage adapter with the new models
        setupListener = LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.initialize(context, setupListener);
        modelSchemaList = setupListener.awaitTerminalEvent().awaitResult();
        assertNotNull(modelSchemaList);
        assertFalse(modelSchemaList.isEmpty());

        // Check if the new version is stored in local storage.
        expectedVersion = modelProviderThatUpgradesVersion.version();
        persistentModelVersion = PersistentModelVersion
                        .fromLocalStorage(sqliteStorageAdapter)
                        .blockingGet()
                        .next();
        actualVersion = persistentModelVersion.getVersion();
        assertEquals(expectedVersion, actualVersion);
    }
}
