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

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.amplifyframework.core.model.query.predicate.QueryPredicateOperation.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the query functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterQueryTest extends StorageAdapterInstrumentedTestBase {

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithSingleItem() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertTrue(blogOwners.contains(blogOwner));
    }

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleItems() throws DataStoreException {
        final Set<BlogOwner> savedModels = new HashSet<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                .name("namePrefix:" + counter)
                .build();
            saveModel(blogOwner);
            savedModels.add(blogOwner);
        }

        final Set<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertEquals(savedModels, blogOwners);
    }

    /**
     * Test that querying the saved item with a foreign key
     * also populates that instance variable with object.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithForeignKey() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(blogOwner)
            .build();

        saveModel(blogOwner);
        saveModel(blog);

        final Set<Blog> blogs = queryModel(Blog.class);
        assertTrue(blogs.contains(blog));
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @SuppressWarnings("checkstyle:MagicNumber") // For predicates, arbitrarily decide some business rules
    @Test
    public void querySavedDataWithNumericalPredicates() throws DataStoreException {
        final List<Post> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Post post = Post.builder()
                .title("titlePrefix:" + counter)
                .status(PostStatus.INACTIVE)
                .rating(counter)
                .build();
            saveModel(post);
            savedModels.add(post);
        }

        // 1, 4, 5, 6
        QueryPredicate predicate = Post.RATING.ge(4).and(Post.RATING.lt(7))
                .or(Post.RATING.eq(1).and(Post.RATING.ne(7)));

        final Set<Post> expectedPosts = new HashSet<>(Arrays.asList(
            savedModels.get(1),
            savedModels.get(4),
            savedModels.get(5),
            savedModels.get(6)
        ));
        final Set<Post> actualPosts = queryModel(Post.class, predicate);
        assertEquals(expectedPosts, actualPosts);
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @SuppressWarnings("checkstyle:MagicNumber") // For predicates, arbitrarily decide some business rules
    @Test
    public void querySavedDataWithStringPredicates() throws DataStoreException {
        final List<Post> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Post post = Post.builder()
                .title(counter + "-title")
                .status(PostStatus.INACTIVE)
                .rating(counter)
                .build();
            saveModel(post);
            savedModels.add(post);
        }

        final Set<Post> expectedPosts = new HashSet<>(Arrays.asList(
                savedModels.get(4),
                savedModels.get(7)
        ));
        final Set<Post> actualPosts = queryModel(Post.class, Post.TITLE
            .beginsWith("4")
                .or(Post.TITLE.beginsWith("7"))
                .or(Post.TITLE.beginsWith("9"))
            .and(not(Post.TITLE.gt(8)))
        );
        assertEquals(expectedPosts, actualPosts);
    }

    /**
     * Test querying with predicate condition on connected model.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithPredicatesOnForeignKey() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Jane Doe")
            .build();
        saveModel(blogOwner);

        final Blog blog = Blog.builder()
            .name("Jane's Commercial Real Estate Blog")
            .owner(blogOwner)
            .build();
        saveModel(blog);

        final Set<Blog> blogsOwnedByJaneDoe = queryModel(Blog.class, QueryField.field("BlogOwner.name").eq("Jane Doe"));
        assertTrue(blogsOwnedByJaneDoe.contains(blog));
    }

    /**
     * Test query with SQL injection.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void queryWithMaliciousPredicates() throws DataStoreException {
        final BlogOwner jane = BlogOwner.builder()
            .name("Jane Doe")
            .build();
        saveModel(jane);

        QueryPredicate predicate = BlogOwner.NAME.eq("Jane; DROP TABLE Person; --");
        final Set<BlogOwner> resultOfMaliciousQuery = queryModel(BlogOwner.class, predicate);
        assertTrue(resultOfMaliciousQuery.isEmpty());

        final Set<BlogOwner> resultAfterMaliciousQuery = queryModel(BlogOwner.class);
        assertTrue(resultAfterMaliciousQuery.contains(jane));
    }
}
