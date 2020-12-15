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

import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the delete functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterDeleteTest {
    private SynchronousStorageAdapter adapter;

    /**
     * Enables strict mode, for the purpose of catching some common errors while using
     * a SQL data-base, such as forgetting to close it when done.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Clear the storage adapter, and then provision a new one that will allow us
     * to store the Comments-Blog models.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Close the storage adapter, and cleanup any database files it left.
     */
    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Assert that delete deletes item in the SQLite database correctly.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelDeletesData() throws DataStoreException {
        // Triggers an insert
        final BlogOwner raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build();
        adapter.save(raphael);

        // Triggers a delete
        adapter.delete(raphael);

        // Get the BlogOwner from the database
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertTrue(blogOwners.isEmpty());
    }

    /**
     * Assert that delete deletes item in the SQLite database without
     * violating foreign key constraints.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelCascades() throws DataStoreException {
        // Insert models
        final BlogOwner raphael = BlogOwner.builder()
                .name("Raphael Kim")
                .build();
        final BlogOwner john = BlogOwner.builder()
                .name("John Pignata")
                .build();
        adapter.save(raphael);
        adapter.save(john);

        // Trigger a foreign key constraint checks
        final Blog raphaelsBlog = Blog.builder()
                .name("Raphael's Blog")
                .owner(raphael)
                .build();
        final Blog johnsBlog = Blog.builder()
                .name("John's Blog")
                .owner(john)
                .build();
        adapter.save(raphaelsBlog);
        adapter.save(johnsBlog);

        // Triggers a delete
        // Deletes Raphael's Blog also to prevent foreign key violation
        adapter.delete(raphael);

        // Get the BlogOwner from the database. Assert john isn't affected.
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertEquals(Collections.singletonList(john), blogOwners);

        // Get the Blog from the database. Assert john's blog isn't affected.
        final List<Blog> blogs = adapter.query(Blog.class);
        assertEquals(Collections.singletonList(johnsBlog), blogs);
    }

    /**
     * Test deleting nonexistent model from the database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteNonexistentModelDoesNotFail() throws DataStoreException {
        final BlogOwner john = BlogOwner.builder()
                .name("John")
                .build();

        // Try deleting John without saving first
        adapter.delete(john);

        // Try deleting John again, this time with a true condition
        QueryPredicate matching = BlogOwner.NAME.eq(john.getName());
        adapter.delete(john, matching);

        // Try deleting John again, this time with a false condition
        QueryPredicate mismatch = BlogOwner.NAME.ne(john.getName());
        adapter.delete(john, mismatch);
    }

    /**
     * Test delete with predicate. Conditional delete is useful for making sure that
     * no data is removed with outdated assumptions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelWithPredicateDeletesConditionally() throws DataStoreException {
        final BlogOwner john = BlogOwner.builder()
                .name("John")
                .build();
        final BlogOwner jane = BlogOwner.builder()
                .name("Jane")
                .build();
        final BlogOwner mark = BlogOwner.builder()
                .name("Mark")
                .build();
        adapter.save(john);
        adapter.save(jane);
        adapter.save(mark);

        // Delete everybody but Mark
        final QueryPredicate predicate = BlogOwner.NAME.ne(mark.getName());
        adapter.delete(john, predicate);
        adapter.delete(jane, predicate);
        //noinspection ThrowableNotThrown This is the point of this method.
        adapter.deleteExpectingError(mark, predicate); // Should not be deleted

        List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertEquals(1, blogOwners.size());
        assertTrue(blogOwners.contains(mark));
    }
}
