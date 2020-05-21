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

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Resources;

import com.google.gson.reflect.TypeToken;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        mockApiResponse(new GraphQLResponse<>(new ArrayList<>(), new ArrayList<>()));
    }

    /**
     * Validates the construction of a base-sync query.
     * @throws JSONException On bad request JSON found in API category call
     * @throws DataStoreException If no valid response returned from AppSync endpoint during sync
     */
    @Test
    public void validateBaseSyncQueryGen() throws JSONException, DataStoreException {
        //noinspection CodeBlock2Expr
        Await.result(
            (
                Consumer<GraphQLResponse<Iterable<ModelWithMetadata<BlogOwner>>>> onResult,
                Consumer<DataStoreException> onError
            ) -> {
                endpoint.sync(BlogOwner.class, null, onResult, onError);
            }
        );

        // Now, capture the request argument on API, so we can see what was passed.
        // Recall that we pass a raw doc to API.
        ArgumentCaptor<GraphQLRequest<String>> requestCaptor = ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(api).query(requestCaptor.capture(), any(Consumer.class), any(Consumer.class));
        GraphQLRequest<String> capturedRequest = requestCaptor.getValue();

        Type type = TypeToken.getParameterized(Iterable.class, String.class).getType();
        assertEquals(type, capturedRequest.getResponseType());

        // The request was sent as JSON. It has a null variables field, and a present query field.
        JSONObject requestJson = new JSONObject(capturedRequest.getContent());
        assertTrue(requestJson.has("variables"));
        assertTrue(requestJson.isNull("variables"));
        assertTrue(requestJson.has("query"));
        assertEquals(
            Resources.readAsString("base-sync-request-document-for-blog-owner.txt"),
            requestJson.getString("query")
        );
    }

    /**
     * Configures the API mock to return a particular response.
     * @param arrangedApiResponse Some response you want the API to return
     */
    private void mockApiResponse(GraphQLResponse<Iterable<String>> arrangedApiResponse) {
        doAnswer(invocation -> {
            final int argPositionOfResponseConsumer = 1; // second/middle arg, starting from arg 0
            Consumer<GraphQLResponse<Iterable<String>>> onResponse =
                invocation.getArgument(argPositionOfResponseConsumer);
            onResponse.accept(arrangedApiResponse);
            return mock(GraphQLOperation.class);
        }).when(api).query(
            any(GraphQLRequest.class),
            any(Consumer.class),
            any(Consumer.class)
        );
    }
}
