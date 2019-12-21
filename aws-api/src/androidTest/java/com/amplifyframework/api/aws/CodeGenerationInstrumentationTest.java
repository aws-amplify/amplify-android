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
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
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
import com.amplifyframework.testutils.EmptyConsumer;
import com.amplifyframework.testutils.LatchedAction;
import com.amplifyframework.testutils.LatchedResponseConsumer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

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
        LatchedResponseConsumer<Person> createdPersonConsumer = LatchedResponseConsumer.instance();
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
            ResultListener.instance(createdPersonConsumer, EmptyConsumer.of(Throwable.class))
        );
        Person createdPerson = createdPersonConsumer.awaitResponseData();
        assertEquals(david, createdPerson);

        // Query for that created person, expect him to be there
        LatchedResponseConsumer<Person> queriedPersonConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(
            PERSON_API_NAME,
            Person.class,
            createdPerson.getId(),
            ResultListener.instance(queriedPersonConsumer, EmptyConsumer.of(Throwable.class))
        );
        Person queriedPerson = queriedPersonConsumer.awaitResponseData();
        assertEquals(createdPerson, queriedPerson);
    }

    /**
     * Tests the code generation for LIST query with a predicate.
     */
    @Test
    public void queryListWithPredicate() {
        LatchedResponseConsumer<Iterable<Person>> queryConsumer = LatchedResponseConsumer.instance();

        Amplify.API.query(
            PERSON_API_NAME,
            Person.class,
            Person.LAST_NAME.eq("Daudelin")
                .and(Person.FIRST_NAME.eq("David")
                    .or(Person.FIRST_NAME.eq("Sarah"))),
            ResultListener.instance(queryConsumer, EmptyConsumer.of(Throwable.class))
        );

        for (Person person : queryConsumer.awaitResponseData()) {
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
        LatchedResponseConsumer<Iterable<Person>> queryConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(PERSON_API_NAME, Person.class,
            ResultListener.instance(queryConsumer, EmptyConsumer.of(Throwable.class)));
        Iterable<Person> queryResultsIterable = queryConsumer.awaitResponseData();

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
     */
    @Test
    public void subscribeReceivesMutationEvent() {
        Person person = Person.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        LatchedResponseConsumer<Person> streamItemConsumer = LatchedResponseConsumer.instance();
        LatchedAction streamCompletionAction = LatchedAction.instance();
        GraphQLOperation<Person> operation = Amplify.API.subscribe(
            PERSON_API_NAME,
            Person.class,
            SubscriptionType.ON_CREATE,
            StreamListener.instance(streamItemConsumer, EmptyConsumer.of(Throwable.class), streamCompletionAction)
        );
        assertNotNull(operation);

        LatchedResponseConsumer<Person> creationConsumer = LatchedResponseConsumer.instance();
        Amplify.API.mutate(
            PERSON_API_NAME,
            person,
            null,
            MutationType.CREATE,
            ResultListener.instance(creationConsumer, EmptyConsumer.of(Throwable.class))
        );
        creationConsumer.awaitResponseData();

        // Validate that subscription received the newly created person.
        Person firstPersonOnSubscription = streamItemConsumer.awaitResponseData();
        assertEquals(person.getFirstName(), firstPersonOnSubscription.getFirstName());
        assertEquals(person.getLastName(), firstPersonOnSubscription.getLastName());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        streamCompletionAction.awaitCall();
    }

    /**
     * Creates an object and tries to update it with a false condition on the existing data which should fail to
     * ensure mutation condition filtering works. Then it sends a correct condition to delete which should succeed.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void mutationFailsInvalidConditionAndPassesCorrectCondition() {
        LatchedResponseConsumer<Person> createdPersonConsumer = LatchedResponseConsumer.instance();
        Person person = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.married)
            .build();
        Amplify.API.mutate(
            PERSON_API_NAME,
            person,
            MutationType.CREATE,
            ResultListener.instance(createdPersonConsumer, EmptyConsumer.of(Throwable.class))
        );
        createdPersonConsumer.awaitResponseData();

        LatchedResponseConsumer<Person> updateConsumer = LatchedResponseConsumer.instance();
        Person updated = person.newBuilder()
            .age(30)
            .build();
        Amplify.API.mutate(
            PERSON_API_NAME,
            updated,
            Person.LAST_NAME.eq("Dandelion"),
            MutationType.UPDATE,
            ResultListener.instance(updateConsumer, EmptyConsumer.of(Throwable.class))
        );
        GraphQLResponse.Error firstError = updateConsumer.awaitErrorsInNextResponse().get(0);
        assertTrue(firstError.getMessage().contains("ConditionalCheckFailedException"));

        LatchedResponseConsumer<Person> deleteConsumer = LatchedResponseConsumer.instance();
        Amplify.API.mutate(
            PERSON_API_NAME,
            Person.justId(person.getId()),
            MutationType.DELETE,
            ResultListener.instance(deleteConsumer, EmptyConsumer.of(Throwable.class))
        );
        deleteConsumer.awaitResponseData();
    }

    /**
     * For a model having an {@link BelongsTo} relationship to another model, validate
     * successful query, mutations, subscription.
     * TODO: Add mutate with condition and list with predicate since those are the ones
     *       that actually use the original model name
     */
    @Test
    public void belongsToRelationship() {
        LatchedResponseConsumer<Projectfields> projectSubscriptionConsumer = LatchedResponseConsumer.instance();
        LatchedAction subscriptionCompletionAction = LatchedAction.instance();
        StreamListener<GraphQLResponse<Projectfields>> streamListener = StreamListener.instance(
            projectSubscriptionConsumer, EmptyConsumer.of(Throwable.class), subscriptionCompletionAction
        );

        GraphQLOperation<Projectfields> operation = Amplify.API.subscribe(
            PROJECT_API_NAME,
            Projectfields.class,
            SubscriptionType.ON_CREATE,
            streamListener
        );
        assertNotNull(operation);

        LatchedResponseConsumer<Team> teamCreationConsumer = LatchedResponseConsumer.instance();
        Team team = Team.builder().name("AWS Mobile SDK").build();
        Amplify.API.mutate(
            PROJECT_API_NAME,
            team,
            MutationType.CREATE,
            ResultListener.instance(teamCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        Team createdTeam = teamCreationConsumer.awaitResponseData();

        LatchedResponseConsumer<Projectfields> projectCreationConsumer = LatchedResponseConsumer.instance();
        Projectfields projectfields = Projectfields.builder()
            .name("API Codegen")
            .team(Team.justId(createdTeam.getId()))
            .build();
        Amplify.API.mutate(
            PROJECT_API_NAME,
            projectfields,
            MutationType.CREATE,
            ResultListener.instance(projectCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        Projectfields createdProjectfields = projectCreationConsumer.awaitResponseData();

        LatchedResponseConsumer<Projectfields> projectQueryConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(
            PROJECT_API_NAME,
            Projectfields.class,
            createdProjectfields.getId(),
            ResultListener.instance(projectQueryConsumer, EmptyConsumer.of(Throwable.class))
        );
        assertEquals(team, projectQueryConsumer.awaitResponseData().getTeam());

        // Validate that subscription received the newly created person.
        Projectfields firstProjectfieldsOnSubscription = projectSubscriptionConsumer.awaitResponseData();
        assertEquals(projectfields.getName(), firstProjectfieldsOnSubscription.getName());
        assertEquals(team, firstProjectfieldsOnSubscription.getTeam());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        subscriptionCompletionAction.awaitCall();
    }

    /**
     * Tests the code generation for HAS_MANY relationship.
     */
    @Test
    public void hasManyRelationship() {
        LatchedResponseConsumer<Blog> blogCreateConsumer = LatchedResponseConsumer.instance();
        Blog blog = Blog.builder()
            .name("All Things Amplify")
            .tags(Arrays.asList("amazon", "amplify", "framework", "software"))
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            blog,
            MutationType.CREATE,
            ResultListener.instance(blogCreateConsumer, EmptyConsumer.of(Throwable.class))
        );
        assertEquals(blog, blogCreateConsumer.awaitResponseData());

        LatchedResponseConsumer<Post> postCreateConsumer = LatchedResponseConsumer.instance();
        Post post = Post.builder()
            .title("Test 1")
            .blog(blog)
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            post,
            MutationType.CREATE,
            ResultListener.instance(postCreateConsumer, EmptyConsumer.of(Throwable.class))
        );
        Post postCreateResult = postCreateConsumer.awaitResponseData();
        assertEquals(post.getId(), postCreateResult.getId());
        assertEquals(post.getTitle(), postCreateResult.getTitle());
        assertEquals(post.getBlog().getId(), postCreateResult.getBlog().getId());
        assertEquals(post.getBlog().getName(), postCreateResult.getBlog().getName());
        assertEquals(post.getBlog().getTags(), postCreateResult.getBlog().getTags());

        LatchedResponseConsumer<Blog> blogGetConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(
            BLOG_API_NAME,
            Blog.class,
            blog.getId(),
            ResultListener.instance(blogGetConsumer, EmptyConsumer.of(Throwable.class))
        );
        Blog blogGetResult = blogGetConsumer.awaitResponseData();
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
        LatchedResponseConsumer<Blog> blogCreationConsumer = LatchedResponseConsumer.instance();
        Blog blog = Blog.builder()
            .name("Necessary blog for post")
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            blog,
            MutationType.CREATE,
            ResultListener.instance(blogCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        blogCreationConsumer.awaitResponseData();

        LatchedResponseConsumer<Post> postCreationConsumer = LatchedResponseConsumer.instance();
        Post post = Post.builder()
            .title("Test post")
            .blog(blog)
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            post,
            MutationType.CREATE,
            ResultListener.instance(postCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        postCreationConsumer.awaitResponseData();

        LatchedResponseConsumer<Rating> ratingCreationConsumer = LatchedResponseConsumer.instance();
        Rating rating = Rating.builder().stars(5).post(post).build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            rating,
            MutationType.CREATE,
            ResultListener.instance(ratingCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        assertEquals(post, ratingCreationConsumer.awaitResponseData().getPost());

        /*
        TODO: This condition should work. However there is a bug on the AppSync transformer side which
            sets up the HasOne / BelongsTo relationship as two independent BelongsTo relationships so it fails.
            Once the AppSync transformer bug is fixed, we can uncomment this part of the test.

        LatchedResponseConsumer<Post> postGetConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(
            BLOG_API_NAME,
            Post.class,
            post.getId(),
            ResultListener.instance(postGetConsumer, EmptyConsumer.of(Throwable.class))
        );
        assertEquals(rating, postGetListener.awaitResponseData().getRating());
         */
    }

    /**
     * Tests the code generation for a Many to Many relationship simulated through two HasMany relationships.
     */
    @Test
    public void manyToManyRelationship() {
        LatchedResponseConsumer<Blog> blogCreateConsumer = LatchedResponseConsumer.instance();
        Blog blog = Blog.builder()
            .name("Necessary blog for post")
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            blog,
            MutationType.CREATE,
            ResultListener.instance(blogCreateConsumer, EmptyConsumer.of(Throwable.class))
        );
        blogCreateConsumer.awaitResponseData();

        LatchedResponseConsumer<Post> postCreateConsumer = LatchedResponseConsumer.instance();
        Post post = Post.builder()
            .title("Test post")
            .blog(blog)
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            post,
            MutationType.CREATE,
            ResultListener.instance(postCreateConsumer, EmptyConsumer.of(Throwable.class))
        );
        postCreateConsumer.awaitResponseData();

        LatchedResponseConsumer<User> userCreateConsumer = LatchedResponseConsumer.instance();
        User user = User.builder()
            .username("Patches46")
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            user,
            MutationType.CREATE,
            ResultListener.instance(userCreateConsumer, EmptyConsumer.of(Throwable.class))
        );
        userCreateConsumer.awaitResponseData();

        LatchedResponseConsumer<PostEditor> postEditorCreationConsumer = LatchedResponseConsumer.instance();
        PostEditor editor = PostEditor.builder()
            .post(post)
            .editor(user)
            .build();
        Amplify.API.mutate(
            BLOG_API_NAME,
            editor,
            MutationType.CREATE,
            ResultListener.instance(postEditorCreationConsumer, EmptyConsumer.of(Throwable.class))
        );
        postEditorCreationConsumer.awaitResponseData();

        LatchedResponseConsumer<Post> postGetConsumer = LatchedResponseConsumer.instance();
        LatchedResponseConsumer<User> userGetConsumer = LatchedResponseConsumer.instance();
        Amplify.API.query(
            BLOG_API_NAME,
            Post.class,
            post.getId(),
            ResultListener.instance(postGetConsumer, EmptyConsumer.of(Throwable.class))
        );
        Amplify.API.query(
            BLOG_API_NAME,
            User.class,
            user.getId(),
            ResultListener.instance(userGetConsumer, EmptyConsumer.of(Throwable.class))
        );
        Post postGetResult = postGetConsumer.awaitResponseData();
        User userGetResult = userGetConsumer.awaitResponseData();

        assertEquals(1, postGetResult.getEditors().size());
        assertEquals(user, postGetResult.getEditors().get(0).getEditor());
        assertEquals(1, userGetResult.getPosts().size());
        assertEquals(post.getTitle(), userGetResult.getPosts().get(0).getPost().getTitle());
        assertEquals(post.getId(), userGetResult.getPosts().get(0).getPost().getId());
    }
}
