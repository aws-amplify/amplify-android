/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Objects;

/**
 * A test utility to create instances of {@link SynchronousStorageAdapter}, and cleanup
 * the system when done using those instances.
 */
final class TestStorageAdapter {
    @SuppressWarnings("checkstyle:all") private TestStorageAdapter() {}

    /**
     * Creates an instance of the {@link SynchronousStorageAdapter}, which has been initialized
     * so that it can be used with the given models. The {@link SynchronousStorageAdapter}
     * is backed by an {@link SQLiteStorageAdapter}. The caller of this method
     * should do due diligence to ensure that any resources created by
     * {@link SQLiteStorageAdapter#initialize(Context, Consumer, Consumer)} have been cleaned up.
     * @return An initialized instance of the {@link SynchronousStorageAdapter}
     */
    static SynchronousStorageAdapter create(ModelProvider modelProvider) {
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        try {
            modelSchemaRegistry.load(modelProvider.models());
        } catch (AmplifyException modelSchemaLoadingFailure) {
            throw new RuntimeException(modelSchemaLoadingFailure);
        }
        SQLiteStorageAdapter sqLiteStorageAdapter =
            SQLiteStorageAdapter.forModels(modelSchemaRegistry, modelProvider);

        SynchronousStorageAdapter synchronousStorageAdapter =
            SynchronousStorageAdapter.instance(sqLiteStorageAdapter);
        Context context = ApplicationProvider.getApplicationContext();
        try {
            synchronousStorageAdapter.initialize(context);
        } catch (DataStoreException initializationFailure) {
            throw new RuntimeException(initializationFailure);
        }
        return synchronousStorageAdapter;
    }

    /**
     * Clean-up resources used by the test storage adapter.
     */
    static void cleanup() {
        deleteDatabase();
    }

    /**
     * Discontinue use of the provided adapter, and cleanup any resources it may
     * have created.
     * @param synchronousStorageAdapter A storage adapter which was created by this utility
     */
    static void cleanup(@NonNull SynchronousStorageAdapter synchronousStorageAdapter) {
        Objects.requireNonNull(synchronousStorageAdapter);
        try {
            synchronousStorageAdapter.terminate();
        } catch (DataStoreException terminationFailure) {
            throw new RuntimeException(terminationFailure);
        }
        deleteDatabase();
    }

    private static void deleteDatabase() {
        ApplicationProvider.getApplicationContext()
            .deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
