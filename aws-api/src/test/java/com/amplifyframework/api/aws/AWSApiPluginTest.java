/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.auth.DummyCredentialsProvider;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.events.ApiChannelEventName;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.model.ModelMutation;
import com.amplifyframework.api.graphql.model.ModelPagination;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Latch;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.TypeMaker;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
     * @throws IOException On failure to start web server
     * @throws JSONException On failure to arrange configuration JSON
     * @throws AmplifyException On failure to create request
     */
    @Before
    public void setup() throws AmplifyException, IOException, JSONException {
        webServer = new MockWebServer();
        webServer.start(8080);
        baseUrl = webServer.url("/");

        JSONObject configuration = new JSONObject()
            .put("graphQlApi", new JSONObject()
                .put("endpointType", "GraphQL")
                .put("endpoint", baseUrl.url())
                .put("region", "us-east-1")
                .put("authorizationType", "API_KEY")
                .put("apiKey", "FAKE-API-KEY"));

        ApiAuthProviders apiAuthProviders = ApiAuthProviders
            .builder()
            .awsCredentialsProvider(DummyCredentialsProvider.INSTANCE)
            .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                @Override
                public String getLatestAuthToken() throws ApiException {
                    return "FAKE_TOKEN";
                }

                @Override
                public String getUsername() {
                    return "FAKE_USER";
                }
            })
            .build();

        this.plugin = AWSApiPlugin.builder()
            .configureClient("graphQlApi", builder -> {
                builder.addInterceptor(chain -> {
                    return chain.proceed(chain.request().newBuilder()
                        .addHeader("specialKey", "specialValue")
                        .build()
                    );
                });
                builder.connectTimeout(10, TimeUnit.SECONDS);
            })
            .apiAuthProviders(apiAuthProviders)
            .build();
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

        GraphQLResponse<PaginatedResult<BlogOwner>> actualResponse =
            Await.<GraphQLResponse<PaginatedResult<BlogOwner>>, ApiException>result(((onResult, onError) ->
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
    @Ignore("fix")
    public void graphQlMutationGetsResponse() throws JSONException, ApiException {
        HubAccumulator networkStatusObserver =
            HubAccumulator.create(HubChannel.API, ApiChannelEventName.API_ENDPOINT_STATUS_CHANGED, 1)
                .start();
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

        // Verify that the expected hub event fired.
        HubEvent<?> event = networkStatusObserver.awaitFirst();
        assertNotNull(event);
        assertTrue(event.getData() instanceof ApiEndpointStatusChangeEvent);
        ApiEndpointStatusChangeEvent eventData = (ApiEndpointStatusChangeEvent) event.getData();
        assertEquals(ApiEndpointStatusChangeEvent.ApiEndpointStatus.REACHABLE, eventData.getCurrentStatus());
    }

    /**
     * When the server returns a client error code in response to calling
     * {@link AWSApiPlugin#mutate(GraphQLRequest, Consumer, Consumer)}.
     * only the onFailure callback should be called.
     *
     * @throws InterruptedException if the thread performing the mutation is interrupted.
     */
    @Test
    public void graphQlMutationWithClientErrorResponseCodeShouldNotCallOnResponse() throws InterruptedException {
        final int CLIENT_ERROR_CODE = 400;
        webServer.enqueue(new MockResponse().setResponseCode(CLIENT_ERROR_CODE));

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<Throwable> unexpectedErrorContainer = new AtomicReference<>();
        final AtomicReference<GraphQLResponse<BlogOwner>> responseContainer = new AtomicReference<>();
        final AtomicReference<ApiException> failureContainer = new AtomicReference<>();

        final Consumer<GraphQLResponse<BlogOwner>> onResponse = (response) -> {
            responseContainer.set(response);
            latch.countDown();
        };
        final Consumer<ApiException> onFailure = (failure) -> {
            failureContainer.set(failure);
            latch.countDown();
        };

        final Thread thread = new Thread(() -> {
            try {
                // Try to perform a mutation.
                final BlogOwner tony = BlogOwner.builder()
                        .name(RandomString.string())
                        .build();
                plugin.mutate(ModelMutation.create(tony), onResponse, onFailure);
            } catch (Throwable unexpectedFailure) {
                unexpectedErrorContainer.set(unexpectedFailure);
                do {
                    latch.countDown();
                } while (latch.getCount() > 0);
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            Latch.await(latch);
            thread.join();
        } catch (RuntimeException runtimeException) {
            // We expect a RuntimeException here.
            //  That means awaiting the latch timed out and both callbacks were not called.
        }

        assertNull(
                "An unexpected error occurred while performing the mutation.",
                unexpectedErrorContainer.get()
        );
        assertTrue(
                "Latch count == 0; both onFailure and onResponse were called.",
                latch.getCount() > 0
        );
        assertTrue(
                "Expected client error response code to result in NonRetryableException.",
                failureContainer.get() instanceof ApiException.NonRetryableException
        );
        assertNull(
                "There was a non-null response but the response code indicates client error.",
                responseContainer.get()
        );
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

    /**
     * Validates that the plugin adds custom headers into the outgoing OkHttp request.
     * @throws ApiException Thrown from the query() call.
     * @throws InterruptedException Possible thrown from takeRequest()
     */
    @Test
    public void headerInterceptorsAreConfigured() throws ApiException, InterruptedException {
        // Arrange some response. This isn't the point of the test,
        // but it keeps the mock web server from freezing up.
        webServer.enqueue(new MockResponse()
            .setBody(Resources.readAsString("blog-owners-query-results.json")));

        // Fire off a request
        Await.<GraphQLResponse<PaginatedResult<BlogOwner>>, ApiException>result((onResult, onError) ->
            plugin.query(ModelQuery.list(BlogOwner.class), onResult, onError)
        );

        RecordedRequest recordedRequest = webServer.takeRequest(5, TimeUnit.MILLISECONDS);
        assertNotNull(recordedRequest);
        assertEquals("specialValue", recordedRequest.getHeader("specialKey"));
    }

    /**
     * If the auth mode is set for the individual request, ensure that the resulting request
     * to AppSync has the correct auth header.
     * @throws AmplifyException Not expected.
     * @throws InterruptedException Not expected.
     */
    @Test
    public void requestUsesCognitoForAuth() throws AmplifyException, InterruptedException {
        webServer.enqueue(new MockResponse()
                              .setBody(Resources.readAsString("blog-owners-query-results.json")));

        AppSyncGraphQLRequest<PaginatedResult<BlogOwner>> appSyncGraphQLRequest =
            createQueryRequestWithAuthMode(BlogOwner.class, AuthorizationType.AMAZON_COGNITO_USER_POOLS);

        GraphQLResponse<PaginatedResult<BlogOwner>> actualResponse =
            Await.<GraphQLResponse<PaginatedResult<BlogOwner>>, ApiException>result(
                (onResult, onError) -> plugin.query(appSyncGraphQLRequest, onResult, onError)
            );

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertNull(recordedRequest.getHeader("x-api-key"));
        assertNotNull(recordedRequest.getHeader("authorization"));
        assertEquals("FAKE_TOKEN", recordedRequest.getHeader("authorization"));
        assertEquals(
            Arrays.asList("Curly", "Moe", "Larry"),
            Observable.fromIterable(actualResponse.getData())
                      .map(BlogOwner::getName)
                      .toList()
                      .blockingGet()
        );
    }

    /**
     * Ensure the auth mode used for the request is AWS_IAM. We verify this by
     * @throws AmplifyException Not expected.
     * @throws InterruptedException Not expected.
     */
    @Test
    public void requestUsesIamForAuth() throws AmplifyException, InterruptedException {
        webServer.enqueue(new MockResponse()
                              .setBody(Resources.readAsString("blog-owners-query-results.json")));

        AppSyncGraphQLRequest<PaginatedResult<BlogOwner>> appSyncGraphQLRequest =
            createQueryRequestWithAuthMode(BlogOwner.class, AuthorizationType.AWS_IAM);

        GraphQLResponse<PaginatedResult<BlogOwner>> actualResponse =
            Await.<GraphQLResponse<PaginatedResult<BlogOwner>>, ApiException>result(
                (onResult, onError) -> plugin.query(appSyncGraphQLRequest, onResult, onError)
            );

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertNull(recordedRequest.getHeader("x-api-key"));
        assertNotNull(recordedRequest.getHeader("authorization"));
        assertTrue(recordedRequest.getHeader("authorization").startsWith("AWS4-HMAC-SHA256"));
        assertEquals(
            Arrays.asList("Curly", "Moe", "Larry"),
            Observable.fromIterable(actualResponse.getData())
                      .map(BlogOwner::getName)
                      .toList()
                      .blockingGet()
        );
    }

    private <R> AppSyncGraphQLRequest<PaginatedResult<R>>
                    createQueryRequestWithAuthMode(Type modelType,
                                                   AuthorizationType authMode) throws AmplifyException {
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, modelType);
        return AppSyncGraphQLRequest
            .builder()
            .authorizationType(authMode)
            .modelClass(BlogOwner.class)
            .responseType(responseType)
            .operation(QueryType.LIST)
            .requestOptions(new ApiGraphQLRequestOptions())
            .build();
    }
}
