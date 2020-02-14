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
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the delete functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterDeleteTest extends StorageAdapterInstrumentedTestBase {

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
        saveModel(raphael);

        // Triggers a delete
        deleteModel(raphael);

        // Get the BlogOwner record from the database
        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertTrue(blogOwners.isEmpty());
    }

    /**
     * Assert that delete deletes item in the SQLite database without
     * violating foreign key constraints.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelCascades() throws DataStoreException {
        // Triggers an insert
        final BlogOwner raphael = BlogOwner.builder()
                .name("Raphael Kim")
                .build();
        saveModel(raphael);

        // Triggers a foreign key constraint check
        final Blog raphaelsBlog = Blog.builder()
                .name("Raphael's Blog")
                .owner(raphael)
                .build();
        saveModel(raphaelsBlog);

        // Triggers a delete
        // Deletes Raphael's Blog also to prevent foreign key violation
        deleteModel(raphael);

        // Get the BlogOwner record from the database
        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertTrue(blogOwners.isEmpty());

        // Get the Blog record from the database
        final Set<Blog> blogs = queryModel(Blog.class);
        assertTrue(blogs.isEmpty());
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
        saveModel(john);
        saveModel(jane);
        saveModel(mark);

        // Delete everybody but Mark
        final QueryPredicate predicate = BlogOwner.NAME.ne(mark.getName());
        deleteModel(john, predicate);
        deleteModel(jane, predicate);
        deleteModelExpectingError(mark, predicate); // Should not be deleted

        Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertEquals(1, blogOwners.size());
        assertTrue(blogOwners.contains(mark));
    }
}
