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
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.ecommerce.Item;
import com.amplifyframework.testmodels.ecommerce.Status;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AppSyncGraphQLRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AppSyncGraphQLRequestFactoryTest {

    /**
     * Validate construction of a GraphQL query from a class and an object ID.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildQueryFromClassAndId() throws JSONException {
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
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildQueryFromClassAndPredicate() throws JSONException {
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
     * Validates construction of a delete mutation query from a Person instance, a predicate.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @SuppressWarnings("deprecation")
    @Test
    public void buildDeleteMutationFromPredicate() throws JSONException {
        // Arrange a person to delete, using UUID from test resource file
        final String expectedId = "dfcdac69-0662-41df-a67b-48c62a023f97";
        final Person tony = Person.builder()
                                .firstName("Tony")
                                .lastName("Swanson")
                                .age(19)
                                .dob(new Temporal.Date("2000-01-15"))
                                .id(expectedId)
                                .relationship(MaritalStatus.single)
                                .build();

        // Act: build a mutation
        GraphQLRequest<Person> requestToDeleteTony = AppSyncGraphQLRequestFactory.buildMutation(
            tony, Person.ID.beginsWith("e6"), MutationType.DELETE
        );

        // Assert: expected is actual
        JSONAssert.assertEquals(
            Resources.readAsString("delete-person-with-predicate.txt"),
            requestToDeleteTony.getContent(),
            true
        );
    }

    /**
     * Checks that we're getting the expected output for a delete mutation for an object with a custom primary key.
     * @throws DataStoreException If the output does not match.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateDeleteWithCustomPrimaryKey() throws AmplifyException, JSONException {
        final Item item = Item.builder()
                              .orderId("123a7asa")
                              .status(Status.IN_TRANSIT)
                              .createdAt(new Temporal.DateTime("2021-04-20T15:20:32.651Z"))
                              .name("Gummy Bears")
                              .build();
        JSONAssert.assertEquals(
            Resources.readAsString("delete-item.txt"),
            AppSyncGraphQLRequestFactory.buildMutation(item, QueryPredicates.all(), MutationType.DELETE).getContent(),
            true
        );
    }

    /**
     * Validates construction of an update mutation query from a Person instance, a predicate.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildUpdateMutationFromPredicate() throws JSONException {
        // Arrange a person to delete, using UUID from test resource file
        final String expectedId = "dfcdac69-0662-41df-a67b-48c62a023f97";
        final Person tony = Person.builder()
                                .firstName("Tony")
                                .lastName("Swanson")
                                .age(19)
                                .dob(new Temporal.Date("2000-01-15"))
                                .id(expectedId)
                                .relationship(MaritalStatus.single)
                                .build();

        // Act: build a mutation
        GraphQLRequest<Person> requestToDeleteTony = AppSyncGraphQLRequestFactory.buildMutation(
            tony, Person.ID.beginsWith("e6"), MutationType.UPDATE
        );

        // Assert: expected is actual
        JSONAssert.assertEquals(
            Resources.readAsString("update-person-with-predicate.txt"),
            requestToDeleteTony.getContent(),
            true
        );
    }

    /**
     * Validates construction of a subscription request using a class and an {@link SubscriptionType}.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void buildSubscriptionFromClassAndSubscriptionType() throws JSONException {
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
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     */
    @Test
    public void validateDateSerializer() throws JSONException {
        // Create expectation
        final Meeting meeting1 = Meeting.builder()
                                     .name("meeting1")
                                     .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
                                     .date(new Temporal.Date("2001-02-03"))
                                     .dateTime(new Temporal.DateTime("2001-02-03T01:30:15Z"))
                                     .time(new Temporal.Time("01:22:33"))
                                     .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                                     .build();

        // Act: build a mutation to create a Meeting
        GraphQLRequest<Meeting> requestToCreateMeeting1 =
            AppSyncGraphQLRequestFactory.buildMutation(meeting1, QueryPredicates.all(), MutationType.CREATE);

        // Assert: expected is actual
        JSONAssert.assertEquals(Resources.readAsString("create-meeting1.txt"),
            requestToCreateMeeting1.getContent(), true);
    }

    /**
     * Verify that the owner field is removed if the value is null.
     */
    @Test
    public void ownerFieldIsRemovedIfNull() {
        // Expect
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "111");
        expected.put("description", "Mop the floor");

        // Act
        Todo todo = new Todo("111", "Mop the floor", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) AppSyncGraphQLRequestFactory.buildMutation(
            todo,
            QueryPredicates.all(),
            MutationType.CREATE
        ).getVariables().get("input");

        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Verify that the owner field is NOT removed if the value is set..
     */
    @Test
    public void ownerFieldIsNotRemovedIfSet() {
        // Expect
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "111");
        expected.put("description", "Mop the floor");
        expected.put("owner", "johndoe");

        // Act
        Todo todo = new Todo("111", "Mop the floor", "johndoe");
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) AppSyncGraphQLRequestFactory.buildMutation(
            todo,
            QueryPredicates.all(),
            MutationType.CREATE).getVariables().get("input");

        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Verify that a nullable relation can have a null (missing) value in the GraphQL request.
     */
    @Test
    public void nullableAssociationCanBeNull() {
        // Expect
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "abc");
        expected.put("text", "text");

        // Act
        // Create a Note that doesn't have a value for the optional measurement relation.
        Note note = new Note("abc", "text", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) AppSyncGraphQLRequestFactory.buildMutation(
            note,
            QueryPredicates.all(),
            MutationType.CREATE
        ).getVariables().get("input");

        // Assert
        assertEquals(expected, actual);
    }

    @ModelConfig(authRules = {@AuthRule(allow = AuthStrategy.OWNER)})
    static final class Todo implements Model {
        @com.amplifyframework.core.model.annotations.ModelField(targetType = "ID", isRequired = true)
        private final String id;

        @com.amplifyframework.core.model.annotations.ModelField(isRequired = true)
        private final String description;

        @com.amplifyframework.core.model.annotations.ModelField
        private final String owner;

        @SuppressWarnings("ParameterName")
            // checkstyle wants variable names to be >2 chars, but id is only 2.
        Todo(String id, String description, String owner) {
            this.id = id;
            this.description = description;
            this.owner = owner;
        }

        public String getId() {
            return "111";
        }
    }

    @ModelConfig(authRules = {@AuthRule(allow = AuthStrategy.OWNER)})
    @Index(name = "byMeasurement", fields = "measurement_id")
    static final class Note implements Model {
        @ModelField(targetType = "ID", isRequired = true)
        private final String id;
        @ModelField(targetType = "String")
        private final String text;

        @ModelField(targetType = "Measurement")
        @BelongsTo(targetName = "measurement_id", targetNames = "measurement_id", type = Measurement.class)
        private final Measurement measurement;

        private Note(String idString, String text, Measurement measurement) {
            this.id = idString;
            this.text = text;
            this.measurement = measurement;
        }

        public String getId() {
            return id;
        }
    }

    @ModelConfig(authRules = {@AuthRule(allow = AuthStrategy.OWNER)})
    static final class Measurement implements Model {
        @ModelField(targetType = "ID")
        private final String id;

        @ModelField(targetType = "Int")
        private final Integer value;

        @ModelField(targetType = "Note")
        @HasMany(associatedWith = "Measurement", type = Note.class)
        private final List<Note> notes = null;

        Measurement(String idString, Integer value) {
            this.id = idString;
            this.value = value;
        }

        public String getId() {
            return id;
        }
    }
}
