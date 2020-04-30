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
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
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

/**
 * Tests the {@link AWSApiPlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AWSApiPluginTest {
    private MockWebServer webServer;
    private HttpUrl baseUrl;
    private AWSApiPlugin plugin;

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

    @After
    public void cleanup() throws IOException {
        webServer.shutdown();
    }

    @Test
    public void pluginKeyHasStableValue() {
        assertEquals("awsAPIPlugin", plugin.getPluginKey());
    }

    @Test
    public void clientReturnedFromEscapeHatchIsUseful() throws IOException, JSONException {
        // Get the actual OkHttpClient through the escape hatch.
        Map<String, OkHttpClient> escapeHatch = plugin.getEscapeHatch();
        OkHttpClient graphQlApiClient = escapeHatch.get("graphQlApi");
        assertNotNull(graphQlApiClient);

        // Now, arrange some fake response from the endpoint of that client
        String expectedBody = new JSONObject()
            .put("", "")
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

    @Test
    public void graphQlQueryRendersValidResponse() throws ApiException {
        webServer.enqueue(new MockResponse()
            .setBody(Resources.readAsString("blog-owners-query-results.json")));

        GraphQLResponse<Iterable<BlogOwner>> actualResponse =
            Await.<GraphQLResponse<Iterable<BlogOwner>>, ApiException>result(((onResult, onError) ->
                plugin.query(BlogOwner.class, onResult, onError)
            ));

        assertEquals(
            Arrays.asList("Curly", "Moe", "Larry"),
            Observable.fromIterable(actualResponse.getData())
                .map(BlogOwner::getName)
                .toList()
                .blockingGet()
        );
    }

    @Test
    public void graphQlMutationGetsResponse() throws ApiException, JSONException {
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
                plugin.mutate(tony, MutationType.CREATE, onResult, onError)
            ));

        // Assert that the expected response was received
        assertEquals(expectedName, actualResponse.getData().getName());
    }

    @Test
    public void singleConfiguredApiIsSelected() throws ApiException {
        String selectedApi = plugin.getSelectedApiName(EndpointType.GRAPHQL);
        assertEquals("graphQlApi", selectedApi);
    }
}

