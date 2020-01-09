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

package com.amplifyframework.api.aws;

import android.os.SystemClock;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.testmodels.noteswithauth.PrivateNote;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testmodels.ratingsblog.Blog;
import com.amplifyframework.testmodels.ratingsblog.Post;
import com.amplifyframework.testmodels.ratingsblog.PostEditor;
import com.amplifyframework.testmodels.ratingsblog.Rating;
import com.amplifyframework.testmodels.ratingsblog.User;
import com.amplifyframework.testmodels.teamproject.Projectfields;
import com.amplifyframework.testmodels.teamproject.Team;
import com.amplifyframework.testutils.SynchronousApi;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO: document how to configure a remote endpoint that can accomodate this test.
 */
public final class CodeGenerationInstrumentationTest {
    private static final String PERSON_API_NAME = "personApi";
    private static final String PROJECT_API_NAME = "projectApi";
    private static final String BLOG_API_NAME = "blogApi";
    private static final String NOTES_WITH_AUTH_API_NAME = "notesWithAuthApi";

    private static SynchronousApi api;

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     * @throws AmplifyException From Amplify configuration
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        AmplifyTestConfigurator.configureIfNotConfigured();
        api = SynchronousApi.singleton();
    }

    /**
     * Mutates an object, and then queries for its value back. Asserts that the two values are the same.
     * This tests our ability to generate GraphQL queries at runtime, from model primitives,
     * for both queries and mutations. The query also tests functionality of the QueryPredicate filter.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void queryMatchesMutationResult() {
        // Create a Person
        Person david = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.married)
            .build();
        Person createdPerson = api.create(PERSON_API_NAME, david);
        assertEquals(david, createdPerson);

        // Query for that created person, expect him to be there
        Person queriedPerson = api.get(PERSON_API_NAME, Person.class, createdPerson.getId());
        assertEquals(createdPerson, queriedPerson);
    }

    /**
     * Tests the code generation for LIST query with a predicate.
     */
    @Test
    public void queryListWithPredicate() {
        final List<Person> matchingPeople = api.list(
            PERSON_API_NAME,
            Person.class,
            Person.LAST_NAME.eq("Daudelin")
                .and(Person.FIRST_NAME.eq("David")
                    .or(Person.FIRST_NAME.eq("Sarah")))
        );

        for (Person person : matchingPeople) {
            assertTrue(Arrays.asList("David", "Sarah").contains(person.getFirstName()));
            assertEquals("Daudelin", person.getLastName());
        }
    }

    /**
     * Tests the code generation for LIST query without a predicate.
     */
    @SuppressWarnings("checkstyle:MagicNumber") // test table configured to have at least 3 items
    @Test
    public void queryListWithoutPredicate() {
        final List<Person> queryResults = api.list(PERSON_API_NAME, Person.class);
        assertTrue(queryResults.size() > 3);
    }

    /**
     * Tests that a subscription can receive an event when a create mutation takes place.
     */
    @Test
    public void subscribeReceivesMutationEvent() {
        SynchronousApi.Subscription<Person> subscription =
            api.onCreate(PERSON_API_NAME, Person.class);
        subscription.awaitSubscriptionStarted();

        Person johnDoe = Person.builder()
            .firstName("John")
            .lastName("Doe")
            .build();
        api.create(PERSON_API_NAME, johnDoe);

        // Validate that subscription received the newly created person.
        Person firstPersonOnSubscription = subscription.awaitFirstValue();
        assertEquals(johnDoe.getFirstName(), firstPersonOnSubscription.getFirstName());
        assertEquals(johnDoe.getLastName(), firstPersonOnSubscription.getLastName());

        // Cancel the subscription, and ensure that onComplete()
        // is called as a response to canceling the operation.
        subscription.cancel();
        subscription.awaitSubscriptionCompletion();
    }

    /**
     * Tests that attempting to subscribe to an API which is protected by Cognito User Pool auth will fail if the user
     * is unauthenticated. Also checks that the connection error is returned quickly without waiting for a timeout.
     */
    @Test
    public void subscribeFailsWithoutProperAuth() {
        // Start timing the subscription call.
        long startTime = SystemClock.elapsedRealtime();

        // Act: try to create a subscription
        SynchronousApi.Subscription<PrivateNote> subscription =
            api.onCreate(NOTES_WITH_AUTH_API_NAME, PrivateNote.class);

        // Assert: it failed with a connection_error
        Throwable exception = subscription.awaitSubscriptionFailure();
        assertTrue(exception instanceof ApiException);
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("connection_error"));

        // A connection error should take less than a second to be reported
        long acceptableDurationMs = TimeUnit.SECONDS.toMillis(1);
        long actualApiCallDurationMs = SystemClock.elapsedRealtime() - startTime;
        assertTrue(actualApiCallDurationMs < acceptableDurationMs);
    }

    /**
     * Creates an object and tries to update it with a false condition on the existing data which should fail to
     * ensure mutation condition filtering works. Then it sends a correct condition to delete which should succeed.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void mutationFailsInvalidConditionAndPassesCorrectCondition() {
        Person person = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.married)
            .build();
        api.create(PERSON_API_NAME, person);

        List<GraphQLResponse.Error> errors = api.updateExpectingErrors(
            PERSON_API_NAME,
            person.newBuilder()
                .age(30)
                .build(),
            Person.LAST_NAME.eq("Dandelion")
        );
        assertTrue(errors.get(0).getMessage().contains("ConditionalCheckFailedException"));

        api.delete(PERSON_API_NAME, Person.justId(person.getId()));
    }

    /**
     * For a model having an {@link BelongsTo} relationship to another model, validate
     * successful query, mutations, subscription.
     * TODO: Add mutate with condition and list with predicate since those are the ones
     *       that actually use the original model name
     */
    @Test
    public void belongsToRelationship() {
        // Subscribe to creation events for any Projectfields
        SynchronousApi.Subscription<Projectfields> subscription =
            api.onCreate(PROJECT_API_NAME, Projectfields.class);
        subscription.awaitSubscriptionStarted();

        // Create a team
        Team team = Team.builder()
            .name("AWS Mobile SDK")
            .build();
        Team createdTeam = api.create(PROJECT_API_NAME, team);

        // Create a Projectfields
        Projectfields projectfields = Projectfields.builder()
            .name("API Codegen")
            .team(Team.justId(createdTeam.getId()))
            .build();
        Projectfields createdProjectfields = api.create(PROJECT_API_NAME, projectfields);

        // Query for the Projectfields that were just created. The referenced team
        // should be the same as the one we requested for creation, earlier.
        assertEquals(
            team,
            api.get(PROJECT_API_NAME, Projectfields.class, createdProjectfields.getId())
                .getTeam()
        );

        // Validate that subscription received the newly created team, too.
        Projectfields projectfieldsOnSubscription = subscription.awaitFirstValue();
        assertEquals(projectfields.getName(), projectfieldsOnSubscription.getName());
        assertEquals(team, projectfieldsOnSubscription.getTeam());

        // We're done using the subscription, so cancel it.
        // As a result of cancellation, it should fire the onComplete action.
        subscription.cancel();
        subscription.awaitSubscriptionCompletion();
    }

    /**
     * Tests the code generation for HAS_MANY relationship.
     */
    @Test
    public void hasManyRelationship() {
        // Create a blog.
        Blog blog = Blog.builder()
            .name("All Things Amplify")
            .tags(Arrays.asList("amazon", "amplify", "framework", "software"))
            .build();
        assertEquals(
            blog,
            api.create(BLOG_API_NAME, blog)
        );

        // Create a post, associated to that blog
        Post post = Post.builder()
            .title("Test 1")
            .blog(blog)
            .build();
        Post createdPost = api.create(BLOG_API_NAME, post);

        // Validate that created post has same fields
        assertEquals(post.getId(), createdPost.getId());
        assertEquals(post.getTitle(), createdPost.getTitle());
        assertEquals(post.getBlog().getId(), createdPost.getBlog().getId());
        assertEquals(post.getBlog().getName(), createdPost.getBlog().getName());
        assertEquals(post.getBlog().getTags(), createdPost.getBlog().getTags());

        // Get the blog, and ensure that posts are associated to it on the endpoint
        Blog queriedBlog = api.get(BLOG_API_NAME, Blog.class, blog.getId());
        Post firstPostInQueriedBlog = queriedBlog.getPosts().get(0);
        assertEquals(post.getId(), firstPostInQueriedBlog.getId());
        assertEquals(post.getTitle(), firstPostInQueriedBlog.getTitle());
    }

    /**
     * Tests the code generation for HAS_ONE relationship.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void hasOneRelationship() {
        // Create a blog
        Blog blog = Blog.builder()
            .name("Necessary blog for post")
            .build();
        api.create(BLOG_API_NAME, blog);

        // Associate a post to that blog
        Post post = Post.builder()
            .title("Test post")
            .blog(blog)
            .build();
        api.create(BLOG_API_NAME, post);

        // Make a ating for the post
        Rating rating = Rating.builder()
            .stars(5)
            .post(post)
            .build();
        Rating createdRating = api.create(BLOG_API_NAME, rating);

        // Validate that rating that exists on the endpoint refers to the original post
        assertEquals(post, createdRating.getPost());

        /*
        TODO: This condition should work. However there is a bug on the AppSync transformer side which
            sets up the HasOne / BelongsTo relationship as two independent BelongsTo relationships so it fails.
            Once the AppSync transformer bug is fixed, we can uncomment this part of the test.

        assertEquals(
            rating,
            api.get(BLOG_API_NAME, Post.class, post.getId())
                .getRating()
        );
        */
    }

    /**
     * Tests the code generation for a Many to Many relationship simulated through two HasMany relationships.
     */
    @Test
    public void manyToManyRelationship() {
        // Create a blog.
        Blog blog = Blog.builder()
            .name("Necessary blog for post")
            .build();
        api.create(BLOG_API_NAME, blog);

        // Which contains a post
        Post post = Post.builder()
            .title("Test post")
            .blog(blog)
            .build();
        api.create(BLOG_API_NAME, post);

        // Create a user
        User user = User.builder()
            .username("Patches46")
            .build();
        api.create(BLOG_API_NAME, user);

        // The user is an editor of the created post
        PostEditor editor = PostEditor.builder()
            .post(post)
            .editor(user)
            .build();
        api.create(BLOG_API_NAME, editor);

        // Now, see what was actually setup on the endpoint, by querying for post and user
        Post queriedPost = api.get(BLOG_API_NAME, Post.class, post.getId());
        User queriedUser = api.get(BLOG_API_NAME, User.class, user.getId());

        // Validate that associations are setup correctly on the objects returned from endpoint
        // The post should refer to the user, and the user should refer to the post.
        assertEquals(1, queriedPost.getEditors().size());
        assertEquals(user, queriedPost.getEditors().get(0).getEditor());
        assertEquals(1, queriedUser.getPosts().size());
        assertEquals(post.getTitle(), queriedUser.getPosts().get(0).getPost().getTitle());
        assertEquals(post.getId(), queriedUser.getPosts().get(0).getPost().getId());
    }
}
