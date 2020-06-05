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

package com.amplifyframework.api.aws;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.model.ModelMutation;
import com.amplifyframework.api.graphql.model.ModelPagination;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AWSApiPlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AWSApiPluginTest {
    private MockWebServer webServer;
    private HttpUrl baseUrl;
    private AWSApiPlugin plugin;

    /**
     * Sets up the test.
     * @throws ApiException On failure to configure plugin
     * @throws IOException On failure to start web server
     * @throws JSONException On failure to arrange configuration JSON
     */
    @Before
    public void setup() throws ApiException, IOException, JSONException {
        webServer = new MockWebServer();
        webServer.start(8080);
        baseUrl = webServer.url("/");

        JSONObject configuration = new JSONObject()
            .put("graphQlApi", new JSONObject()
                .put("endpointType", "GraphQL")
                .put("endpoint", baseUrl.url())
                .put("region", "us-east-1")
                .put("authorizationType", "API_KEY")
                .put("apiKey", "api-key")
            );
        this.plugin = new AWSApiPlugin();
        this.plugin.configure(configuration, ApplicationProvider.getApplicationContext());
    }

    /**
     * Stop the {@link MockWebServer} that was started in {@link #setup()}.
     * @throws IOException On failure to shutdown the MockWebServer
     */
    @After
    public void cleanup() throws IOException {
        webServer.shutdown();
    }

    /**
     * The value of {@link AWSApiPlugin#getPluginKey()} should be stable.
     * Think twice before updating this value. It is expected by the Amplify CLI / code-gen.
     */
    @Test
    public void pluginKeyHasStableValue() {
        assertEquals("awsAPIPlugin", plugin.getPluginKey());
    }

    /**
     * The object returned by {@link AWSApiPlugin#getEscapeHatch()} should be useful.
     * To validate this, obtain the escape hatch (which contains a collection of {@link OkHttpClient}),
     * and use them to make some simple network calls.
     * @throws IOException On failure interacting with OkHttpClient.
     * @throws JSONException On failure to arrange fake response JSON
     */
    @Test
    public void clientReturnedFromEscapeHatchIsUseful() throws IOException, JSONException {
        // Get the actual OkHttpClient through the escape hatch.
        Map<String, OkHttpClient> escapeHatch = plugin.getEscapeHatch();
        OkHttpClient graphQlApiClient = escapeHatch.get("graphQlApi");
        assertNotNull(graphQlApiClient);

        // Now, arrange some fake response from the endpoint of that client
        String expectedBody = new JSONObject()
            .put("key", "value")
            .toString();
        webServer.enqueue(new MockResponse().setBody(expectedBody));

        // Make a request using the client from the escape hatch
        Request request = new Request.Builder().url(baseUrl).build();
        Response actualResponse = graphQlApiClient.newCall(request).execute();

        // The escape hatch is only valuable if the client is actually usable.
        // Did the client return the response we expected to see, from the endpoint?
        ResponseBody actualResponseBody = actualResponse.body();
        assertNotNull(actualResponseBody);
        assertEquals(expectedBody, actualResponseBody.string());
    }

    /**
     * It should be possible to perform a GraphQL query. When the server returns a
     * valid response, content should be returned via the query(...) methods' value consumer.
     * @throws ApiException If call to query(...) itself emits such an exception
     */
    @Test
    public void graphQlQueryRendersValidResponse() throws ApiException {
        webServer.enqueue(new MockResponse()
            .setBody(Resources.readAsString("blog-owners-query-results.json")));

        GraphQLResponse<Iterable<BlogOwner>> actualResponse =
            Await.<GraphQLResponse<Iterable<BlogOwner>>, ApiException>result(((onResult, onError) ->
                plugin.query(ModelQuery.list(BlogOwner.class), onResult, onError)
            ));

        assertEquals(
            Arrays.asList("Curly", "Moe", "Larry"),
            Observable.fromIterable(actualResponse.getData())
                .map(BlogOwner::getName)
                .toList()
                .blockingGet()
        );
    }

    /**
     * Same as {@link #graphQlQueryRendersValidResponse()}, except with pagination.
     * Expect a PaginatedResult&lt;BlogOwner&gt;
     * instead of an Iterable&lt;BlogOwner&gt;, and verify that getRequestForNextResult is not null.
     * @throws ApiException If call to query(...) itself emits such an exception
     */
    @Test
    public void graphQlPaginatedQueryRendersExpectedResponse() throws ApiException {
        webServer.enqueue(new MockResponse()
                .setBody(Resources.readAsString("blog-owners-query-results.json")));

        GraphQLResponse<PaginatedResult<BlogOwner>> actualResponse =
                Await.<GraphQLResponse<PaginatedResult<BlogOwner>>, ApiException>result(((onResult, onError) ->
                        plugin.query(ModelQuery.list(BlogOwner.class, ModelPagination.firstPage()), onResult, onError)
                ));

        assertEquals(
                Arrays.asList("Curly", "Moe", "Larry"),
                Observable.fromIterable(actualResponse.getData().getItems())
                        .map(BlogOwner::getName)
                        .toList()
                        .blockingGet()
        );
        assertTrue(actualResponse.getData().hasNextResult());
        assertNotNull(actualResponse.getData().getRequestForNextResult());
    }

    /**
     * It should be possible to perform a successful call to
     * {@link AWSApiPlugin#mutate(GraphQLRequest, Consumer, Consumer)}.
     * When the server returns a valid response, then the mutate methods should
     * emit content via their value consumer.
     * @throws ApiException If call to mutate(...) itself emits such an exception
     * @throws JSONException On failure to arrange response JSON
     */
    @Test
    public void graphQlMutationGetsResponse() throws JSONException, ApiException {
        // Arrange a response from the "server"
        String expectedName = RandomString.string();
        webServer.enqueue(new MockResponse().setBody(new JSONObject()
            .put("data", new JSONObject()
                .put("createBlogOwner", new JSONObject()
                    .put("name", expectedName)
                )
            )
            .toString()
        ));

        // Try to perform a mutation.
        BlogOwner tony = BlogOwner.builder()
            .name(expectedName)
            .build();
        GraphQLResponse<BlogOwner> actualResponse =
            Await.<GraphQLResponse<BlogOwner>, ApiException>result(((onResult, onError) ->
                plugin.mutate(ModelMutation.create(tony), onResult, onError)
            ));

        // Assert that the expected response was received
        assertEquals(expectedName, actualResponse.getData().getName());
    }

    /**
     * Given that only one API was configured in {@link #setup()},
     * the {@link AWSApiPlugin#getSelectedApiName(EndpointType)} should be able to identify
     * this same API according to just its {@link EndpointType}.
     * @throws ApiException If the {@link AWSApiPlugin#getSelectedApiName(EndpointType)} itself fails
     */
    @Test
    public void singleConfiguredApiIsSelected() throws ApiException {
        String selectedApi = plugin.getSelectedApiName(EndpointType.GRAPHQL);
        assertEquals("graphQlApi", selectedApi);
    }
}
