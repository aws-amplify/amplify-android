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

import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the save functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterSaveTest {
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
     * to warehouse the Comments-Blogs family of models.
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
     * Assert that save stores item in the SQLite database correctly.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelUpdatesData() throws DataStoreException {
        // Triggers an insert
        final BlogOwner raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build();
        adapter.save(raphael);

        // Triggers an update
        final BlogOwner raph = raphael.copyOfBuilder()
            .name("Raph Kim")
            .build();
        adapter.save(raph);

        // Get the BlogOwner from the database
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
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
        adapter.save(alan);

        // Get the BlogOwner from the database
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
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
        adapter.save(tony);

        // Get the BlogOwner from the database
        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
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
        adapter.save(alan);

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(alan)
            .build();
        adapter.save(blog);

        // Get the Blog from the database
        final List<Blog> blogs = adapter.query(Blog.class);
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
        adapter.save(blogOwner);

        final Blog blog = Blog.builder()
            .name("Alan's Blog")
            .owner(BlogOwner.builder()
                .name("Susan Swanson") // What??
                .build())
            .build();
        Throwable actualError = adapter.saveExpectingError(blog);

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
        adapter.save(blogOwner);

        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertTrue(blogOwners.contains(blogOwner));
    }

    /**
     * Test save with predicate. Conditional insert is not viable since conditional write
     * applies predicate to existing data. Insert is only performed if there isn't any existing
     * data. Save operation should fail.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void saveModelWithPredicateFailsInsert() throws DataStoreException {
        final BlogOwner john = BlogOwner.builder()
                .name("John")
                .build();
        final BlogOwner jane = BlogOwner.builder()
                .name("Jane")
                .build();
        final BlogOwner mark = BlogOwner.builder()
                .name("Mark")
                .build();

        // Try inserting with predicate
        final QueryPredicate predicate = BlogOwner.NAME.beginsWith("J");
        adapter.saveExpectingError(john, predicate);
        adapter.saveExpectingError(jane, predicate);
        adapter.saveExpectingError(mark, predicate);

        // Nothing was saved
        assertTrue(adapter.query(BlogOwner.class).isEmpty());
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
        adapter.save(john);
        adapter.save(jane);
        adapter.save(mark);

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
        adapter.save(newJohn, predicate);
        adapter.save(newJane, predicate);
        //noinspection ThrowableNotThrown
        adapter.saveExpectingError(newMark, predicate); // Should not update

        assertEquals(
            Observable.fromArray(newJohn, newJane, mark)
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(adapter.query(BlogOwner.class))
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );
    }

    /**
     * Verify that saving an item that already exists emits a StorageItemChange event with a patchItem that only
     * contains the fields that are different.
     *
     * @throws AmplifyException On failure to obtain ModelSchema from model class.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void patchItemOnlyHasChangedFields() throws AmplifyException, InterruptedException {
        // Create a BlogOwner.
        final BlogOwner johnSmith = BlogOwner.builder()
                .name("John Smith")
                .wea("ther")
                .build();
        adapter.save(johnSmith);

        // Start observing for changes
        TestObserver<StorageItemChange<? extends Model>> observer = adapter.observe().test();

        // Update one field on the BlogOwner.
        BlogOwner johnAdams = johnSmith.copyOfBuilder().name("John Adams").build();
        adapter.save(johnAdams);

        // Observe that the StorageItemChange contains an item with only the fields that changed (`id`, and `name`, but
        // not `wea`)
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("id", johnAdams.getId());
        serializedData.put("name", "John Adams");
        SerializedModel expectedItem = SerializedModel.builder()
                .serializedData(serializedData)
                .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                .build();
        observer.await(1, TimeUnit.SECONDS);
        observer.assertValueCount(1);
        observer.assertValueAt(0, storageItemChange -> storageItemChange.patchItem().equals(expectedItem));
    }

    /**
     * Test save with predicate. Confirms that conditionally updating a nested model also works.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void saveModelWithPredicateUpdatesForNestedModels() throws DataStoreException {
        // Save a model
        final BlogOwner mark = BlogOwner.builder()
                .name("Mark")
                .build();
        adapter.save(mark);

        // Save a model that belongs to another model
        final Blog marksBlog = Blog.builder()
                .name("Mark's very first blog.")
                .owner(mark)
                .build();
        adapter.save(marksBlog);

        // Update a model that belongs to another model
        final Blog marksBlogEdit = marksBlog.copyOfBuilder()
                .name("Mark's edited blog.")
                .build();
        adapter.save(marksBlogEdit);

        // Assert that update went through successfully
        assertEquals(Collections.singletonList(marksBlogEdit), adapter.query(Blog.class));
    }
}
