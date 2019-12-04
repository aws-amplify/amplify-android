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

package com.amplifyframework.datastore.network;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.LatchedSingleResponseListener;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link AppSyncApi}.
 */
@SuppressWarnings("unchecked") // most matchers ...
@RunWith(RobolectricTestRunner.class)
public final class AppSyncApiTest {

    private ApiCategoryBehavior api;
    private AppSyncEndpoint endpoint;

    /**
     * Setup an {@link AppSyncApi} instance, under test.
     * Mock its {@link ApiCategoryBehavior} dependency, so we can spoof
     * responses.
     */
    @Before
    public void setup() {
        this.api = mock(ApiCategoryBehavior.class);
        this.endpoint = new AppSyncApi(api);

        // We need it to response with **something** by default.
        // Use this same method to send more interesting test values back...
        mockApiResponse(new GraphQLResponse<>(new ArrayList<>(), new ArrayList<>()));
    }

    /**
     * Validates the construction of a base-sync query.
     * @throws JSONException On bad request JSON found in API category call
     */
    @Test
    public void validateBaseSyncQueryGen() throws JSONException {
        // Request a sync.
        final SyncListener<BlogOwner> syncListener = SyncListener.instance();
        endpoint.sync(BlogOwner.class, null, syncListener);
        syncListener.awaitSuccessResponse();

        // Now, capture the request argument on API, so we can see what was passed.
        // Recall that we pass a raw doc to API.
        ArgumentCaptor<GraphQLRequest<String>> requestCaptor = ArgumentCaptor.forClass(GraphQLRequest.class);
        verify(api).query(requestCaptor.capture(), any(ResultListener.class));
        GraphQLRequest<String> capturedRequest = requestCaptor.getValue();

        assertEquals(String.class, capturedRequest.getModelClass());

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
            final int argPositionOfResultListener = 1; // second and final arg, starting from arg 0
            ResultListener<GraphQLResponse<Iterable<String>>> listener =
                invocation.getArgument(argPositionOfResultListener);
            listener.onResult(arrangedApiResponse);
            return mock(GraphQLOperation.class);
        }).when(api).query(
            any(GraphQLRequest.class),
            any(ResultListener.class)
        );
    }

    /**
     * A listener to sync results. You can use it to block the calling thread, and await for
     * results to arrive, in a synchronous way, by invoking {@link SyncListener#awaitSuccessResponse()}.
     * @param <T> Type of model being sync'd
     */
    static final class SyncListener<T extends Model>
            implements ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> {

        private final LatchedSingleResponseListener<Iterable<ModelWithMetadata<T>>> listener;

        private SyncListener() {
            listener = new LatchedSingleResponseListener<>();
        }

        static <T extends Model> SyncListener<T> instance() {
            return new SyncListener<>();
        }

        @Override
        public void onResult(GraphQLResponse<Iterable<ModelWithMetadata<T>>> result) {
            listener.onResult(result);
        }

        @Override
        public void onError(Throwable error) {
            listener.onError(error);
        }

        Iterable<ModelWithMetadata<T>> awaitSuccessResponse() {
            return listener.awaitSuccessResponse();
        }
    }
}
