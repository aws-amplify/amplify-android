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

import android.content.Context;
import android.database.Cursor;
import android.os.StrictMode;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.amplifyframework.core.model.query.predicate.QueryPredicateOperation.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test the functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterInstrumentedTest {
    private static final long SQLITE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private Context context;
    private SQLiteStorageAdapter sqliteStorageAdapter;

    /**
     * Enable strict mode for catching SQLite leaks.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build());
    }

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);

        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);

        LatchedResultListener<List<ModelSchema>> setupListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);

        sqliteStorageAdapter.initialize(context, setupListener);

        List<ModelSchema> setupResults = setupListener.awaitResult();

        List<Class<? extends Model>> expectedModels = new ArrayList<>(modelProvider.models());
        expectedModels.add(StorageItemChange.Record.class); // Internal
        expectedModels.add(PersistentModelVersion.class); // Internal
        assertEquals(expectedModels.size(), setupResults.size());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @After
    public void tearDown() throws DataStoreException {
        sqliteStorageAdapter.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Assert that save stores item in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelUpdatesData() throws DataStoreException {
        // Triggers an insert
        final BlogOwner raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build();
        saveModel(raphael);

        // Triggers an update
        final BlogOwner realRaph = raphael.copyOfBuilder()
            .name("Raph Kim")
            .build();
        saveModel(realRaph);

        // Get the BlogOwner record from the database
        List<BlogOwner> people = new ArrayList<>();
        Iterator<BlogOwner> iterator = queryModel(BlogOwner.class);
        while (iterator.hasNext()) {
            people.add(iterator.next());
        }
        assertEquals(1, people.size());
        BlogOwner possiblyRaph = people.get(0);

        assertEquals(realRaph, possiblyRaph);
    }

    /**
     * Assert that save stores data in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelInsertsData() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("BlogOwner");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals(
                "Alan Turing",
                cursor.getString(cursor.getColumnIndexOrThrow("BlogOwner_name"))
            );
        }
        cursor.close();
    }

    /**
     * Assert that save stores data in the SQLite database correctly
     * even if some optional values are null.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithNullsInsertsData() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Tony Danielsen")
            .wea(null)
            .build();
        saveModel(blogOwner);

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("BlogOwner");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals(
                "Tony Danielsen",
                cursor.getString(cursor.getColumnIndexOrThrow("BlogOwner_name"))
            );
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("BlogOwner_wea")));
        }
        cursor.close();
    }

    /**
     * Assert that save stores foreign key in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithValidForeignKey() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(blogOwner)
            .build();
        saveModel(blog);

        final Cursor cursor = sqliteStorageAdapter.getQueryAllCursor("Blog");
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        if (cursor.moveToFirst()) {
            assertEquals(
                "Alan's Software Blog",
                cursor.getString(cursor.getColumnIndexOrThrow("Blog_name"))
            );
            assertEquals(
                blogOwner.getId(),
                cursor.getString(cursor.getColumnIndexOrThrow("Blog_blogOwnerId"))
            );
        }
        cursor.close();
    }

    /**
     * Assert that foreign key constraint is enforced.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithInvalidForeignKey() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        LatchedResultListener<StorageItemChange.Record> saveListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        final Blog blog = Blog.builder()
            .name("Alan's Blog")
            .owner(BlogOwner.builder()
                .name("Susan Swanson") // What??
                .build())
            .build();
        sqliteStorageAdapter.save(blog, StorageItemChange.Initiator.DATA_STORE_API, saveListener);

        final String expectedError = "FOREIGN KEY constraint failed";

        Throwable actualError = saveListener.awaitError();
        assertNotNull(actualError.getCause());
        assertNotNull(actualError.getCause().getMessage());
        assertThat(Log.getStackTraceString(actualError), containsString(expectedError));
    }

    /**
     * Test save with SQL injection.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelWithMaliciousInputs() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Jane'); DROP TABLE Person; --")
            .build();
        saveModel(blogOwner);

        Iterator<BlogOwner> result = queryModel(BlogOwner.class);
        assertTrue(result.hasNext());
        assertEquals(blogOwner, result.next());
    }

    /**
     * Test save with predicate. Conditional write is useful for making sure that
     * no data is overwritten with outdated assumptions.
     * @throws DataStoreException from possible underlying DataStore exceptions
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
        saveModel(newMark, predicate); // Should not update

        Iterator<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertNotNull(blogOwners);
        Set<BlogOwner> actualBlogOwners = new HashSet<>();
        while (blogOwners.hasNext()) {
            actualBlogOwners.add(blogOwners.next());
        }
        assertEquals(
                new HashSet<>(Arrays.asList(
                        newJohn,
                        newJane,
                        mark
                )),
                actualBlogOwners
        );
    }

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void querySavedDataWithSingleItem() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        saveModel(blogOwner);

        Iterator<BlogOwner> result = queryModel(BlogOwner.class);
        assertNotNull(result);
        assertTrue(result.hasNext());
        BlogOwner queriedBlogOwner = result.next();
        assertNotNull(queriedBlogOwner);
        assertEquals(blogOwner, queriedBlogOwner);
    }

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void querySavedDataWithMultipleItems() throws DataStoreException {
        final List<BlogOwner> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                .name("namePrefix:" + counter)
                .build();
            saveModel(blogOwner);
            savedModels.add(blogOwner);
        }

        final Iterator<BlogOwner> resultIterator = queryModel(BlogOwner.class);
        final List<BlogOwner> results = new ArrayList<>();
        while (resultIterator.hasNext()) {
            results.add(resultIterator.next());
        }
        Comparator<BlogOwner> comparator = (o1, o2) -> o1.getName().compareTo(o2.getName());
        Collections.sort(savedModels, comparator);
        Collections.sort(results, comparator);
        assertEquals(savedModels, results);
    }

    /**
     * Test that querying the saved item with a foreign key
     * also populates that instance variable with object.
     * @throws DataStoreException from possible underlying DataStore exceptions
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

        Iterator<Blog> result = queryModel(Blog.class);
        assertNotNull(result);
        assertTrue(result.hasNext());

        final BlogOwner queriedBlogOwner = result.next().getOwner();
        assertNotNull(queriedBlogOwner);
        assertEquals(blogOwner.getId(), queriedBlogOwner.getId());
        assertEquals(blogOwner.getName(), queriedBlogOwner.getName());
        assertEquals(blogOwner.getBlog(), queriedBlogOwner.getBlog());
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException from possible underlying DataStore exceptions
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
        Iterator<Post> result = queryModel(Post.class, predicate);

        final Set<Post> expectedPosts = new HashSet<>(Arrays.asList(
            savedModels.get(1),
            savedModels.get(4),
            savedModels.get(5),
            savedModels.get(6)
        ));

        final Set<Post> actualPosts = new HashSet<>();
        while (result.hasNext()) {
            final Post post = result.next();
            assertNotNull(post);
            assertTrue(
                "Unable to find expected item in the storage adapter.",
                savedModels.contains(post)
            );
            actualPosts.add(post);
        }
        assertEquals(expectedPosts, actualPosts);
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException from possible underlying DataStore exceptions
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

        Iterator<Post> result = queryModel(Post.class, Post.TITLE
            .beginsWith("4")
                .or(Post.TITLE.beginsWith("7"))
                .or(Post.TITLE.beginsWith("9"))
            .and(not(Post.TITLE.gt(8)))
        );

        Set<Post> actualPosts = new HashSet<>();
        while (result.hasNext()) {
            actualPosts.add(result.next());
        }
        assertEquals(
            new HashSet<>(Arrays.asList(
                savedModels.get(4),
                savedModels.get(7)
            )),
            actualPosts
        );
    }

    /**
     * Test querying with predicate condition on connected model.
     * @throws DataStoreException from possible underlying DataStore exceptions
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

        Iterator<Blog> result = queryModel(Blog.class, QueryField.field("BlogOwner.name").eq("Jane Doe"));
        assertTrue(result.hasNext());
        assertEquals(blog, result.next());
    }

    /**
     * Test query with SQL injection.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void queryWithMaliciousPredicates() throws DataStoreException {
        final BlogOwner jane = BlogOwner.builder()
            .name("Jane Doe")
            .build();
        saveModel(jane);

        QueryPredicate predicate = BlogOwner.NAME.eq("Jane; DROP TABLE Person; --");
        Iterator<BlogOwner> resultOfMaliciousQuery = queryModel(BlogOwner.class, predicate);
        assertFalse(resultOfMaliciousQuery.hasNext());

        Iterator<BlogOwner> resultAfterMaliciousQuery = queryModel(BlogOwner.class);
        assertTrue(resultAfterMaliciousQuery.hasNext());
        assertEquals(jane, resultAfterMaliciousQuery.next());
    }

    /**
     * Assert that delete deletes item in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
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
        Iterator<BlogOwner> iterator = queryModel(BlogOwner.class);
        assertFalse(iterator.hasNext());
    }

    /**
     * Assert that delete deletes item in the SQLite database without
     * violating foreign key constraints.
     * @throws DataStoreException from possible underlying DataStore exceptions
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
        Iterator<BlogOwner> blogOwnerIterator = queryModel(BlogOwner.class);
        assertFalse(blogOwnerIterator.hasNext());

        // Get the Blog record from the database
        Iterator<Blog> blogIterator = queryModel(Blog.class);
        assertFalse(blogIterator.hasNext());
    }

    /**
     * Test delete with predicate. Conditional delete is useful for making sure that
     * no data is removed with outdated assumptions.
     * @throws DataStoreException from possible underlying DataStore exceptions
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
        deleteModel(mark, predicate); // Should not be deleted

        Iterator<BlogOwner> blogOwners = queryModel(BlogOwner.class);
        assertNotNull(blogOwners);
        assertEquals(mark, blogOwners.next());
        assertFalse(blogOwners.hasNext());
    }

    private <T extends Model> void saveModel(@NonNull T model) {
        saveModel(model, null);
    }

    private <T extends Model> void saveModel(
            @NonNull T model,
            @Nullable QueryPredicate predicate
    ) {
        LatchedResultListener<StorageItemChange.Record> saveListener =
                LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.save(model,
                StorageItemChange.Initiator.DATA_STORE_API,
                predicate,
                saveListener);
        saveListener.awaitTerminalEvent();
    }

    private <T extends Model> Iterator<T> queryModel(@NonNull Class<T> modelClass) {
        return queryModel(modelClass, null);
    }

    private <T extends Model> Iterator<T> queryModel(
            @NonNull Class<T> modelClass,
            @Nullable QueryPredicate predicate
    ) {
        LatchedResultListener<Iterator<T>> queryResultListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.query(modelClass,
                predicate,
                queryResultListener);
        return queryResultListener.awaitResult();
    }

    private <T extends Model> void deleteModel(@NonNull T model) {
        deleteModel(model, null);
    }

    private <T extends Model> void deleteModel(
            @NonNull T model,
            @Nullable QueryPredicate predicate
    ) {
        LatchedResultListener<StorageItemChange.Record> deleteListener =
            LatchedResultListener.waitFor(SQLITE_OPERATION_TIMEOUT_MS);
        sqliteStorageAdapter.delete(model,
                StorageItemChange.Initiator.DATA_STORE_API,
                predicate,
                deleteListener);
        deleteListener.awaitTerminalEvent();
    }
}

