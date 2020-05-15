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

import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.todo.AmplifyModelProvider;
import com.amplifyframework.testmodels.todo.Todo;
import com.amplifyframework.testmodels.todo.TodoOwner;
import com.amplifyframework.testmodels.todo.TodoStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This is a basic test to ensure that an {@link Model} class can be saved and queried
 * into the {@link SQLiteStorageAdapter}. This is one of the original tests that was written
 * for the {@link SQLiteStorageAdapter}, when this level of basic functionality was
 * in question. Now, the test may serve as a "smoke test."
 */
public final class SQLiteStorageAdapterModelConverterTest {
    private static final AWSDateTime MAY_THE_FOURTH = new AWSDateTime("2020-05-04T14:20:00-07:00");

    private SynchronousStorageAdapter adapter;

    /**
     * Enable Android Strict Model. This helps catch common errors while using a SQLite database,
     * such as forgetting to close a handle to it (in the source code).
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Delete any existing SQLite database files, and create a new storage adapter that
     * is able to warehouse the To-do family of models.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Close the adapter and delete any database files that it may have left behind.
     */
    @After
    public void teardown() {
        if (adapter != null) {
            TestStorageAdapter.cleanup(adapter);
        }
    }

    /**
     * Assert that save a model instance with fields of different types in the SQLite database correctly.
     * Then after save succeeds, query it and compare the values against the original model.
     *
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelWithAllTypesThenQuery() throws DataStoreException {
        final Todo todo = createTestTodoModel();
        this.adapter.save(todo);

        final List<Todo> result = this.adapter.query(Todo.class, Where.id(todo.getId()));
        assertEquals(result.size(), 1);

        final Todo queriedTodo = result.get(0);
        assertNotNull(queriedTodo);
        assertEquals(todo, queriedTodo);
    }

    private Todo createTestTodoModel() {
        TodoOwner owner = TodoOwner.builder()
                .name("John Doe")
                .email("email@example.com")
                .build();
        return Todo.builder()
                .title("Title")
                .content("Content")
                .status(TodoStatus.InProgress)
                .createdAt(MAY_THE_FOURTH)
                .duplicate(false)
                .owner(owner)
                .hoursSpent(3.5F)
                .tags(Arrays.asList("tag1", "tag2", "tag3"))
                .lastUpdated(System.currentTimeMillis())
                .build();
    }
}
