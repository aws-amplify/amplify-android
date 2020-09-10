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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.events.ApiChannelEventName;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.graphql.model.ModelMutation;
import com.amplifyframework.api.graphql.model.ModelPagination;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.api.graphql.model.ModelSubscription;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.ownerauth.OwnerAuth;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.EmptyAction;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link AWSApiPlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AWSApiPluginTest {
    private MockWebServer webServer;
    private HttpUrl baseUrl;
    private AWSApiPlugin plugin;
    private CognitoUserPoolsAuthProvider authProvider;

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
        authProvider = mock(CognitoUserPoolsAuthProvider.class);
        // Returns a sample access token with the username value of "Facebook_100003287976754"
        when(authProvider.getLatestAuthToken()).thenReturn("eyJraWQiOiJnMmtYXC8rSXRmNFwvcmwyODhBSTNCMk9kNDVsdEU" +
                        "4ZUtIZmF0RkNRWEVDMmM9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI2YTRjZjMxMi01MWM5LTQyNjAtYjRhZC0wMDdj" +
                        "MDdkZDdmMzMiLCJjb2duaXRvOmdyb3VwcyI6WyJ1cy13ZXN0LTJfejVWM1ZQa1h5X0ZhY2Vib29rIiwiQWRtaW4iXSwi" +
                        "dG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiBwaG9uZSBvcGVu" +
                        "aWQgcHJvZmlsZSBlbWFpbCIsImF1dGhfdGltZSI6MTU5OTY4MTk2MywiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRw" +
                        "LnVzLXdlc3QtMi5hbWF6b25hd3MuY29tXC91cy13ZXN0LTJfejVWM1ZQa1h5IiwiZXhwIjoxNTk5NzU3OTE3LCJpYXQi" +
                        "OjE1OTk3NTQzMTcsInZlcnNpb24iOjIsImp0aSI6ImQyNzYxMDg3LTliNmYtNDYzMS1iYWJjLTY0ZWQzM2UyNGQzMiIs" +
                        "ImNsaWVudF9pZCI6IjNjZDdjcGJ1N2huYzlka2xoMmZsamg3am0xIiwidXNlcm5hbWUiOiJGYWNlYm9va18xMDAwMDMy" +
                        "ODc5NzY3NTQifQ.gtINNiuOhAPG5Z-4KgT7Hppw9wYyoVF8lvFhGdAWPi0c3sQvGpTLlBlgh8NqaELai84fTOTsQT4sH" +
                        "YwP31ik58qrIp7QQ8IOU91mXy2i3-ygsIWGEetvNaMd5ICXhTWUxg7gpKxsJQbrtH88DkO3NxAQSpGoUzlkKzJILekNn" +
                        "H5wC5drUocg_1yHTYfwsG23QVsm-cHvNsxkzRzjS3Gr18x5jTuhflr24yOGl_fdRas8-kA5q_vMuKclnNuxCztNHOBGk" +
                        "h-sfTaOh6C-FstV2GOuwtEknlCQqLdJUVSMQO2M4hTScGPXOr2Gz9xTX9QY0D9eNL7806LYObm5nmRd7g");

        JSONObject configuration = new JSONObject()
            .put("graphQlApi", new JSONObject()
                .put("endpointType", "GraphQL")
                .put("endpoint", baseUrl.url())
                .put("region", "us-east-1")
                .put("authorizationType", "API_KEY")
                .put("apiKey", "api-key")
            );

        this.plugin = new AWSApiPlugin(ApiAuthProviders.builder()
                .cognitoUserPoolsAuthProvider(authProvider)
                .build());
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
     * Test that request is serialized as expected, with owner variable.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void ownerArgumentIsAddedAndSerializedInRequest() throws JSONException {
        GraphQLRequest<OwnerAuth> request = ModelSubscription.onCreate(OwnerAuth.class);
        GraphQLOperation<OwnerAuth> operation = plugin.subscribe(request,
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                EmptyAction.create());

        JSONAssert.assertEquals(Resources.readAsString("request-owner-auth.json"),
                operation.getRequest().getContent(),
                true);
    }

    /**
     * Verify that owner argument is required for all subscriptions if ModelOperation.READ is specified.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForRestrictedRead() throws AmplifyException {
        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_UPDATE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_UPDATE));

        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_DELETE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_DELETE));

        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_CREATE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify owner argument is NOT required if the subscription type is not one of the restricted operations.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfOperationNotRestricted() throws AmplifyException {
        assertFalse(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_UPDATE));

        assertFalse(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_DELETE));
        assertFalse(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_DELETE));
        assertFalse(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_DELETE));

        assertFalse(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify owner argument is NOT added if authStrategy is not OWNER.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfNotOwnerStrategy() throws AmplifyException {
        assertFalse(isOwnerArgumentAdded(Group.class, SubscriptionType.ON_CREATE));
    }

    private boolean isOwnerArgumentAdded(Class<? extends Model> clazz, Operation operation)
            throws AmplifyException {
        AppSyncGraphQLRequest<Model> request = AppSyncGraphQLRequest.builder()
                .modelClass(clazz)
                .operation(operation)
                .requestOptions(new ApiGraphQLRequestOptions())
                .responseType(clazz)
                .build();

        GraphQLOperation<Model> graphQLOperation = plugin.subscribe(request,
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                NoOpConsumer.create(),
                EmptyAction.create());

        return "Facebook_100003287976754".equals(graphQLOperation.getRequest().getVariables().get("owner"));
    }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER) })
    private abstract class Owner implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.CREATE)})
    private abstract class OwnerCreate implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.READ)})
    private abstract class OwnerRead implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.UPDATE)})
    private abstract class OwnerUpdate implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.DELETE)})
    private abstract class OwnerDelete implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.GROUPS)})
    private abstract class Group implements Model { }
}
