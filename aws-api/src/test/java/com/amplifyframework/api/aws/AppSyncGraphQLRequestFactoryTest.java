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
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.types.scalar.AWSDate;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AppSyncGraphQLRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AppSyncGraphQLRequestFactoryTest {

    /**
     * Validate construction of a GraphQL query from a class and an object ID.
     * @throws ApiException from possible query builder failure
     */
    @Test
    public void buildQueryFromClassAndId() throws ApiException {
        // Arrange a hard-coded ID as found int the expected data file.
        final String uuidForExpectedQuery = "9a1bee5c-248f-4746-a7da-58f703ec572d";

        // Act: create a request
        GraphQLRequest<Person> request =
            AppSyncGraphQLRequestFactory.buildQuery(Person.class, uuidForExpectedQuery);

        // Assert: content is expected content
        assertEquals(
            Resources.readAsString("query-for-person-by-id.txt"),
            request.getContent()
        );
    }

    /**
     * Validate construction of a GraphQL query from a class and a predicate.
     * @throws AmplifyException from buildQuery().
     */
    @Test
    public void buildQueryFromClassAndPredicate() throws AmplifyException {
        // Arrange - ID for predicate, hard-coded version of what's in the expected txt
        final String expectedId = "aca4a318-181e-445a-beb9-7656f5005c7b";

        // Act: generate query
        GraphQLRequest<Person> request =
            AppSyncGraphQLRequestFactory.buildQuery(Person.class, Person.ID.eq(expectedId));

        // Validate request is expected request
        assertEquals(
            Resources.readAsString("query-person-by-predicate.txt"),
            request.getContent()
        );
    }

    /**
     * Validates construction of a mutation query from a Person instance, a predicate,
     * and an {@link MutationType}.
     * @throws AmplifyException From buildMutation().
     */
    @SuppressWarnings({"deprecation", "checkstyle:MagicNumber"})
    @Test
    public void buildMutationFromPredicateAndMutationType() throws AmplifyException {
        // Arrange a person to delete, using UUID from test resource file
        final String expectedId = "dfcdac69-0662-41df-a67b-48c62a023f97";
        final Person tony = Person.builder()
            .firstName("Tony")
            .lastName("Swanson")
            .age(19)
            .dob(new AWSDate(new Date(2000, 1, 15)))
            .id(expectedId)
            .relationship(MaritalStatus.single)
            .build();

        // Act: build a mutation
        GraphQLRequest<Person> requestToDeleteTony = AppSyncGraphQLRequestFactory.buildMutation(
            tony, Person.ID.beginsWith("e6"), MutationType.DELETE
        );

        // Assert: expected is actual
        assertEquals(
            Resources.readAsString("mutate-person-with-predicate.txt"),
            requestToDeleteTony.getContent()
        );
    }

    /**
     * Validates construction of a subscription request using a class and an
     * {@link SubscriptionType}.
     * @throws ApiException from subscription builder potential failure
     */
    @Test
    public void buildSubscriptionFromClassAndSubscriptionType() throws ApiException {
        GraphQLRequest<Person> subscriptionRequest = AppSyncGraphQLRequestFactory.buildSubscription(
            Person.class, SubscriptionType.ON_CREATE
        );

        assertEquals(
            Resources.readAsString("subscription-request-for-on-create.txt"),
            subscriptionRequest.getContent()
        );
    }
}
