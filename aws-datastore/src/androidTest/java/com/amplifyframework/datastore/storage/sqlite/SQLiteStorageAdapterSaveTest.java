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

import android.util.Log;

import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the save functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterSaveTest extends StorageAdapterInstrumentedTestBase {

    /**
     * Assert that save stores item in the SQLite database correctly.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelUpdatesData() throws DataStoreException {
        // Triggers an insert
        final BlogOwner raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build();
        saveModel(raphael);

        // Triggers an update
        final BlogOwner raph = raphael.copyOfBuilder()
            .name("Raph Kim")
            .build();
        saveModel(raph);

        // Get the BlogOwner record from the database
        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertEquals(1, blogOwners.size());
        assertFalse(blogOwners.contains(raphael)); // Replaced by "Raph Kim"
        assertTrue(blogOwners.contains(raph));
    }

    /**
     * Assert that save stores data in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelInsertsData() throws DataStoreException {
        final BlogOwner alan = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(alan);

        // Get the BlogOwner record from the database
        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertEquals(1, blogOwners.size());
        assertTrue(blogOwners.contains(alan));
    }

    /**
     * Assert that save stores data in the SQLite database correctly
     * even if some optional values are null.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithNullsInsertsData() throws DataStoreException {
        final BlogOwner tony = BlogOwner.builder()
            .name("Tony Danielsen")
            .wea(null)
            .build();
        saveModel(tony);

        // Get the BlogOwner record from the database
        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertEquals(1, blogOwners.size());
        assertTrue(blogOwners.contains(tony));
    }

    /**
     * Assert that save stores foreign key in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithValidForeignKey() throws DataStoreException {
        final BlogOwner alan = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(alan);

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(alan)
            .build();
        saveModel(blog);

        // Get the Blog record from the database
        final Set<Blog> blogs = queryModel(Blog.class);
        assertEquals(1, blogs.size());
        assertTrue(blogs.contains(blog));
    }

    /**
     * Assert that foreign key constraint is enforced.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelWithInvalidForeignKey() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        final Blog blog = Blog.builder()
            .name("Alan's Blog")
            .owner(BlogOwner.builder()
                .name("Susan Swanson") // What??
                .build())
            .build();
        Throwable actualError = saveModelExpectingError(blog);

        final String expectedError = "FOREIGN KEY constraint failed";
        assertNotNull(actualError.getCause());
        assertNotNull(actualError.getCause().getMessage());
        assertThat(Log.getStackTraceString(actualError), containsString(expectedError));
    }

    /**
     * Test save with SQL injection.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelWithMaliciousInputs() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Jane'); DROP TABLE Person; --")
            .build();
        saveModel(blogOwner);

        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertTrue(blogOwners.contains(blogOwner));
    }

    /**
     * Test save with predicate. Conditional write is useful for making sure that
     * no data is overwritten with outdated assumptions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelWithPredicateUpdatesConditionally() throws DataStoreException {
        final BlogOwner john = BlogOwner.builder()
                .name("John")
                .build();
        final BlogOwner jane = BlogOwner.builder()
                .name("Jane")
                .build();
        final BlogOwner mark = BlogOwner.builder()
                .name("Mark")
                .build();
        saveModel(john);
        saveModel(jane);
        saveModel(mark);

        // Only update John and Jane
        final QueryPredicate predicate = BlogOwner.NAME.beginsWith("J");
        final BlogOwner newJohn = john.copyOfBuilder()
                .name("John Doe")
                .build();
        final BlogOwner newJane = jane.copyOfBuilder()
                .name("Jane Doe")
                .build();
        final BlogOwner newMark = mark.copyOfBuilder()
                .name("Mark Doe")
                .build();
        saveModel(newJohn, predicate);
        saveModel(newJane, predicate);
        //noinspection ThrowableNotThrown
        saveModelExpectingError(newMark, predicate); // Should not update

        final Set<BlogOwner> expectedBlogOwners = new HashSet<>(Arrays.asList(
                newJohn,
                newJane,
                mark
        ));
        final Set<BlogOwner> actualBlogOwners = queryModel(BlogOwner.class);
        assertEquals(expectedBlogOwners, actualBlogOwners);
    }
}
