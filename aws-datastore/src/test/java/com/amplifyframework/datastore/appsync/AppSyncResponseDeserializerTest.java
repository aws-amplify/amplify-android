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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testutils.Resources;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class AppSyncResponseDeserializerTest {
    private AppSyncResponseDeserializer appSyncResponseDeserializer;

    /**
     * Set up the object under test, an AppSyncResponseDeserializer.
     */
    @Before
    public void setup() {
        this.appSyncResponseDeserializer = new AppSyncResponseDeserializer();
    }

    /**
     * {@link Temporal.Date}, {@link Temporal.DateTime}, {@link Temporal.Time}, and {@link Temporal.Timestamp} all
     * have different JSON representations. It must be possible to recover the Java type which
     * models the JSON representation of each.
     * @throws ApiException If the response factory fails to construct a response,
     *                      perhaps because deserialization to one of these types
     *                      has failed.
     */
    @Test
    public void awsDateTypesCanBeDeserialized() {
        // Expect
        String meetingId = "45a5f600-8aa8-41ac-a529-aed75036f5be";
        Meeting meeting = Meeting.builder()
                .name("meeting0")
                .id(meetingId)
                .date(new Temporal.Date("2001-02-03"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30Z"))
                .time(new Temporal.Time("01:22"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build();
        Boolean deleted = false;
        Integer version = 42;
        Long lastChangedAt = Long.valueOf(1594858827);
        ModelMetadata modelMetadata = new ModelMetadata(meetingId, deleted, version, lastChangedAt);
        ModelWithMetadata<Meeting> modelWithMetadata = new ModelWithMetadata<>(meeting, modelMetadata);
        GraphQLResponse<ModelWithMetadata<Meeting>> expected = new GraphQLResponse<>(modelWithMetadata, null);

        // Act
        final String responseString = Resources.readAsString("meeting-response.json");

        final GraphQLResponse<ModelWithMetadata<Meeting>> response =
                appSyncResponseDeserializer.deserialize(responseString, Meeting.class);

        // Assert
        assertEquals(expected, response);
    }
}
