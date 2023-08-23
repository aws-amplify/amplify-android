/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.testmodels.personcar.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.personcar.RandomVersionModelProvider;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.util.Empty;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} with model update operations.
 */
public final class ModelUpgradeSQLiteInstrumentedTest {
    private static final long SQLITE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private SQLiteStorageAdapter sqliteStorageAdapter;
    private AmplifyCliGeneratedModelProvider modelProvider;
    private RandomVersionModelProvider modelProviderThatUpgradesVersion;
    private SchemaRegistry schemaRegistry;

    private Context context;

    /**
     * Enable strict mode for catching SQLite leaks.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     *
     * @throws AmplifyException may throw {@link AmplifyException} from {@link SchemaRegistry#register(Set)}
     */
    @Before
    public void setUp() throws AmplifyException {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);

        modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();
        modelProviderThatUpgradesVersion = RandomVersionModelProvider.singletonInstance();

        schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.clear();
        schemaRegistry.register(modelProvider.models());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException On failure to terminate adapter
     */
    @After
    public void tearDown() throws DataStoreException {
        sqliteStorageAdapter.terminate();
        context.deleteDatabase(DATABASE_NAME);
        schemaRegistry.clear();
    }

    /**
     * Asserts if the model version change updates the new version in local storage.
     * @throws AmplifyException On failure to load schema into model schema registry,
     *                          or on failure to terminate adapter
     */
    @Test
    public void modelVersionStoredCorrectlyBeforeAndAfterUpgrade() throws AmplifyException {
        // Initialize StorageAdapter with models
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider);
        List<ModelSchema> firstResults = Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<List<ModelSchema>> onResult, Consumer<DataStoreException> onError) -> {
                try {
                    sqliteStorageAdapter.initialize(context, onResult, onError,
                            DataStoreConfiguration.builder()
                                    .syncInterval(2L, TimeUnit.MINUTES)
                                    .build());
                } catch (DataStoreException exception) {
                    Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore").warn(exception.toString());
                }
            }
        );
        // Assert if initialize succeeds.
        assertFalse(Empty.check(firstResults));

        // Assert if version is stored correctly
        String expectedVersion =
            CompoundModelProvider.of(SystemModelsProviderFactory.create(), modelProvider)
                .version();
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

        sqliteStorageAdapter =
            SQLiteStorageAdapter.forModels(schemaRegistry, modelProviderThatUpgradesVersion);

        // Now, initialize storage adapter with the new models
        List<ModelSchema> secondResults = Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<List<ModelSchema>> onResult, Consumer<DataStoreException> onError) -> {
                try {
                    sqliteStorageAdapter.initialize(context, onResult, onError,
                            DataStoreConfiguration.builder()
                            .syncInterval(2L, TimeUnit.MINUTES)
                            .build());
                } catch (DataStoreException exception) {
                    Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore").warn(exception.toString());
                }
            }
        );
        assertFalse(Empty.check(secondResults));

        // Check if the new version is stored in local storage.
        expectedVersion =
            CompoundModelProvider.of(SystemModelsProviderFactory.create(), modelProviderThatUpgradesVersion)
                .version();
        persistentModelVersion = PersistentModelVersion
                .fromLocalStorage(sqliteStorageAdapter)
                .blockingGet()
                .next();
        actualVersion = persistentModelVersion.getVersion();
        assertEquals(expectedVersion, actualVersion);
    }
}
