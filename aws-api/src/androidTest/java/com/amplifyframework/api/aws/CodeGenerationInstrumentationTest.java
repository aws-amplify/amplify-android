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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testmodels.ratingsblog.Blog;
import com.amplifyframework.testmodels.ratingsblog.Post;
import com.amplifyframework.testmodels.ratingsblog.PostEditor;
import com.amplifyframework.testmodels.ratingsblog.Rating;
import com.amplifyframework.testmodels.ratingsblog.User;
import com.amplifyframework.testmodels.teamproject.Projectfields;
import com.amplifyframework.testmodels.teamproject.Team;
import com.amplifyframework.testutils.LatchedResponseStreamListener;
import com.amplifyframework.testutils.LatchedSingleResponseListener;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TODO: document how to configure a remote endpoint that can accomodate this test.
 */
public final class CodeGenerationInstrumentationTest {
    private static final String PERSON_API_NAME = "personApi";
    private static final String PROJECT_API_NAME = "projectApi";
    private static final String BLOG_API_NAME = "blogApi";

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     * @throws AmplifyException From Amplify configuration
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        AmplifyTestConfigurator.configureIfNotConfigured();
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
        LatchedSingleResponseListener<Person> mutationListener = new LatchedSingleResponseListener<>();
        Person david = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.married)
            .build();
        Amplify.API.mutate(
            PERSON_API_NAME,
            david,
            MutationType.CREATE,
            mutationListener
        );
        Person createdPerson = mutationListener.awaitSuccessResponse();
        assertEquals(david, createdPerson);

        // Query for that created person, expect him to be there
        LatchedSingleResponseListener<Person> queryListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(
            PERSON_API_NAME,
            Person.class,
            createdPerson.getId(),
            queryListener
        );
        Person queriedPerson = queryListener.awaitSuccessResponse();
        assertEquals(createdPerson, queriedPerson);
    }

    /**
     * Tests the code generation for LIST query with a predicate.
     */
    @Test
    public void queryListWithPredicate() {
        LatchedSingleResponseListener<Iterable<Person>> queryListener = new LatchedSingleResponseListener<>();

        Amplify.API.query(
            PERSON_API_NAME,
            Person.class,
            Person.LAST_NAME.eq("Daudelin")
                .and(Person.FIRST_NAME.eq("David")
                    .or(Person.FIRST_NAME.eq("Sarah"))),
            queryListener
        );

        for (Person person : queryListener.awaitSuccessResponse()) {
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
        LatchedSingleResponseListener<Iterable<Person>> queryListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(PERSON_API_NAME, Person.class, queryListener);
        Iterable<Person> queryResultsIterable = queryListener.awaitSuccessResponse();

        // Test table should always have at least three items
        int count = 0;
        Iterator<Person> iterator = queryResultsIterable.iterator();
        while (iterator.hasNext() && count < 3) {
            iterator.next();
            count++;
        }
        assertEquals(3, count);
    }

    /**
     * Tests that a subscription can receive an event when a create mutation takes place.
     * @throws Throwable If we timeout while talking to the endpoint,
     *                   or if any response comes back invalid
     */
    @Test
    public void subscribeReceivesMutationEvent() throws Throwable {
        Person person = Person.builder().firstName("John").lastName("Doe").build();
        LatchedResponseStreamListener<Person> streamListener = new LatchedResponseStreamListener<>(1);
        GraphQLOperation<Person> operation = Amplify.API.subscribe(
                PERSON_API_NAME,
                Person.class,
                SubscriptionType.ON_CREATE,
                streamListener
        );

        Amplify.API.mutate(
                PERSON_API_NAME,
                person,
                null,
                MutationType.CREATE,
                null
        );

        // Validate that subscription received the newly created person.
        List<Person> peopleOnSubscription = streamListener.awaitSuccessfulResponses();
        assertEquals(1, peopleOnSubscription.size());
        Person firstPersonOnSubscription = peopleOnSubscription.get(0);
        assertEquals(person.getFirstName(), firstPersonOnSubscription.getFirstName());
        assertEquals(person.getLastName(), firstPersonOnSubscription.getLastName());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        streamListener.awaitCompletion();
    }

    /**
     * Creates an object and tries to update it with a false condition on the existing data which should fail to
     * ensure mutation condition filtering works. Then it sends a correct condition to delete which should succeed.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void mutationFailsInvalidConditionAndPassesCorrectCondition() {
        LatchedSingleResponseListener<Person> deleteListener = new LatchedSingleResponseListener<>();

        LatchedSingleResponseListener<Person> createListener = new LatchedSingleResponseListener<>();
        Person person = Person
                .builder()
                .firstName("David")
                .lastName("Daudelin")
                .age(29)
                .relationship(MaritalStatus.married)
                .build();

        Amplify.API.mutate(
                PERSON_API_NAME,
                person,
                MutationType.CREATE,
                createListener
        );
        createListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Person> updateListener = new LatchedSingleResponseListener<>();
        Person updated = person.newBuilder().age(30).build();
        Amplify.API.mutate(
                PERSON_API_NAME,
                updated,
                Person.LAST_NAME.eq("Dandelion"),
                MutationType.UPDATE,
                updateListener
        );

        GraphQLResponse.Error firstError = updateListener.awaitErrors().get(0);
        assertTrue(firstError.getMessage().contains("ConditionalCheckFailedException"));

        Amplify.API.mutate(
                PERSON_API_NAME,
                Person.justId(person.getId()),
                MutationType.DELETE,
                deleteListener
        );
        deleteListener.awaitSuccessResponse();
    }

    /**
     * For a model having an {@link BelongsTo} relationship to another model, validate
     * successful query, mutations, subscription.
     * TODO: Add mutate with condition and list with predicate since those are the ones
     *       that actually use the original model name
     * @throws Throwable If we timeout while talking to the endpoint, or if any response comes back invalid
     */
    @Test
    public void belongsToRelationship() throws Throwable {
        LatchedResponseStreamListener<Projectfields> projectSubscriptionListener =
                new LatchedResponseStreamListener<>(1);
        GraphQLOperation<Projectfields> operation = Amplify.API.subscribe(
                PROJECT_API_NAME,
                Projectfields.class,
                SubscriptionType.ON_CREATE,
                projectSubscriptionListener
        );

        LatchedSingleResponseListener<Team> teamMutationListener = new LatchedSingleResponseListener<>();
        Team team = Team.builder().name("AWS Mobile SDK").build();
        Amplify.API.mutate(
                PROJECT_API_NAME,
                team,
                MutationType.CREATE,
                teamMutationListener
        );
        Team createdTeam = teamMutationListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Projectfields> projectMutationListener = new LatchedSingleResponseListener<>();
        Projectfields projectfields = Projectfields
                .builder()
                .name("API Codegen")
                .team(Team.justId(createdTeam.getId()))
                .build();
        Amplify.API.mutate(
                PROJECT_API_NAME,
                projectfields,
                MutationType.CREATE,
                projectMutationListener
        );
        Projectfields createdProjectfields = projectMutationListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Projectfields> projectQueryListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(
                PROJECT_API_NAME,
                Projectfields.class,
                createdProjectfields.getId(),
                projectQueryListener
        );
        assertEquals(team, projectQueryListener.awaitSuccessResponse().getTeam());

        // Validate that subscription received the newly created person.
        List<Projectfields> projectfieldsFromSubscription =
            projectSubscriptionListener.awaitSuccessfulResponses();
        assertEquals(1, projectfieldsFromSubscription.size());
        Projectfields firstProjectfieldsOnSubscription = projectfieldsFromSubscription.get(0);
        assertEquals(projectfields.getName(), firstProjectfieldsOnSubscription.getName());
        assertEquals(team, firstProjectfieldsOnSubscription.getTeam());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        projectSubscriptionListener.awaitCompletion();
    }

    /**
     * Tests the code generation for HAS_MANY relationship.
     */
    @Test
    public void hasManyRelationship() {
        LatchedSingleResponseListener<Blog> blogCreateListener = new LatchedSingleResponseListener<>();
        Blog blog = Blog.builder()
                .name("All Things Amplify")
                .tags(Arrays.asList("amazon", "amplify", "framework", "software"))
                .build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                blog,
                MutationType.CREATE,
                blogCreateListener
        );
        Blog blogCreateResult = blogCreateListener.awaitSuccessResponse();
        assertEquals(blog, blogCreateResult);

        LatchedSingleResponseListener<Post> postCreateListener = new LatchedSingleResponseListener<>();
        Post post = Post.builder().title("Test 1").blog(blog).build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                post,
                MutationType.CREATE,
                postCreateListener
        );
        Post postCreateResult = postCreateListener.awaitSuccessResponse();
        assertEquals(post.getId(), postCreateResult.getId());
        assertEquals(post.getTitle(), postCreateResult.getTitle());
        assertEquals(post.getBlog().getId(), postCreateResult.getBlog().getId());
        assertEquals(post.getBlog().getName(), postCreateResult.getBlog().getName());
        assertEquals(post.getBlog().getTags(), postCreateResult.getBlog().getTags());

        LatchedSingleResponseListener<Blog> blogGetListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(
                BLOG_API_NAME,
                Blog.class,
                blog.getId(),
                blogGetListener
        );
        Blog blogGetResult = blogGetListener.awaitSuccessResponse();
        Post blogGetResultPost = blogGetResult.getPosts().get(0);
        assertEquals(post.getId(), blogGetResultPost.getId());
        assertEquals(post.getTitle(), blogGetResultPost.getTitle());
    }

    /**
     * Tests the code generation for HAS_ONE relationship.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void hasOneRelationship() {
        LatchedSingleResponseListener<Blog> blogCreateListener = new LatchedSingleResponseListener<>();
        Blog blog = Blog.builder()
                .name("Necessary blog for post")
                .build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                blog,
                MutationType.CREATE,
                blogCreateListener
        );
        blogCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Post> postCreateListener = new LatchedSingleResponseListener<>();
        Post post = Post.builder().title("Test post").blog(blog).build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                post,
                MutationType.CREATE,
                postCreateListener
        );
        postCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Rating> ratingCreateListener = new LatchedSingleResponseListener<>();

        Rating rating = Rating.builder().stars(5).post(post).build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                rating,
                MutationType.CREATE,
                ratingCreateListener
        );
        assertEquals(post, ratingCreateListener.awaitSuccessResponse().getPost());

        /*
        TODO: This condition should work. However there is a bug on the AppSync transformer side which
            sets up the HasOne / BelongsTo relationship as two independent BelongsTo relationships so it fails.
            Once the AppSync transformer bug is fixed, we can uncomment this part of the test.

        LatchedSingleResponseListener<Post> postGetListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(
                BLOG_API_NAME,
                Post.class,
                post.getId(),
                postGetListener
        );
        assertEquals(rating, postGetListener.awaitSuccessResponse().getRating());
         */
    }

    /**
     * Tests the code generation for a Many to Many relationship simulated through two HasMany relationships.
     */
    @Test
    public void manyToManyRelationship() {
        LatchedSingleResponseListener<Blog> blogCreateListener = new LatchedSingleResponseListener<>();
        Blog blog = Blog.builder()
                .name("Necessary blog for post")
                .build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                blog,
                MutationType.CREATE,
                blogCreateListener
        );
        blogCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Post> postCreateListener = new LatchedSingleResponseListener<>();
        Post post = Post.builder().title("Test post").blog(blog).build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                post,
                MutationType.CREATE,
                postCreateListener
        );
        postCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<User> userCreateListener = new LatchedSingleResponseListener<>();
        User user = User.builder().username("Patches46").build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                user,
                MutationType.CREATE,
                userCreateListener
        );
        userCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<PostEditor> editorCreateListener = new LatchedSingleResponseListener<>();
        PostEditor editor = PostEditor.builder().post(post).editor(user).build();
        Amplify.API.mutate(
                BLOG_API_NAME,
                editor,
                MutationType.CREATE,
                editorCreateListener
        );
        editorCreateListener.awaitSuccessResponse();

        LatchedSingleResponseListener<Post> postGetListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<User> userGetListener = new LatchedSingleResponseListener<>();
        Amplify.API.query(
                BLOG_API_NAME,
                Post.class,
                post.getId(),
                postGetListener
        );
        Amplify.API.query(
                BLOG_API_NAME,
                User.class,
                user.getId(),
                userGetListener
        );
        Post postGetResult = postGetListener.awaitSuccessResponse();
        User userGetResult = userGetListener.awaitSuccessResponse();

        assertEquals(1, postGetResult.getEditors().size());
        assertEquals(user, postGetResult.getEditors().get(0).getEditor());
        assertEquals(1, userGetResult.getPosts().size());
        assertEquals(post.getTitle(), userGetResult.getPosts().get(0).getPost().getTitle());
        assertEquals(post.getId(), userGetResult.getPosts().get(0).getPost().getId());
    }
}
