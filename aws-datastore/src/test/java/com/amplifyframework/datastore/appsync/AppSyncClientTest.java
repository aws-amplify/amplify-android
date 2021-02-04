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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.meeting.Meeting;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.util.TypeMaker;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link AppSyncClient}.
 */
@SuppressWarnings("unchecked") // Mockito matchers, i.e. any(Raw.class), etc.
@RunWith(RobolectricTestRunner.class)
public final class AppSyncClientTest {

    private ApiCategoryBehavior api;
    private AppSync endpoint;

    /**
     * Setup an {@link AppSyncClient} instance, under test.
     * Mock its {@link ApiCategoryBehavior} dependency, so we can spoof
     * responses.
     */
    @Before
    public void setup() {
        this.api = mock(ApiCategoryBehavior.class);
        this.endpoint = AppSyncClient.via(api);

        // We need it to response with **something** by default.
        // Use this same method to send more interesting test values back...
        PaginatedResult<ModelWithMetadata<BlogOwner>> data = new PaginatedResult<>(Collections.emptyList(), null);
        mockApiResponse(new GraphQLResponse<>(data, Collections.emptyList()));
    }

    /**
     * Validates the construction of a base-sync query.
     * @throws JSONException On bad request JSON found in API category call
     * @throws DataStoreException If no valid response returned from AppSync endpoint during sync
     * @throws AmplifyException On failure to arrange model schema
     */
    @Test
    public void validateBaseSyncQueryGen() throws JSONException, AmplifyException {
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        Await.result(
            (
                Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>>> onResult,
                Consumer<DataStoreException> onError
            ) -> {
                try {
                    GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> request =
                            endpoint.buildSyncRequest(schema, null, null, QueryPredicates.all());
                    endpoint.sync(request, onResult, onError);
                } catch (DataStoreException datastoreException) {
                    onError.accept(datastoreException);
                }
            }
        );

        // Now, capture the request argument on API, so we can see what was passed.
        // Recall that we pass a raw doc to API.
        ArgumentCaptor<GraphQLRequest<ModelWithMetadata<BlogOwner>>> requestCaptor =
                ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(api).query(requestCaptor.capture(), any(Consumer.class), any(Consumer.class));
        GraphQLRequest<ModelWithMetadata<BlogOwner>> capturedRequest = requestCaptor.getValue();

        Type type = TypeMaker.getParameterizedType(PaginatedResult.class, ModelWithMetadata.class, BlogOwner.class);
        assertEquals(type, capturedRequest.getResponseType());

        // The request was sent as JSON. It has a null variables field, and a present query field.
        JSONAssert.assertEquals(
            Resources.readAsString("base-sync-request-document-for-blog-owner.txt"),
            capturedRequest.getContent(),
            true
        );
    }

    /**
     * Configures the API mock to return a particular response.
     * @param arrangedApiResponse Some response you want the API to return
     */
    private void mockApiResponse(GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>> arrangedApiResponse) {
        doAnswer(invocation -> {
            final int argPositionOfResponseConsumer = 1; // second/middle arg, starting from arg 0
            Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>>> onResponse =
                invocation.getArgument(argPositionOfResponseConsumer);
            onResponse.accept(arrangedApiResponse);
            return mock(GraphQLOperation.class);
        }).when(api).query(
            any(GraphQLRequest.class),
            any(Consumer.class),
            any(Consumer.class)
        );
    }

    /**
     * Validates sync query is constructed with all expected variables.
     * @throws AmplifyException On failure to build the sync request.
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     */
    @Test
    public void validateSyncQueryIsBuiltWithLimitLastSyncAndFilter() throws AmplifyException, JSONException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(BlogOwner.class);
        GraphQLRequest<PaginatedResult<ModelWithMetadata<BlogOwner>>> request = endpoint
                .buildSyncRequest(modelSchema, 123_412_341L, 342, BlogOwner.NAME.beginsWith("J"));
        JSONAssert.assertEquals(Resources.readAsString("sync-request-with-predicate.txt"), request.getContent(), true);
    }

    /**
     * Validates date serialization when creating mutation.
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     * @throws AmplifyException from ModelSchema.fromModelClass to convert model to schema
     */
    @Test
    public void validateUpdateMutationWithDates() throws JSONException, AmplifyException {
        // Act: build a mutation to create a Meeting
        final Meeting meeting = Meeting.builder()
                .name("meeting1")
                .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
                .date(new Temporal.Date("2001-02-03"))
                .dateTime(new Temporal.DateTime("2001-02-03T01:30:15Z"))
                .time(new Temporal.Time("01:22:33"))
                .timestamp(new Temporal.Timestamp(1234567890000L, TimeUnit.MILLISECONDS))
                .build();
        endpoint.update(meeting, ModelSchema.fromModelClass(Meeting.class), 1, response -> { }, error -> { });

        // Now, capture the request argument on API, so we can see what was passed.
        ArgumentCaptor<GraphQLRequest<ModelWithMetadata<Meeting>>> requestCaptor =
                ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(api).mutate(requestCaptor.capture(), any(Consumer.class), any(Consumer.class));
        GraphQLRequest<ModelWithMetadata<Meeting>> capturedRequest = requestCaptor.getValue();

        // Assert
        assertEquals(TypeMaker.getParameterizedType(ModelWithMetadata.class, Meeting.class),
                capturedRequest.getResponseType());
        JSONAssert.assertEquals(Resources.readAsString("update-meeting.txt"),
                capturedRequest.getContent(), true);
    }
}
