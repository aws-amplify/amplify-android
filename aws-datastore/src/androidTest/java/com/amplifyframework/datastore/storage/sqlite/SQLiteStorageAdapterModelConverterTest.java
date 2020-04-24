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
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class SQLiteStorageAdapterModelConverterTest {

    private static final Date MAY_THE_FOURTH = new Date(1588627200000L);

    private SynchronousStorageAdapter adapter;

    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

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

        final List<Todo> result = this.adapter.query(Todo.class, Todo.ID.eq(todo.getId()));
        assertEquals(result.size(), 1);

        final Todo queriedTodo = result.get(0);
        assertNotNull(queriedTodo);

        // Test common scalar types
        assertEquals(queriedTodo.getTitle(), todo.getTitle());
        assertEquals(queriedTodo.getContent(), todo.getContent());
        assertEquals(queriedTodo.getDuplicate(), todo.getDuplicate());
        assertEquals(queriedTodo.getPriority(), todo.getPriority());
        assertEquals(queriedTodo.getHoursSpent(), todo.getHoursSpent());

        // Test date scalars
        // TODO fix tests once new Date/Time handling is done
        // assertEquals(queriedTodo.getCreatedAt(), todo.getCreatedAt());
        // assertEquals(queriedTodo.getDueDate(), todo.getDueDate());
        assertEquals(todo.getLastUpdated(), todo.getLastUpdated());

        // Test status enum
        assertEquals(queriedTodo.getStatus(), todo.getStatus());

        // Test embedded TodoOwner
        assertNotNull(queriedTodo.getOwner());
        assertEquals(queriedTodo.getOwner().getName(), todo.getOwner().getName());
        assertEquals(queriedTodo.getOwner().getEmail(), todo.getOwner().getEmail());

        // Test embedded tags (String[])
        assertNotNull(queriedTodo.getTags());
        assertEquals(queriedTodo.getTags().size(), todo.getTags().size());
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
