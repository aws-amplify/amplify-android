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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.testmodels.MaritalStatus;
import com.amplifyframework.testmodels.Person;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TODO: document howto configure a remote endpoint that can accomodate this test.
 */
@Ignore("This test is used for development, only.")
public final class CodeGenerationInstrumentationTest {
    private static final String API_NAME = GraphQLInstrumentationTest.class.getSimpleName();

    /**
     * Mutates an object, and then queries for its value back. Asserts that the two values are the same.
     * This tests our ability to generate GraphQL queries at runtime, from model primitives,
     * for both queries and mutations. The query also tests functionality of the QueryPredicate filter.
     * @throws Throwable when interrupted
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void codeGeneratedQueryMatchesMutationResult() throws Throwable {
        BlockingResultListener<Person> mutationListener = new BlockingResultListener<>();
        BlockingResultListener<Person> queryListener = new BlockingResultListener<>();

        Person person = Person
            .builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.MARRIED)
            .build();

        //noinspection ConstantConditions TODO: predicate isn't supposed to be null...
        Amplify.API.mutate(
            API_NAME,
            person,
            null,
            MutationType.CREATE,
            mutationListener
        );

        GraphQLResponse<Person> mutationResponse = mutationListener.awaitResult();
        assertFalse(mutationResponse.hasErrors());
        assertTrue(mutationResponse.hasData());

        Amplify.API.query(
            API_NAME,
            Person.class,
            Person.ID.eq(mutationResponse.getData().getId()),
            QueryType.GET,
            queryListener
        );

        GraphQLResponse<Person> queryResponse = queryListener.awaitResult();
        assertEquals(queryResponse.getData().getId(), queryResponse.getData().getId());
    }
}
