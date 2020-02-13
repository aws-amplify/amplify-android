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
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testutils.Await;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} operations.
 */
@SuppressWarnings("DesignForExtension") // Utility methods shouldn't be overwritten
public abstract class StorageAdapterInstrumentedTestBase {
    private static final long SQLITE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private SQLiteStorageAdapter sqliteStorageAdapter;
    private Context context;

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
     * @throws DataStoreException If initialization of storage adapter fails
     */
    @Before
    public void setUp() throws DataStoreException {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);

        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);
        List<ModelSchema> setupResults = Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<List<ModelSchema>> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.initialize(context, onResult, onError)
        );

        List<Class<? extends Model>> expectedModels = new ArrayList<>(modelProvider.models());
        expectedModels.add(StorageItemChange.Record.class); // Internal
        expectedModels.add(PersistentModelVersion.class); // Internal
        assertEquals(expectedModels.size(), setupResults.size());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @After
    public void tearDown() throws DataStoreException {
        sqliteStorageAdapter.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    <T extends Model> void saveModel(@NonNull T model) throws DataStoreException {
        //noinspection ConstantConditions
        saveModel(model, null);
    }

    <T extends Model> void saveModel(
            @NonNull T model, @NonNull QueryPredicate predicate) throws DataStoreException {
        Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<StorageItemChange.Record> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.save(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }

    <T extends Model> DataStoreException saveModelExpectingError(@NonNull T model) {
        //noinspection ConstantConditions
        return saveModelExpectingError(model, null);
    }

    <T extends Model> DataStoreException saveModelExpectingError(
            @NonNull T model, @NonNull QueryPredicate predicate) {
        return Await.error(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<StorageItemChange.Record> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.save(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }

    <T extends Model> Set<T> queryModel(@NonNull Class<T> modelClass) throws DataStoreException {
        //noinspection ConstantConditions
        return queryModel(modelClass, null);
    }

    <T extends Model> Set<T> queryModel(
            @NonNull Class<T> modelClass, @NonNull QueryPredicate predicate) throws DataStoreException {
        Iterator<T> resultIterator = Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<Iterator<T>> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.query(modelClass, predicate, onResult, onError)
        );
        final Set<T> resultSet = new HashSet<>();
        while (resultIterator.hasNext()) {
            resultSet.add(resultIterator.next());
        }
        return resultSet;
    }

    <T extends Model> void deleteModel(@NonNull T model) throws DataStoreException {
        //noinspection ConstantConditions
        deleteModel(model, null);
    }

    <T extends Model> void deleteModel(
            @NonNull T model, @NonNull QueryPredicate predicate) throws DataStoreException {
        Await.result(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<StorageItemChange.Record> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.delete(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }

    <T extends Model> DataStoreException deleteModelExpectingError(@NonNull T model) {
        //noinspection ConstantConditions
        return deleteModelExpectingError(model, null);
    }

    <T extends Model> DataStoreException deleteModelExpectingError(
            @NonNull T model,
            @NonNull QueryPredicate predicate) {
        return Await.error(
            SQLITE_OPERATION_TIMEOUT_MS,
            (Consumer<StorageItemChange.Record> onResult, Consumer<DataStoreException> onError) ->
                sqliteStorageAdapter.delete(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }
}
