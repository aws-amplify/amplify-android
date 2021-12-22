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

import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.ecommerce.AmplifyModelProvider;
import com.amplifyframework.testmodels.ecommerce.Customer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} operations with types that use a custom primary key.
 */
public final class SQLiteStorageAdapterCustomPrimaryKeyTest {

    private SynchronousStorageAdapter adapter;

    /**
     * Enable Android Strict Mode, to help catch common errors while using SQLite,
     * such as forgetting to close a database (from source).
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Remove any old SQLite database files. Setup a new storage adapter, which is able
     * to warehouse the ecommerce (Customer, Item, Order) family of models.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Close the storage adapter and delete any SQLite database files that it may
     * have left.
     */
    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Validate that a model with a custom primary key can be updated.
     * @throws DataStoreException on failure to create or update the model.
     */
    @Test
    public void updateModelWithCustomPrimaryKey() throws DataStoreException {
        // Create a model
        Customer jeff = Customer.builder()
                .email("jeff@amazon.com")
                .username("jeff")
                .build();
        adapter.save(jeff);

        // Then update it
        Customer updatedJeff = jeff.copyOfBuilder()
                .username("jeffbezos")
                .build();
        adapter.save(updatedJeff);
    }
}
