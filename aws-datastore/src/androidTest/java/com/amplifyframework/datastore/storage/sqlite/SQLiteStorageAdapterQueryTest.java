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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.query.Page;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.Blog3;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.BlogOwner3;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.Post2;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testmodels.cpk.BlogCPK;
import com.amplifyframework.testmodels.cpk.BlogOwnerCPK;
import com.amplifyframework.testmodels.cpk.PostCPK;
import com.amplifyframework.testmodels.phonecall.Call;
import com.amplifyframework.testmodels.phonecall.Person;
import com.amplifyframework.testmodels.phonecall.Phone;

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
import java.util.UUID;

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
     * Remove any old database files, and then re-provision a new storage adapter,
     * that is able to store the Comment-Blog family of models.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Remove any old database files, and then re-provision a new storage adapter,
     * that is able to store the Phone Call family of models.
     */
    public void setupForCallModel() {
        teardown();
        this.adapter = TestStorageAdapter.create(
                com.amplifyframework.testmodels.phonecall.AmplifyModelProvider.getInstance()
        );
    }

    /**
     * Remove any old database files, and then re-provision a new storage adapter,
     * that is able to store the Blog CPK family of models.
     */
    public void setupForBlogCPKModel() {
        teardown();
        this.adapter = TestStorageAdapter.create(
                com.amplifyframework.testmodels.cpk.AmplifyModelProvider.getInstance()
        );
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
     * Test predicates that check for null/not null values.
     * @throws DataStoreException not expected.
     */
    @Test
    public void queryBasedOnNullPredicateFields() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        adapter.save(blogOwner);

        List<BlogOwner> blogOwners = adapter.query(
                BlogOwner.class,
                Where.matches(BlogOwner.WEA.eq(null)));
        assertTrue(blogOwners.contains(blogOwner));

        blogOwners = adapter.query(
                BlogOwner.class,
                Where.matches(BlogOwner.NAME.eq(null)));
        assertTrue(blogOwners.isEmpty());
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
     * Test that querying the saved item with multiple foreign keys from the same
     * model also populates that instance variable with object.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleForeignKeysOfSameType() throws DataStoreException {
        setupForCallModel();
        final Person personCalling = Person.builder()
                .name("Alan Turing")
                .build();
        final Person personCalled = Person.builder()
                .name("Grace Hopper")
                .build();
        
        final Phone phoneCalling = Phone.builder()
                .number("123-456-7890")
                .ownerOfPhone(personCalling)
                .build();
        final Phone phoneCalled = Phone.builder()
                .number("567-890-1234")
                .ownerOfPhone(personCalled)
                .build();
        
        final Call phoneCall = Call.builder()
                .startTime(new Temporal.Time("10:35:22Z"))
                .caller(phoneCalling)
                .callee(phoneCalled)
                .build();

        adapter.save(personCalling);
        adapter.save(personCalled);
        adapter.save(phoneCalling);
        adapter.save(phoneCalled);
        adapter.save(phoneCall);

        final List<Call> phoneCalls = adapter.query(Call.class);
        assertTrue(phoneCalls.contains(phoneCall));
    }

    /**
     * Test that querying the saved item with multiple paths for foreign keys to the same
     * model correctly forms the where clause and returns expected data.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore.
     */
    @Test
    public void querySavedDataWithMultipleJoinPaths() throws DataStoreException {

        final String blogOwnerId = UUID.randomUUID().toString();
        final String blogId = UUID.randomUUID().toString();
        final String postId = UUID.randomUUID().toString();

        final BlogOwner3 blogOwner = BlogOwner3.builder()
                .name("Sample Blog")
                .id(blogOwnerId)
                .build();

        final Blog3 blog = Blog3.builder()
                .name("Sample Blog")
                .id(blogId)
                .owner(blogOwner)
                .build();

        final Post2 post = Post2.builder()
                .title("Placeholder title")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .id(postId)
                .blog(blog)
                .blogOwner(blogOwner)
                .build();

        adapter.save(blogOwner);
        adapter.save(blog);
        adapter.save(post);

        final List<Post2> posts = adapter.query(Post2.class,
                Where.matches(Post2.BLOG_OWNER.eq(blogOwnerId)));
        assertTrue(posts.contains(post));
    }

    /**
     * Test that querying the saved item with multiple paths for foreign keys to the same
     * model with missing data for intermediate model in join path correctly forms
     * the where clause and returns expected data.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithMultipleJoinWithMissingDataForIntermediateJoinPath()
            throws DataStoreException {

        final String blogOwnerAlphaId = UUID.randomUUID().toString();
        final String blogOwnerBetaId = UUID.randomUUID().toString();
        final String blogOwnerGammaId = UUID.randomUUID().toString();

        final String alphaPostId = UUID.randomUUID().toString();
        final String betaPostId = UUID.randomUUID().toString();
        final String gammaPostId = UUID.randomUUID().toString();

        final BlogOwner3 blogOwnerAlpha = BlogOwner3.builder()
                .name("Alpha")
                .id(blogOwnerAlphaId)
                .build();

        final BlogOwner3 blogOwnerBeta = BlogOwner3.builder()
                .name("Beta")
                .id(blogOwnerBetaId)
                .build();

        final BlogOwner3 blogOwnerGamma = BlogOwner3.builder()
                .name("Gamma")
                .id(blogOwnerGammaId)
                .build();

        final Post2 postByAlpha = Post2.builder()
                .title("Alpha Post")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .id(alphaPostId)
                .blogOwner(blogOwnerAlpha)
                .build();

        final Post2 postByBeta = Post2.builder()
                .title("Beta Post")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .id(betaPostId)
                .blogOwner(blogOwnerBeta)
                .build();

        final Post2 postByGamma = Post2.builder()
                .title("Gamma Post")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .id(gammaPostId)
                .blogOwner(blogOwnerGamma)
                .build();

        adapter.save(blogOwnerAlpha);
        adapter.save(blogOwnerBeta);
        adapter.save(blogOwnerGamma);
        adapter.save(postByAlpha);
        adapter.save(postByBeta);
        adapter.save(postByGamma);

        List<Post2> posts = adapter.query(Post2.class,
                Where.matches(Post2.BLOG_OWNER.eq(blogOwnerAlphaId)));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByAlpha));

        posts = adapter.query(Post2.class,
                Where.matches(Post2.BLOG_OWNER.eq(blogOwnerBetaId)));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByBeta));

        posts = adapter.query(Post2.class,
                Where.matches(Post2.BLOG_OWNER.eq(blogOwnerGammaId)));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByGamma));

    }

    /**
     * Test that querying the saved item with multiple paths for foreign keys to the same
     * model along with Multiple conditions correctly forms the where clause and returns expected
     * data.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore.
     */
    @Test
    public void querySavedDataWithMultipleJoinPathsWithMultipleConditions() throws DataStoreException {

        final String blogOwnerId = UUID.randomUUID().toString();
        final String blogId = UUID.randomUUID().toString();
        final String postId = UUID.randomUUID().toString();

        final BlogOwner3 blogOwner = BlogOwner3.builder()
                .name("Sample BlogOwner")
                .id(blogOwnerId)
                .build();

        final Blog3 blog = Blog3.builder()
                .name("Sample Blog")
                .id(blogId)
                .owner(blogOwner)
                .build();

        final Post2 post = Post2.builder()
                .title("Placeholder title")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .id(postId)
                .blog(blog)
                .blogOwner(blogOwner)
                .build();

        adapter.save(blogOwner);
        adapter.save(blog);
        adapter.save(post);

        final List<Post2> posts = adapter.query(Post2.class,
                Where.matches(Post2.BLOG_OWNER.eq(blogOwnerId).and(Post2.RATING.gt(4).
                        and(Post2.STATUS.eq(PostStatus.ACTIVE)))));
        assertTrue(posts.contains(post));
    }

    /**
     * Test that querying the saved item with multiple paths for foreign keys to the same
     * model with Custom Primary Key correctly forms the where clause
     * and returns expected data.
     * @throws AmplifyException On unexpected failure manipulating items in/out of DataStore.
     */
    @Test
    public void querySavedDataWithCPK() throws AmplifyException {
        setupForBlogCPKModel();
        final String blogOwnerId = UUID.randomUUID().toString();
        final String blogId = UUID.randomUUID().toString();
        final String postId = UUID.randomUUID().toString();

        final BlogOwnerCPK blogOwner = BlogOwnerCPK.builder()
                .id(blogOwnerId)
                .name("Sample BlogOwner")
                .build();

        final BlogCPK blog = BlogCPK.builder()
                .id(blogId)
                .name("Sample Blog")
                .owner(blogOwner)
                .build();

        final PostCPK post = PostCPK.builder()
                .id(postId)
                .title("Placeholder title")
                .rating(5)
                .blogOwner(blogOwner)
                 .blog(blog)
                .build();

        adapter.save(blogOwner);
        adapter.save(blog);
        adapter.save(post);

        // Basic CPK Query
        final List<PostCPK> posts = adapter.query(PostCPK.class,
                Where.identifier(PostCPK.class, new PostCPK.PostCPKIdentifier(postId, "Placeholder title")));
        assertTrue(posts.contains(post));
    }

    /**
     * Tests the retrieval of {@link PostCPK} objects through a query operation without the presence
     * of intermediate {@link Blog} records in a complex relationship chain. The relationship chain
     * tested here is {@link BlogOwnerCPK} -> {@link Blog} -> {@link PostCPK} and
     * {@link BlogOwnerCPK} -> {@link PostCPK}. The test ensures that {@link PostCPK} objects
     * associated directly with a {@link BlogOwnerCPK} can be successfully retrieved even when
     * the {@link Blog} records, which are typically part of the path in the relationship, are not
     * present in the database.
     * <p>
     * This test is important for scenarios where the data model allows for direct associations
     * between {@link BlogOwnerCPK} and {@link PostCPK} without the necessity of linking through
     * a {@link Blog} record, ensuring data integrity and query capabilities in partial data states.
     *
     * @throws AmplifyException if an error occurs during the setup or execution of the query
     *                         operation, indicating a failure in the data layer or the query
     *                         construction.
     */
    @Test
    public void querySavedDataWithMultipleJoinPathsWithMultipleConditionsAndCPK() throws AmplifyException {
        setupForBlogCPKModel();
        final String blogOwnerId = UUID.randomUUID().toString();
        final String blogId = UUID.randomUUID().toString();
        final String postId = UUID.randomUUID().toString();

        final BlogOwnerCPK blogOwner = BlogOwnerCPK.builder()
                .id(blogOwnerId)
                .name("Sample BlogOwner")
                .build();

        final PostCPK post = PostCPK.builder()
                .id(postId)
                .title("Placeholder title")
                .rating(5)
                .blogOwner(blogOwner)
                .build();
        adapter.save(blogOwner);
        adapter.save(post);
        String expectedBlogOwnerCPK = "\"" + blogOwnerId + "\"#\"Sample BlogOwner\"";
        final List<PostCPK> postsByBlogOwner = adapter.query(PostCPK.class,
                Where.identifier(PostCPK.class, new PostCPK.PostCPKIdentifier(postId, "Placeholder title"))
                .matches(PostCPK.BLOG_OWNER.eq(expectedBlogOwnerCPK).and(PostCPK.RATING.gt(4)
                        .and(PostCPK.TITLE.eq("Placeholder title")))));
        assertTrue(postsByBlogOwner.contains(post));
    }

    /**
     * Test that querying the saved item with multiple paths for foreign keys to the same
     * model with missing data for intermediate model in join path and Custom Primary Key
     * correctly forms the where clause and returns expected data.
     * @throws AmplifyException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void queryMultipleJoinWithMultipleRecordsWithMultipleConditionsAndCPK()
            throws AmplifyException {
        setupForBlogCPKModel();
        final String blogOwnerAlphaId = UUID.randomUUID().toString();
        final String blogOwnerBetaId = UUID.randomUUID().toString();
        final String blogOwnerGammaId = UUID.randomUUID().toString();

        final String alphaPostId = UUID.randomUUID().toString();
        final String betaPostId = UUID.randomUUID().toString();
        final String gammaPostId = UUID.randomUUID().toString();

        final BlogOwnerCPK blogOwnerAlpha = BlogOwnerCPK.builder()
                .id(blogOwnerAlphaId)
                .name("Alpha")
                .build();

        final BlogOwnerCPK blogOwnerBeta = BlogOwnerCPK.builder()
                .id(blogOwnerBetaId)
                .name("Beta")
                .build();

        final BlogOwnerCPK blogOwnerGamma = BlogOwnerCPK.builder()
                .id(blogOwnerGammaId)
                .name("Gamma")
                .build();

        final PostCPK postByAlpha = PostCPK.builder()
                .id(alphaPostId)
                .title("Alpha Post")
                .rating(5)
                .blogOwner(blogOwnerAlpha)
                .build();

        final PostCPK postByBeta = PostCPK.builder()
                .id(betaPostId)
                .title("Beta Post")
                .rating(5)
                .blogOwner(blogOwnerBeta)
                .build();

        final PostCPK postByGamma = PostCPK.builder()
                .id(gammaPostId)
                .title("Gamma Post")
                .rating(5)
                .blogOwner(blogOwnerGamma)
                .build();

        adapter.save(blogOwnerAlpha);
        adapter.save(blogOwnerBeta);
        adapter.save(blogOwnerGamma);
        adapter.save(postByAlpha);
        adapter.save(postByBeta);
        adapter.save(postByGamma);

        String expectedAlphaBlogOwnerCPK = "\"" + blogOwnerAlphaId + "\"#\"Alpha\"";
        String expectedBetaBlogOwnerCPK = "\"" + blogOwnerBetaId + "\"#\"Beta\"";
        String expectedGammaBlogOwnerCPK = "\"" + blogOwnerGammaId + "\"#\"Gamma\"";

        List<PostCPK> posts = adapter.query(PostCPK.class,
                Where.identifier(PostCPK.class, new PostCPK.PostCPKIdentifier(alphaPostId, "Alpha Post"))
                        .matches(PostCPK.BLOG_OWNER.eq(expectedAlphaBlogOwnerCPK).and(PostCPK.RATING.gt(4)
                        .and(PostCPK.TITLE.eq("Alpha Post")))));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByAlpha));

        posts = adapter.query(PostCPK.class,
                Where.identifier(PostCPK.class, new PostCPK.PostCPKIdentifier(betaPostId, "Beta Post"))
                        .matches(PostCPK.BLOG_OWNER.eq(expectedBetaBlogOwnerCPK).and(PostCPK.RATING.gt(4)
                        .and(PostCPK.TITLE.eq("Beta Post")))));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByBeta));

        posts = adapter.query(PostCPK.class,
                Where.identifier(PostCPK.class, new PostCPK.PostCPKIdentifier(gammaPostId, "Gamma Post"))
                        .matches(PostCPK.BLOG_OWNER.eq(expectedGammaBlogOwnerCPK).and(PostCPK.RATING.gt(4)
                        .and(PostCPK.TITLE.eq("Gamma Post")))));
        assertEquals(1, posts.size());
        assertTrue(posts.contains(postByGamma));

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
     * Test querying the saved item in the SQLite database with DateTime
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithDateTimePredicates() throws DataStoreException {
        final List<BlogOwner> savedModels = new ArrayList<>();
        final int numModels = 8;
        final List<Temporal.DateTime> createdAtTimes = Arrays.asList(
                new Temporal.DateTime("2020-01-01T19:30:45.000000000Z"),
                new Temporal.DateTime("2020-01-01T19:30:45.100000000Z"),
                new Temporal.DateTime("2020-01-01T19:30:45.100250000Z"),
                new Temporal.DateTime("2020-01-01T19:30:45.1000Z"),
                new Temporal.DateTime("2020-01-01T20:30:45.111Z"),
                new Temporal.DateTime("2020-01-01T19:30:45.111+00:00"),
                new Temporal.DateTime("2020-01-01T19:30:45.111+01:00"),
                new Temporal.DateTime("2020-01-01T19:30:45.111222333Z")
        );

        for (int counter = 0; counter < numModels; counter++) {
            BlogOwner blogOwner = BlogOwner.builder()
                    .name("Test Blogger " + counter)
                    .createdAt(createdAtTimes.get(counter))
                    .build();
            adapter.save(blogOwner);
            savedModels.add(blogOwner);
        }

        // 0, 1, 3, 6
        QueryPredicate predicate = BlogOwner.CREATED_AT.le(new Temporal.DateTime("2020-01-01T19:30:45.100000000Z"));
        
        assertEquals(
                Observable.fromArray(0, 1, 3, 6)
                        .map(savedModels::get)
                        .map(BlogOwner::getId)
                        .toList()
                        .map(HashSet::new)
                        .blockingGet(),
                Observable.fromIterable(adapter.query(BlogOwner.class, Where.matches(predicate)))
                        .map(BlogOwner::getId)
                        .toList()
                        .map(HashSet::new)
                        .blockingGet()
        );
    }

    /**
     * Test querying the saved item in the SQLite database with Time
     * predicate conditions.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithTimePredicates() throws DataStoreException {
        setupForCallModel();
        final Person personCalling = Person.builder()
                .name("Alan Turing")
                .build();
        final Person personCalled = Person.builder()
                .name("Grace Hopper")
                .build();

        final Phone phoneCalling = Phone.builder()
                .number("123-456-7890")
                .ownerOfPhone(personCalling)
                .build();
        final Phone phoneCalled = Phone.builder()
                .number("567-890-1234")
                .ownerOfPhone(personCalled)
                .build();

        adapter.save(personCalling);
        adapter.save(personCalled);
        adapter.save(phoneCalling);
        adapter.save(phoneCalled);

        final List<Call> savedModels = new ArrayList<>();
        final int numModels = 8;
        final List<Temporal.Time> callStartTimes = Arrays.asList(
                new Temporal.Time("19:30:45.000000000"),
                new Temporal.Time("19:30:45.100000000Z"),
                new Temporal.Time("19:30:45.100250000Z"),
                new Temporal.Time("19:30:45.1000Z"),
                new Temporal.Time("20:30:45.111Z"),
                new Temporal.Time("19:30:45.111+00:00"),
                new Temporal.Time("19:30:45.111+01:00"),
                new Temporal.Time("19:30:45.111222333Z")
        );

        for (int counter = 0; counter < numModels; counter++) {
            Call phoneCall = Call.builder()
                    .startTime(callStartTimes.get(counter))
                    .caller(phoneCalling)
                    .callee(phoneCalled)
                    .build();
            adapter.save(phoneCall);
            savedModels.add(phoneCall);
        }

        // 0, 1, 3, 6
        QueryPredicate predicate = Call.STARTTIME.le(new Temporal.Time("19:30:45.100000000Z"));

        assertEquals(
                Observable.fromArray(0, 1, 3, 6)
                        .map(savedModels::get)
                        .map(Call::getId)
                        .toList()
                        .map(HashSet::new)
                        .blockingGet(),
                Observable.fromIterable(adapter.query(Call.class, Where.matches(predicate)))
                        .map(Call::getId)
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
            Where.matches(BlogOwner.NAME.eq("Jane Doe"))
        );
        assertTrue(blogsOwnedByJaneDoe.contains(blog));
    }

    /**
     * Test querying with Where.Id predicate condition on connected model.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void querySavedDataWithIdPredicateOnForeignKey() throws AmplifyException {
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
                Where.identifier(Blog.class, blog.getId())
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

    private void createBlogOwnerRecords(final int count) throws DataStoreException {
        for (int i = 0; i < count * 2; i++) {
            final BlogOwner blogOwner = BlogOwner.builder()
                    .name("John Doe " + i)
                    .build();
            adapter.save(blogOwner);
        }
    }
}
