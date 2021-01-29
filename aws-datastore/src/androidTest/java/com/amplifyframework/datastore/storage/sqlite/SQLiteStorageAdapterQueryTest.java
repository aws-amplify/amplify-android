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

import com.amplifyframework.core.model.query.Page;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;
import static com.amplifyframework.core.model.query.predicate.QueryPredicate.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the query functionality of {@link SQLiteStorageAdapter} operations.
 */
public final class SQLiteStorageAdapterQueryTest {
    private SynchronousStorageAdapter adapter;

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
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithSingleItem() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
            .name("Alan Turing")
            .build();
        adapter.save(blogOwner);

        final List<BlogOwner> blogOwners = adapter.query(BlogOwner.class);
        assertTrue(blogOwners.contains(blogOwner));
    }

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleItems() throws DataStoreException {
        final List<BlogOwner> savedModels = new ArrayList<>();
        final int numModels = 10;
        for (int counter = 0; counter < numModels; counter++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                .name("namePrefix:" + counter)
                .build();
            adapter.save(blogOwner);
            savedModels.add(blogOwner);
        }

        assertEquals(
            Observable.fromIterable(savedModels)
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

        adapter.save(blogOwner);
        adapter.save(blog);

        final List<Blog> blogs = adapter.query(Blog.class);
        assertTrue(blogs.contains(blog));
    }

    /**
     * Test that querying the saved item with a foreign key
     * also populates that instance variable with object.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultiLevelJoins() throws DataStoreException {
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

        final List<Comment> comments = adapter.query(Comment.class);
        assertTrue(comments.contains(comment));
        assertEquals(comments.get(0).getPost(), post);
        assertEquals(comments.get(0).getPost().getBlog(), blog);
        assertEquals(comments.get(0).getPost().getBlog().getOwner(), blogOwner);
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithNumericalPredicates() throws DataStoreException {
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

        assertEquals(
            Observable.fromArray(1, 4, 5, 6)
                .map(savedModels::get)
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(adapter.query(Post.class, Where.matches(predicate)))
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );
    }

    /**
     * Test querying the saved item in the SQLite database with
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithStringPredicates() throws DataStoreException {
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

        final List<Post> actualPosts = adapter.query(
                Post.class,
                Where.matches(
                    Post.TITLE.beginsWith("4")
                        .or(Post.TITLE.beginsWith("7"))
                        .or(Post.TITLE.beginsWith("9"))
                        .and(not(Post.TITLE.gt(8)))
                )
        );
        assertEquals(
            Observable.fromArray(4, 7)
                .map(savedModels::get)
                .toList()
                .map(HashSet::new)
                .blockingGet(),
            Observable.fromIterable(actualPosts)
                .toList()
                .map(HashSet::new)
                .blockingGet()
        );
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
        adapter.save(blogOwner);

        final Blog blog = Blog.builder()
            .name("Jane's Commercial Real Estate Blog")
            .owner(blogOwner)
            .build();
        adapter.save(blog);

        final List<Blog> blogsOwnedByJaneDoe = adapter.query(
            Blog.class,
            Where.matches(field("BlogOwner.name").eq("Jane Doe"))
        );
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
        adapter.save(jane);

        QueryPredicate predicate = BlogOwner.NAME.eq("Jane; DROP TABLE Person; --");
        final List<BlogOwner> resultOfMaliciousQuery = adapter.query(BlogOwner.class, Where.matches(predicate));
        assertTrue(resultOfMaliciousQuery.isEmpty());

        final List<BlogOwner> resultAfterMaliciousQuery = adapter.query(BlogOwner.class);
        assertTrue(resultAfterMaliciousQuery.contains(jane));
    }

    /**
     * When there are 20 items available, and the user queries with a page size of 10,
     * the user should be given two pages of 10 items each.
     * @throws DataStoreException On arranging records, or from the query action itself
     */
    @Test
    public void queryWithPaginationWithCustomValues() throws DataStoreException {
        final int pageSize = 10;
        createBlogOwnerRecords(pageSize * 2);

        List<BlogOwner> result = adapter.query(
            BlogOwner.class,
            Where.paginated(Page.startingAt(0).withLimit(pageSize))
        );
        assertNotNull(result);
        assertEquals(pageSize, result.size());
    }

    /**
     * When there are 102 items, and the user requests just the {@link Page#firstPage()},
     * with a page size of 100, that first page should come back with 100 items.
     * @throws DataStoreException On arranging records, or from the query action itself
     */
    @Test
    public void queryWithPaginationWithFirstPage() throws DataStoreException {
        final int pageSize = 100;
        createBlogOwnerRecords(pageSize + 2);

        List<BlogOwner> result = adapter.query(
            BlogOwner.class,
            Where.paginated(Page.firstPage())
        );
        assertNotNull(result);
        assertEquals(pageSize, result.size());
    }

    /**
     * When the user requests only the {@link Page#firstResult()}, they should get back
     * a single page that contains one value. (This, assuming that there is at least one
     * value that could be returned.)
     * @throws DataStoreException On failure to arrange items into store, or
     *                            from the query action itself
     */
    @Test
    public void queryWithPaginationWithFirstResult() throws DataStoreException {
        createBlogOwnerRecords(2);

        List<BlogOwner> result = adapter.query(
            BlogOwner.class,
            Where.paginated(Page.firstResult())
        );
        assertNotNull(result);
        assertEquals(1, result.size());
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

    private void createBlogOwnerRecords(final int count) throws DataStoreException {
        for (int i = 0; i < count * 2; i++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                    .name("John Doe " + i)
                    .build();
            adapter.save(blogOwner);
        }
    }
}
