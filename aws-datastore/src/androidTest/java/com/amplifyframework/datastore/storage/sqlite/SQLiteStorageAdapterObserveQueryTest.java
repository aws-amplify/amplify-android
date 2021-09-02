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

import static com.amplifyframework.core.model.query.predicate.QueryField.field;
import static com.amplifyframework.core.model.query.predicate.QueryPredicate.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.Page;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test the query functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterObserveQueryTest {
    private SynchronousStorageAdapter adapter;
    SQLiteStorageAdapter sqLiteStorageAdapter;

    /**
     * Enables Android Strict Mode, to help catch common errors while using
     * SQLite, such as forgetting to close the database.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Remove any old database files, and the re-provision a new storage adapter,
     * that is able to store the Comment-Blog family of models.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        AmplifyModelProvider modelProvider = AmplifyModelProvider.getInstance();
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        try {
            modelSchemaRegistry.register(modelProvider.models());
        } catch (AmplifyException modelSchemaLoadingFailure) {
            throw new RuntimeException(modelSchemaLoadingFailure);
        }
        sqLiteStorageAdapter =
                SQLiteStorageAdapter.forModels(modelSchemaRegistry, modelProvider);
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Close the open database, and cleanup any database files that it left.
     */
    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Test querying the saved item in the SQLite database with observeQuery.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithSingleItem() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        adapter.save(blogOwner);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(blogOwner));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

       adapter.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test querying the saved item in the SQLite database with observeQuery.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleItems() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<BlogOwner> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                .name("namePrefix:" + counter)
                .build();
            adapter.save(blogOwner);
            savedModels.add(blogOwner);
        }
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            for (BlogOwner blogOwner: savedModels) {
                assertTrue(value.getItems().contains(blogOwner));
            }
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test that querying the saved item with a foreign key with observeQuery
     * also populates that instance variable with object.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithForeignKey() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(blogOwner)
            .build();

        adapter.save(blogOwner);
        adapter.save(blog);

        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<Blog>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(blog));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                Blog.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test that querying the saved item with a foreign key
     * also populates that instance variable with object.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultiLevelJoins() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();

        final Blog blog = Blog.builder()
            .name("Alan's Software Blog")
            .owner(blogOwner)
            .build();

        final Post post = Post.builder()
            .title("Alan's first post")
            .status(PostStatus.ACTIVE)
            .rating(2)
            .blog(blog)
            .build();

        final Comment comment = Comment.builder()
            .content("Alan's first comment")
            .post(post)
            .build();

        adapter.save(blogOwner);
        adapter.save(blog);
        adapter.save(post);
        adapter.save(comment);

        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<Comment>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(comment));
            assertEquals(value.getItems().get(0).getPost(), post);
            assertEquals(value.getItems().get(0).getPost().getBlog(), blog);
            assertEquals(value.getItems().get(0).getPost().getBlog().getOwner(), blogOwner);
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                Comment.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithNumericalPredicates() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<Post> savedModels = new ArrayList<>();
        final int numModels = 10;

        BlogOwner blogOwner = BlogOwner.builder().name("Test Dummy").build();
        adapter.save(blogOwner);
        Blog blog = Blog.builder().name("Blogging for Dummies").owner(blogOwner).build();
        adapter.save(blog);

        for (int counter = 0; counter < numModels; counter++) {
            final Post post = Post.builder()
                .title("titlePrefix:" + counter)
                .status(PostStatus.INACTIVE)
                .rating(counter)
                .blog(blog)
                .build();
            adapter.save(post);
            savedModels.add(post);
        }

        // 1, 4, 5, 6
        QueryPredicate predicate = Post.RATING.ge(4)
                .and(Post.RATING.lt(7))
                .or(
                    Post.RATING.eq(1)
                    .and(Post.RATING.ne(7))
                );

        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<Post>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(savedModels.get(1)));
            assertTrue(value.getItems().contains(savedModels.get(4)));
            assertTrue(value.getItems().contains(savedModels.get(5)));
            assertTrue(value.getItems().contains(savedModels.get(6)));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                Post.class,
                Where.matches(predicate), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithStringPredicates() throws DataStoreException, InterruptedException {
        final List<Post> savedModels = new ArrayList<>();
        
        BlogOwner blogOwner = BlogOwner.builder().name("Test Dummy").build();
        adapter.save(blogOwner);
        Blog blog = Blog.builder().name("Blogging for Dummies").owner(blogOwner).build();
        adapter.save(blog);

        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Post post = Post.builder()
                .title(counter + "-title")
                .status(PostStatus.INACTIVE)
                .rating(counter)
                .blog(blog)
                .build();
            adapter.save(post);
            savedModels.add(post);
        }
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<Post>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(savedModels.get(4)));
            assertTrue(value.getItems().contains(savedModels.get(7)));
            assertFalse(value.getItems().contains(savedModels.get(9)));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                Post.class,
                Where.matches(
                        Post.TITLE.beginsWith("4")
                                .or(Post.TITLE.beginsWith("7"))
                                .or(Post.TITLE.beginsWith("9"))
                                .and(not(Post.TITLE.gt(8)))
                ), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test querying with predicate condition on connected model.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithPredicatesOnForeignKey() throws DataStoreException, InterruptedException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Jane Doe")
            .build();
        adapter.save(blogOwner);

        final Blog blog = Blog.builder()
            .name("Jane's Commercial Real Estate Blog")
            .owner(blogOwner)
            .build();
        adapter.save(blog);
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<Blog>> onQuerySnapshot = value->{
            assertTrue(value.getItems().contains(blog));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                Blog.class,
                Where.matches(BlogOwner.NAME.eq("Jane Doe")), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test query with SQL injection.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void queryWithMaliciousPredicates() throws DataStoreException, InterruptedException {
        final BlogOwner jane = BlogOwner.builder()
            .name("Jane Doe")
            .build();
        adapter.save(jane);

        QueryPredicate predicate = BlogOwner.NAME.eq("Jane; DROP TABLE Person; --");

        CountDownLatch latch = new CountDownLatch(2);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onMaliciousQuerySnapshot = resultOfMaliciousQuery->{
            assertTrue(resultOfMaliciousQuery.getItems().isEmpty());
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};
        adapter.observeQuery(
                BlogOwner.class,
                Where.matches(predicate), observationStarted, onMaliciousQuerySnapshot, onObservationError, onObservationComplete);

        Consumer<DataStoreQuerySnapshot<BlogOwner>> onAfterMaliciousQuery = resultAfterMaliciousQuery->{
            assertTrue(resultAfterMaliciousQuery.getItems().contains(jane));
            latch.countDown();
        };
        adapter.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onAfterMaliciousQuery, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * When there are 20 items available, and the user queries with a page size of 10,
     * the user should be given two pages of 10 items each.
     * @throws DataStoreException On arranging records, or from the query action itself
     */
    @Test
    public void queryWithPaginationWithCustomValues() throws DataStoreException, InterruptedException {
        final int pageSize = 10;
        createBlogOwnerRecords(pageSize * 2);

        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            assertNotNull(value.getItems());
            assertEquals(pageSize, value.getItems().size());
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                BlogOwner.class,
                Where.paginated(Page.startingAt(0).withLimit(pageSize)), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * When there are 102 items, and the user requests just the {@link Page#firstPage()},
     * with a page size of 100, that first page should come back with 100 items.
     * @throws DataStoreException On arranging records, or from the query action itself
     */
    @Test
    public void queryWithPaginationWithFirstPage() throws DataStoreException, InterruptedException {
        final int pageSize = 100;
        createBlogOwnerRecords(pageSize + 2);
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            assertNotNull(value.getItems());
            assertEquals(pageSize, value.getItems().size());
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};

        adapter.observeQuery(
                BlogOwner.class,
                Where.paginated(Page.firstPage()), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * When the user requests only the {@link Page#firstResult()}, they should get back
     * a single page that contains one value. (This, assuming that there is at least one
     * value that could be returned.)
     * @throws DataStoreException On failure to arrange items into store, or
     *                            from the query action itself
     */
    @Test
    public void queryWithPaginationWithFirstResult() throws DataStoreException, InterruptedException {
        createBlogOwnerRecords(2);
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            assertNotNull(value.getItems());
            assertEquals(1, value.getItems().size());
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};
        adapter.observeQuery(
                BlogOwner.class,
                Where.paginated(Page.firstResult()), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test query with order by.  Validate that a list of BlogOwners can be sorted first by name in descending order,
     * then by wea in ascending order.
     * @throws DataStoreException On failure to arrange items into store, or from the query action itself
     */
    @Test
    public void queryWithOrderBy() throws DataStoreException {
        // Expect
        List<String> names = Arrays.asList("Joe", "Joe", "Joe", "Bob", "Bob", "Bob", "Dan", "Dan", "Dan");
        List<String> weas = Arrays.asList("pon", "lth", "ver", "kly", "ken", "sel", "ner", "rer", "ned");
        List<BlogOwner> owners = new ArrayList<>();

        for (int i = 0; i < names.size(); i++) {
            BlogOwner owner = BlogOwner.builder()
                    .name(names.get(i))
                    .wea(weas.get(i))
                    .build();
            adapter.save(owner);
            owners.add(owner);
        }

        // Act
        List<BlogOwner> result = adapter.query(
                BlogOwner.class,
                Where.sorted(BlogOwner.NAME.descending(), BlogOwner.WEA.ascending())
        );

        // Verify
        List<BlogOwner> sorted = new ArrayList<>(owners);
        Collections.sort(sorted, Comparator
                .comparing(BlogOwner::getName)
                .reversed()
                .thenComparing(BlogOwner::getWea)
        );
        assertEquals(sorted, result);
    }

    /**
     * Test query with order by.  Validate that a list of Blog can be sorted by the names of BlogOwners.
     * @throws DataStoreException On failure to arrange items into store, or from the query action itself
     */
    @Test
    public void queryWithOrderByRelatedModel() throws DataStoreException {
        // Expect: Create BlogOwners and their respective blogs
        List<String> names = Arrays.asList("Joe", "Bob", "Dan", "Jane");
        List<Blog> blogs = new ArrayList<>();

        for (String name : names) {
            BlogOwner owner = BlogOwner.builder()
                    .name(name)
                    .build();
            adapter.save(owner);
            Blog blog = Blog.builder()
                    .name("")
                    .owner(owner)
                    .build();
            adapter.save(blog);
            blogs.add(blog);
        }

        // Act: Query Blogs sorted by owner's name
        List<Blog> result = adapter.query(Blog.class, Where.sorted(BlogOwner.NAME.ascending()));

        // Verify: Query result is sorted by owner's name
        List<Blog> sorted = new ArrayList<>(blogs);
        Collections.sort(sorted, Comparator.comparing(blog -> blog.getOwner().getName()));
        assertEquals(sorted, result);
    }

    /**
     * Test that new QueryField with explicit model name produces the same result as old QueryField.
     * @throws DataStoreException On failure to arrange items into store, or from the query action itself
     */
    @Test
    public void queryFieldsAreBackwardsCompatible() throws DataStoreException {
        BlogOwner blogOwner = BlogOwner.builder().name("Test Dummy").build();
        adapter.save(blogOwner);
        Blog blog = Blog.builder().name("Blogging for Dummies").owner(blogOwner).build();
        adapter.save(blog);

        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final Post post = Post.builder()
                    .title("title " + counter)
                    .status(PostStatus.INACTIVE)
                    .rating(counter)
                    .blog(blog)
                    .build();
            adapter.save(post);
        }

        // Assert that using QueryField without model name yields same results if there is no column ambiguity
        assertEquals(
            adapter.query(Post.class, Where.matches(field("Post", "title").contains("4"))),
            adapter.query(Post.class, Where.matches(field("title").contains("4")))
        );
        assertEquals(
            adapter.query(Post.class, Where.matches(field("Post", "rating").gt(3))),
            adapter.query(Post.class, Where.matches(field("rating").gt(3)))
        );
    }


    /**
     * Test querying the saved item in the SQLite database with observeQuery.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleItemsThenItemSaves() throws DataStoreException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};
        final List<BlogOwner> savedModels = new ArrayList<>();
        final int numModels = 10;
        AtomicInteger count = new AtomicInteger(0);
        for (int counter = 0; counter < numModels; counter++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                    .name("namePrefix:" + counter)
                    .build();
            adapter.save(blogOwner);
            savedModels.add(blogOwner);
        }

        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            if (count.get() == 0){
                for (BlogOwner blogOwner: savedModels) {
                    assertTrue(value.getItems().contains(blogOwner));
                }
                latch.countDown();
            }else{
                assertEquals(1,value.getItemChanges().size());
                changeLatch.countDown();
            }
           count.incrementAndGet();
        };
        adapter.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        for (int counter = 11; counter < 13; counter++) {
            try {
                final BlogOwner blogOwner = BlogOwner.builder()
                        .name("namePrefix:" + counter)
                        .build();
                savedModels.add(blogOwner);
                adapter.save(blogOwner);
            } catch (DataStoreException e) {
                e.printStackTrace();
            }
        }
       assertTrue(changeLatch.await(5, TimeUnit.SECONDS));
    }

    private void createBlogOwnerRecords(final int count) throws DataStoreException {
        for (int i = 0; i < count * 2; i++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                    .name("John Doe " + i)
                    .build();
            adapter.save(blogOwner);
        }
    }
}
