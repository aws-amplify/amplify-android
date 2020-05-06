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
import com.amplifyframework.core.model.AWSDate;
import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.AWSTime;
import com.amplifyframework.core.model.AWSTimestamp;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Tests the {@link AppSyncGraphQLRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AppSyncGraphQLRequestFactoryTest {

    /**
     * Validate construction of a GraphQL query from a class and an object ID.
     * @throws ApiException from possible query builder failure
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildQueryFromClassAndId() throws ApiException, JSONException {
        // Arrange a hard-coded ID as found int the expected data file.
        final String uuidForExpectedQuery = "9a1bee5c-248f-4746-a7da-58f703ec572d";

        // Act: create a request
        GraphQLRequest<Person> request =
            AppSyncGraphQLRequestFactory.buildQuery(Person.class, uuidForExpectedQuery);

        // Assert: content is expected content
        JSONAssert.assertEquals(
            Resources.readAsString("query-for-person-by-id.txt"),
            request.getContent(),
            true
        );
    }

    /**
     * Validate construction of a GraphQL query from a class and a predicate.
     * @throws AmplifyException from buildQuery().
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildQueryFromClassAndPredicate() throws AmplifyException, JSONException {
        // Arrange - ID for predicate, hard-coded version of what's in the expected txt
        final String expectedId = "aca4a318-181e-445a-beb9-7656f5005c7b";

        // Act: generate query
        GraphQLRequest<Person> request =
            AppSyncGraphQLRequestFactory.buildQuery(Person.class, Person.ID.eq(expectedId));

        // Validate request is expected request
        JSONAssert.assertEquals(
            Resources.readAsString("query-person-by-predicate.txt"),
            request.getContent(),
            true
        );
    }

    /**
     * Validates construction of a mutation query from a Person instance, a predicate,
     * and an {@link MutationType}.
     * @throws AmplifyException From buildMutation().
     * @throws JSONException from JSONAssert.assertEquals
     */
    @SuppressWarnings("deprecation")
    @Test
    public void buildMutationFromPredicateAndMutationType() throws AmplifyException, JSONException {
        // Arrange a person to delete, using UUID from test resource file
        final String expectedId = "dfcdac69-0662-41df-a67b-48c62a023f97";
        final Person tony = Person.builder()
            .firstName("Tony")
            .lastName("Swanson")
            .age(19)
            .dob(new Date(2000, 1, 15))
            .id(expectedId)
            .relationship(MaritalStatus.single)
            .build();

        // Act: build a mutation
        GraphQLRequest<Person> requestToDeleteTony = AppSyncGraphQLRequestFactory.buildMutation(
            tony, Person.ID.beginsWith("e6"), MutationType.DELETE
        );

        // Assert: expected is actual
        JSONAssert.assertEquals(
            Resources.readAsString("mutate-person-with-predicate.txt"),
            requestToDeleteTony.getContent(),
            true
        );
    }

    /**
     * Validates construction of a subscription request using a class and an
     * {@link SubscriptionType}.
     * @throws ApiException from subscription builder potential failure
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildSubscriptionFromClassAndSubscriptionType() throws ApiException, JSONException {
        GraphQLRequest<Person> subscriptionRequest = AppSyncGraphQLRequestFactory.buildSubscription(
            Person.class, SubscriptionType.ON_CREATE
        );

        JSONAssert.assertEquals(
            Resources.readAsString("subscription-request-for-on-create.txt"),
            subscriptionRequest.getContent(),
            true
        );
    }

    /**
     * Validates date serialization when creating GraphQLRequest.
     * @throws ApiException from buildMutation potential failure
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     */
    @Test
    public void validateDateSerializer() throws ApiException, JSONException {
        // Create expectation
        final Meeting meeting1 = Meeting.builder()
                .name("meeting1")
                .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
                .date(new AWSDate("2001-02-03"))
                .dateTime(new AWSDateTime("2001-02-03T01:30:15Z"))
                .time(new AWSTime("01:22:33"))
                .timestamp(new AWSTimestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build();

        // Act: build a mutation to create a Meeting
        GraphQLRequest<Meeting> requestToCreateMeeting1 =
                AppSyncGraphQLRequestFactory.buildMutation(meeting1, null, MutationType.CREATE);

        // Assert: expected is actual
        JSONAssert.assertEquals(Resources.readAsString("create-meeting1.txt"),
                requestToCreateMeeting1.getContent(), true);
    }
}
