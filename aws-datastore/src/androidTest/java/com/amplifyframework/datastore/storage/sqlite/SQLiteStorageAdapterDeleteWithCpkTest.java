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
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.customprimarykey.AmplifyModelProvider;
import com.amplifyframework.testmodels.customprimarykey.Comment;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.rxjava3.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the delete functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterDeleteWithCpkTest {
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
     * Assert that delete model type with predicate deletes items in
     * the SQLite database without violating foreign key constraints.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void deleteCustomPrimaryKeyModelTypeWithDeleteAllPredicateCascades() throws DataStoreException {
        // Create 1 post, which has 3 comments each
        Set<String> expected = new HashSet<>();
        com.amplifyframework.testmodels.customprimarykey.Post
                postModel = com.amplifyframework.testmodels.customprimarykey.Post.builder()
                .title("test post")
                .id("testPostId")
                .build();
        adapter.save(postModel);
        expected.add(postModel.getPrimaryKeyString());
        for (int comment = 1; comment <= 3; comment++) {
            Comment commentModel = Comment.builder()
                    .title("comment " + comment)
                    .content("content " + comment)
                    .likes(2)
                    .description("description " + comment)
                    .post(postModel)
                    .build();
            adapter.save(commentModel);
            expected.add(commentModel.getPrimaryKeyString());
        }
        // Observe deletions
        TestObserver<String> deleteObserver = adapter.observe()
                .filter(change -> StorageItemChange.Type.DELETE.equals(change.type()))
                .map(StorageItemChange::item)
                .map(Model::getPrimaryKeyString)
                .test();

        // Triggers a delete of all blogs.
        // All posts will be deleted by cascade.
        adapter.delete(com.amplifyframework.testmodels.customprimarykey.Post.class, QueryPredicates.all());

        // Assert 3 comments.
        deleteObserver.assertValueCount(4);
        assertEquals(expected, new HashSet<>(deleteObserver.values()));

        // Get the Post and Comments from the database. Should be deleted.
        assertTrue(adapter.query(com.amplifyframework.testmodels.customprimarykey.Post.class).isEmpty());
        assertTrue(adapter.query(Comment.class).isEmpty());

    }
}
