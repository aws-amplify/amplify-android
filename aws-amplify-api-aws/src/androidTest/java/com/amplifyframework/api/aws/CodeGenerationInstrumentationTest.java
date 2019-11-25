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

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TODO: document how to configure a remote endpoint that can accomodate this test.
 */
public final class CodeGenerationInstrumentationTest {
    private static final String API_NAME = "personApi";

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

        //noinspection ConstantConditions TODO: predicate isn't supposed to be null...
        Amplify.API.mutate(
            API_NAME,
            person,
            null,
            MutationType.CREATE,
            mutationListener
        );

        GraphQLResponse<Person> mutationResponse = mutationListener.awaitTerminalEvent().getResponse();
        assertFalse(mutationResponse.hasErrors());
        assertTrue(mutationResponse.hasData());

        Amplify.API.query(
            API_NAME,
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
    public void queryList() {
        LatchedSingleResponseListener<Iterable<Person>> queryListener = new LatchedSingleResponseListener<>();

        Amplify.API.query(
            API_NAME,
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
     * Tests that a subscription can receive an event when a create mutation takes place.
     * @throws Throwable If we timeout while talking to the endpoint,
     *                   or if any response comes back invalid
     */
    @Test
    public void subscribeReceivesMutationEvent() throws Throwable {
        Person person = Person.builder().firstName("John").lastName("Doe").build();
        LatchedResponseStreamListener<Person> streamListener = new LatchedResponseStreamListener<>(1);

        GraphQLOperation<Person> operation = Amplify.API.subscribe(
                API_NAME,
                Person.class,
                null,
                SubscriptionType.ON_CREATE,
                streamListener
        );

        Amplify.API.mutate(
                API_NAME,
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
}
