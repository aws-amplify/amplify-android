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

import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.testmodels.MaritalStatus;
import com.amplifyframework.testmodels.Person;
import com.amplifyframework.testmodels.Project;
import com.amplifyframework.testmodels.Team;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TODO: document how to configure a remote endpoint that can accomodate this test.
 */
public final class CodeGenerationInstrumentationTest {
    private static final String PERSON_API_NAME = "personApi";
    private static final String PROJECT_API_NAME = "projectApi";

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     */
    @BeforeClass
    public static void onceBeforeTests() {
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
        LatchedSingleResponseListener<Person> mutationListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<Person> queryListener = new LatchedSingleResponseListener<>();

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
            mutationListener
        );

        GraphQLResponse<Person> mutationResponse = mutationListener.awaitTerminalEvent().getResponse();
        assertFalse(mutationResponse.hasErrors());
        assertTrue(mutationResponse.hasData());

        Amplify.API.query(
            PERSON_API_NAME,
            Person.class,
            mutationResponse.getData().getId(),
            queryListener
        );

        GraphQLResponse<Person> queryResponse = queryListener.awaitTerminalEvent().getResponse();
        assertEquals(mutationResponse.getData(), queryResponse.getData());
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

        GraphQLResponse<Iterable<Person>> queryResponse =
            queryListener.awaitTerminalEvent().assertNoError().assertResponse().getResponse();
        assertTrue(queryResponse.hasData());
        assertFalse(queryResponse.hasErrors());

        for (Person person : queryResponse.getData()) {
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

        Amplify.API.query(
                PERSON_API_NAME,
                Person.class,
                queryListener
        );

        GraphQLResponse<Iterable<Person>> queryResponse =
                queryListener.awaitTerminalEvent().assertNoError().assertResponse().getResponse();
        assertTrue(queryResponse.hasData());
        assertFalse(queryResponse.hasErrors());

        // Test table should always have at least three items
        int count = 0;
        Iterator<Person> iterator = queryResponse.getData().iterator();
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
                null,
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
        List<GraphQLResponse<Person>> subscriptionResponses = streamListener.awaitItems();
        assertEquals(1, subscriptionResponses.size());
        assertFalse(subscriptionResponses.get(0).hasErrors());
        Person responsePerson = subscriptionResponses.get(0).getData();
        assertEquals(person.getFirstName(), responsePerson.getFirstName());
        assertEquals(person.getLastName(), responsePerson.getLastName());

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
        LatchedSingleResponseListener<Person> createListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<Person> updateListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<Person> deleteListener = new LatchedSingleResponseListener<>();

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

        GraphQLResponse<Person> createResponse = createListener.awaitTerminalEvent().getResponse();
        assertFalse(createResponse.hasErrors());
        assertTrue(createResponse.hasData());

        Person updated = person.newBuilder().age(30).build();

        Amplify.API.mutate(
                PERSON_API_NAME,
                updated,
                Person.LAST_NAME.eq("Dandelion"),
                MutationType.UPDATE,
                updateListener
        );

        GraphQLResponse<Person> updateResponse = updateListener.awaitTerminalEvent().getResponse();
        assertTrue(updateResponse.hasErrors());
        assertTrue(updateResponse.getErrors().get(0).getMessage().contains("ConditionalCheckFailedException"));

        Amplify.API.mutate(
                PERSON_API_NAME,
                Person.justId(person.getId()),
                MutationType.DELETE,
                deleteListener
        );

        GraphQLResponse<Person> deleteResponse = deleteListener.awaitTerminalEvent().getResponse();
        assertFalse(deleteResponse.hasErrors());
        assertTrue(deleteResponse.hasData());
    }

    /**
     * Creates a Team object, then creates a Project object with a belongsTo relationship with that Team and ensures
     * that Query and Subscription on the Project both get the original Team object.
     * @throws Throwable If we timeout while talking to the endpoint, or if any response comes back invalid
     */
    @Test
    public void belongsToCreateQuerySubscribe() throws Throwable {
        LatchedSingleResponseListener<Team> teamMutationListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<Project> projectMutationListener = new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<Project> projectQueryListener = new LatchedSingleResponseListener<>();
        LatchedResponseStreamListener<Project> projectSubscriptionListener = new LatchedResponseStreamListener<>(1);

        GraphQLOperation<Project> operation = Amplify.API.subscribe(
                PROJECT_API_NAME,
                Project.class,
                null,
                SubscriptionType.ON_CREATE,
                projectSubscriptionListener
        );

        Team team = Team.builder().name("AWS Mobile SDK").build();

        Amplify.API.mutate(
                PROJECT_API_NAME,
                team,
                MutationType.CREATE,
                teamMutationListener
        );

        GraphQLResponse<Team> teamMutationResponse = teamMutationListener.awaitTerminalEvent().getResponse();
        assertFalse(teamMutationResponse.hasErrors());
        assertTrue(teamMutationResponse.hasData());

        Project project = Project
                .builder()
                .name("API Codegen")
                .team(Team.justId(teamMutationResponse.getData().getId()))
                .build();

        Amplify.API.mutate(
                PROJECT_API_NAME,
                project,
                MutationType.CREATE,
                projectMutationListener
        );

        GraphQLResponse<Project> projectMutationResponse = projectMutationListener.awaitTerminalEvent().getResponse();
        assertFalse(projectMutationResponse.hasErrors());
        assertTrue(projectMutationResponse.hasData());

        Amplify.API.query(
                PROJECT_API_NAME,
                Project.class,
                projectMutationResponse.getData().getId(),
                projectQueryListener
        );

        GraphQLResponse<Project> projectQueryResponse = projectQueryListener.awaitTerminalEvent().getResponse();
        assertEquals(team, projectQueryResponse.getData().getTeam());

        // Validate that subscription received the newly created person.
        List<GraphQLResponse<Project>> subscriptionResponses = projectSubscriptionListener.awaitItems();
        assertEquals(1, subscriptionResponses.size());
        assertFalse(subscriptionResponses.get(0).hasErrors());
        Project responseProject = subscriptionResponses.get(0).getData();
        assertEquals(project.getName(), responseProject.getName());
        assertEquals(team, responseProject.getTeam());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        projectSubscriptionListener.awaitCompletion();
    }
}
