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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.observers.TestObserver;

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
        assertTrue(adapter.query(BlogOwner.class).isEmpty());
    }

    /**
     * Assert that delete deletes item in the SQLite database without
     * violating foreign key constraints.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelCascades() throws DataStoreException {
        // Create 1 blog owner, which has 3 blogs each, which has 3 posts each.
        // Insert 1 blog owner, 3 blogs, 9 posts
        Set<String> expected = new HashSet<>();
        BlogOwner ownerModel = BlogOwner.builder()
            .name("Blog Owner 1")
            .build();
        adapter.save(ownerModel);
        expected.add(ownerModel.getId());
        for (int blog = 1; blog <= 3; blog++) {
            Blog blogModel = Blog.builder()
                .name("Blog " + blog)
                .owner(ownerModel)
                .build();
            adapter.save(blogModel);
            expected.add(blogModel.getId());
            for (int post = 1; post <= 3; post++) {
                Post postModel = Post.builder()
                    .title("Post " + post)
                    .status(PostStatus.INACTIVE)
                    .rating(5)
                    .blog(blogModel)
                    .build();
                adapter.save(postModel);
                expected.add(postModel.getId());
            }
        }

        // Observe deletions
        TestObserver<String> deleteObserver = adapter.observe()
                .filter(change -> StorageItemChange.Type.DELETE.equals(change.type()))
                .map(StorageItemChange::item)
                .map(Model::getPrimaryKeyString)
                .test();

        // Triggers a delete.
        // Deletes every saved model to prevent foreign key constraint violation
        adapter.delete(ownerModel);

        // Assert that cascaded deletions are observed.
        deleteObserver.assertValueCount(13);
        assertEquals(expected, new HashSet<>(deleteObserver.values()));

        // Get data from the database and assert that everything is deleted.
        assertTrue(adapter.query(BlogOwner.class).isEmpty());
        assertTrue(adapter.query(Blog.class).isEmpty());
        assertTrue(adapter.query(Post.class).isEmpty());
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
        adapter.delete(john, BlogOwner.NAME.eq(john.getName()));

        // Try deleting John again, this time with a false condition
        adapter.delete(john, BlogOwner.NAME.ne(john.getName()));
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
        assertEquals(Collections.singletonList(mark), blogOwners);
    }


    /**
     * Test delete type with predicate.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelTypeWithPredicateDeletesData() throws DataStoreException {
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

        // Delete everybody whose names start with "J" (i.e. John & Jane)
        adapter.delete(BlogOwner.class, BlogOwner.NAME.beginsWith("J"));

        List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertEquals(Collections.singletonList(mark), blogOwners);
    }

    /**
     * Assert that delete model type with predicate deletes items in
     * the SQLite database without violating foreign key constraints.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelTypeWithPredicateCascades() throws DataStoreException {
        // Create 1 blog owner, which has 3 blogs each, which has 3 posts each.
        // Insert 1 blog owner, 3 blogs, 9 posts
        Set<String> expected = new HashSet<>();
        BlogOwner ownerModel = BlogOwner.builder()
                .name("Blog Owner 1")
                .build();
        adapter.save(ownerModel);
        for (int blog = 1; blog <= 3; blog++) {
            Blog blogModel = Blog.builder()
                    .name("Blog " + blog)
                    .owner(ownerModel)
                    .build();
            adapter.save(blogModel);
            expected.add(blogModel.getId());
            for (int post = 1; post <= 3; post++) {
                Post postModel = Post.builder()
                        .title("Post " + blog + "-" + post)
                        .status(PostStatus.INACTIVE)
                        .rating(5)
                        .blog(blogModel)
                        .build();
                adapter.save(postModel);
                expected.add(postModel.getId());
            }
        }

        // Observe deletions
        TestObserver<String> deleteObserver = adapter.observe()
                .filter(change -> StorageItemChange.Type.DELETE.equals(change.type()))
                .map(StorageItemChange::item)
                .map(Model::getPrimaryKeyString)
                .test();

        // Triggers a delete of all blogs.
        // All posts will be deleted by cascade.
        adapter.delete(Blog.class, QueryPredicates.all());

        // Assert 3 blogs and 9 posts are deleted.
        deleteObserver.assertValueCount(12);
        assertEquals(expected, new HashSet<>(deleteObserver.values()));

        // Get the BlogOwner from the database. Should not have been deleted.
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertEquals(Collections.singletonList(ownerModel), blogOwners);

        // Get the Blogs and Posts from the database. Should be deleted.
        assertTrue(adapter.query(Blog.class).isEmpty());
        assertTrue(adapter.query(Post.class).isEmpty());
    }

    /**
     * Test deleting model type by predicate where no model matches.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteNoneDoesNotFail() throws DataStoreException {
        // Save an entry in storage adapter
        final BlogOwner john = BlogOwner.builder()
                .name("John")
                .build();
        adapter.save(john);

        // Try deleting with zero matches
        adapter.delete(BlogOwner.class, QueryPredicates.none());

        // Assert that nothing was deleted
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertEquals(Collections.singletonList(john), blogOwners);
    }

    /**
     * Test deleting item with many children does not trigger SQLite exception.
     * SQLite places a hard limit of 1000 on expression tree depth, so chaining too many
     * predicates can potentially trigger an error.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteModelWithManyChildrenSucceeds() throws DataStoreException {
        // Create model with more than 1000 child models
        BlogOwner ownerModel = BlogOwner.builder()
                .name("BlogOwner")
                .build();
        adapter.save(ownerModel);
        for (int blog = 0; blog < 1001; blog++) {
            Blog blogModel = Blog.builder()
                    .name("Blog " + blog)
                    .owner(ownerModel)
                    .build();
            adapter.save(blogModel);
        }

        // Observe deletions
        TestObserver<String> deleteObserver = adapter.observe()
                .filter(change -> StorageItemChange.Type.DELETE.equals(change.type()))
                .map(StorageItemChange::item)
                .map(Model::getPrimaryKeyString)
                .test();

        // Delete parent model
        adapter.delete(ownerModel);

        // Assert 1 blog owner and 1001 blogs are deleted.
        deleteObserver.assertValueCount(1002);

        // Assert the local deletion happened.
        assertTrue(adapter.query(BlogOwner.class).isEmpty());
        assertTrue(adapter.query(Blog.class).isEmpty());
    }
}
