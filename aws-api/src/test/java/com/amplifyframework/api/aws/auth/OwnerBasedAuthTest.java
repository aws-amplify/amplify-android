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

package com.amplifyframework.api.aws.auth;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.ApiGraphQLRequestOptions;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.graphql.model.ModelSubscription;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.testmodels.ownerauth.OwnerAuth;
import com.amplifyframework.testutils.EmptyAction;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests owner-based auth for Cognito User Pools and OIDC authorized APIs.
 */
@RunWith(RobolectricTestRunner.class)
public final class OwnerBasedAuthTest {
    private static final String GRAPHQL_API_WITH_API_KEY = "graphQlApi_apiKey";
    private static final String GRAPHQL_API_WITH_COGNITO = "graphQlApi_cognito";
    private static final String GRAPHQL_API_WITH_OIDC = "graphQlApi_oidc";

    private MockWebServer webServer;
    private HttpUrl baseUrl;
    private AWSApiPlugin plugin;
    private CognitoUserPoolsAuthProvider cognitoProvider;
    private OidcAuthProvider oidcProvider;
    private String apiName;

    /**
     * Sets up the test.
     * @throws ApiException On failure to configure plugin
     * @throws IOException On failure to start web server
     */
    @Before
    public void setup() throws ApiException, IOException {
        webServer = new MockWebServer();
        webServer.start(8080);
        baseUrl = webServer.url("/");
        cognitoProvider = new FakeCognitoAuthProvider();
        oidcProvider = new FakeOidcAuthProvider();
        configurePlugin();
    }

    /**
     * Stop the {@link MockWebServer} that was started in {@link #setup()}.
     * @throws IOException On failure to shutdown the MockWebServer
     */
    @After
    public void cleanup() throws IOException {
        webServer.shutdown();
    }

    private void configurePlugin() throws ApiException {
        ApiAuthProviders providers = ApiAuthProviders.builder()
                .cognitoUserPoolsAuthProvider(cognitoProvider)
                .oidcAuthProvider(oidcProvider)
                .build();
        JSONObject configuration = new JSONObject();
        try {
            configuration = configuration
                .put(GRAPHQL_API_WITH_API_KEY, new JSONObject()
                        .put("endpointType", "GraphQL")
                        .put("endpoint", baseUrl.url())
                        .put("region", "us-east-1")
                        .put("authorizationType", "API_KEY")
                        .put("apiKey", "FAKE-API-KEY"))
                .put(GRAPHQL_API_WITH_COGNITO, new JSONObject()
                        .put("endpointType", "GraphQL")
                        .put("endpoint", baseUrl.url())
                        .put("region", "us-east-1")
                        .put("authorizationType", "AMAZON_COGNITO_USER_POOLS"))
                .put(GRAPHQL_API_WITH_OIDC, new JSONObject()
                        .put("endpointType", "GraphQL")
                        .put("endpoint", baseUrl.url())
                        .put("region", "us-east-1")
                        .put("authorizationType", "OPENID_CONNECT"));
        } catch (JSONException exception) {
            // This shouldn't happen...
        }

        plugin = new AWSApiPlugin(providers);
        plugin.configure(configuration, ApplicationProvider.getApplicationContext());
    }

    /**
     * Test that owner argument fails to be appended to subscription request if
     * the authorization mode is not OIDC compliant.
     */
    @Test
    public void ownerArgumentNotAddedWithApiKey() {
        // Set API to use API key auth mode
        apiName = GRAPHQL_API_WITH_API_KEY;

        // Attempting to subscribe to a model with owner-based auth with API key auth mode.
        GraphQLRequest<OwnerAuth> request = ModelSubscription.onCreate(OwnerAuth.class);
        GraphQLOperation<OwnerAuth> operation = subscribe(request);

        // Subscription should fail at pre-processing
        assertNull(operation);
    }

    /**
     * Test that request is serialized as expected, with owner variable.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void ownerArgumentIsAddedAndSerializedInRequest() throws JSONException {
        // Set API to use Cognito User Pools auth mode
        apiName = GRAPHQL_API_WITH_COGNITO;

        GraphQLRequest<OwnerAuth> request = ModelSubscription.onCreate(OwnerAuth.class);
        GraphQLOperation<OwnerAuth> operation = subscribe(request);

        assertNotNull(operation);
        JSONAssert.assertEquals(Resources.readAsString("request-owner-auth.json"),
                operation.getRequest().getContent(),
                true);
    }

    /**
     * Verify that owner argument is required for all subscriptions if ModelOperation.READ is specified
     * while using Cognito User Pools auth mode.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForRestrictedReadWithUserPools() throws AmplifyException {
        // Set API to use Cognito User Pools auth mode
        apiName = GRAPHQL_API_WITH_COGNITO;

        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_UPDATE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_UPDATE));

        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_DELETE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_DELETE));

        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_CREATE));
        assertTrue(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify that owner argument is required for all subscriptions if ModelOperation.READ is specified
     * while using OpenID Connect auth mode.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForRestrictedReadWithOidc() throws AmplifyException {
        // Set API to use OpenID Connect auth mode
        apiName = GRAPHQL_API_WITH_OIDC;

        assertTrue(isOwnerArgumentAdded(OwnerOidc.class, SubscriptionType.ON_UPDATE));
        assertTrue(isOwnerArgumentAdded(OwnerOidc.class, SubscriptionType.ON_DELETE));
        assertTrue(isOwnerArgumentAdded(OwnerOidc.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify owner argument is NOT required if the subscription type is not one of the restricted operations.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfOperationNotRestrictedWithUserPools() throws AmplifyException {
        // Set API to use Cognito User Pools auth mode
        apiName = GRAPHQL_API_WITH_COGNITO;

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
        // Set API to use Cognito User Pools auth mode
        apiName = GRAPHQL_API_WITH_COGNITO;

        assertFalse(isOwnerArgumentAdded(Group.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(Group.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(Group.class, SubscriptionType.ON_DELETE));

        assertFalse(isOwnerArgumentAdded(Public.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(Public.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(Public.class, SubscriptionType.ON_DELETE));
    }

    private <M extends Model> boolean isOwnerArgumentAdded(Class<M> clazz, Operation operation)
            throws AmplifyException {
        GraphQLRequest<M> request = createRequest(clazz, operation);
        GraphQLOperation<M> graphQLOperation = subscribe(request);

        assertNotNull(graphQLOperation);
        final String owner = (String) graphQLOperation.getRequest()
            .getVariables()
            .get("owner");
        switch (apiName) {
            case GRAPHQL_API_WITH_COGNITO:
                return FakeCognitoAuthProvider.USERNAME.equals(owner);
            case GRAPHQL_API_WITH_OIDC:
                return FakeOidcAuthProvider.SUB.equals(owner);
            case GRAPHQL_API_WITH_API_KEY:
                return false;
            default:
                throw new RuntimeException("Invalid API is being used for this test.");
        }
    }

    // Simple subscription request with given model class and operation
    private <M extends Model> GraphQLRequest<M> createRequest(Class<M> clazz, Operation operation)
            throws AmplifyException {
        return AppSyncGraphQLRequest.builder()
            .modelClass(clazz)
            .operation(operation)
            .requestOptions(new ApiGraphQLRequestOptions())
            .responseType(clazz)
            .build();
    }

    // Simple subscription with blank callbacks
    private <M extends Model> GraphQLOperation<M> subscribe(GraphQLRequest<M> request) {
        return plugin.subscribe(
            apiName,
            request,
            NoOpConsumer.create(),
            NoOpConsumer.create(),
            NoOpConsumer.create(),
            EmptyAction.create()
        );
    }

    private static final class FakeCognitoAuthProvider implements CognitoUserPoolsAuthProvider {
        private static final String USERNAME = "Facebook_100003287976754";
        private static final String ID_TOKEN = "eyJraWQiOiJnMmtYXC8rSXRmNFwvcmwyODhBSTNCMk9kNDVsdEU4" +
                "ZUtIZmF0RkNRWEVDMmM9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI2YTRjZjMxMi01MWM5LTQyNjAtYjRh" +
                "ZC0wMDdjMDdkZDdmMzMiLCJjb2duaXRvOmdyb3VwcyI6WyJ1cy13ZXN0LTJfejVWM1ZQa1h5X0ZhY2Vib29" +
                "rIiwiQWRtaW4iXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udX" +
                "Nlci5hZG1pbiBwaG9uZSBvcGVuaWQgcHJvZmlsZSBlbWFpbCIsImF1dGhfdGltZSI6MTU5OTY4MTk2Mywia" +
                "XNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMi5hbWF6b25hd3MuY29tXC91cy13ZXN0LTJf" +
                "ejVWM1ZQa1h5IiwiZXhwIjoxNTk5NzU3OTE3LCJpYXQiOjE1OTk3NTQzMTcsInZlcnNpb24iOjIsImp0aSI" +
                "6ImQyNzYxMDg3LTliNmYtNDYzMS1iYWJjLTY0ZWQzM2UyNGQzMiIsImNsaWVudF9pZCI6IjNjZDdjcGJ1N2" +
                "huYzlka2xoMmZsamg3am0xIiwidXNlcm5hbWUiOiJGYWNlYm9va18xMDAwMDMyODc5NzY3NTQifQ.FAKE-S" +
                "IGNATURE";

        @Override
        public String getLatestAuthToken() {
            return ID_TOKEN;
        }

        @Override
        public String getUsername() {
            return USERNAME;
        }
    }

    private static final class FakeOidcAuthProvider implements OidcAuthProvider {
        private static final String SUB = "google-oauth2|112385265530942831934";
        private static final String ID_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IkNxUDlx" +
                "a1RQa2s5ZHVRN042SWFXVCJ9.eyJodHRwczovL215YXBwLmNvbS9jbGFpbXMvZ3JvdXBzIjpbImFkbWlucy" +
                "JdLCJpc3MiOiJodHRwczovL3JhcGhraW0udXMuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8M" +
                "TEyMzg1MjY1NTMwOTQyODMxOTM0IiwiYXVkIjoiNmpSUVJueFd6blg1N3Z5TTQwSmxHZXlGdGcwdnZaMkwi" +
                "LCJpYXQiOjE2MDMzODU1MDAsImV4cCI6MTYwMzQyMTUwMCwibm9uY2UiOiJub25jZSJ9.FAKE-SIGNATURE";

        @Override
        public String getLatestAuthToken() {
            return ID_TOKEN;
        }
    }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER) })
    private abstract static class Owner implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.CREATE)})
    private abstract static class OwnerCreate implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.READ)})
    private abstract static class OwnerRead implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.UPDATE)})
    private abstract static class OwnerUpdate implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.DELETE)})
    private abstract static class OwnerDelete implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, identityClaim = "sub") })
    private abstract static class OwnerOidc implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.PUBLIC) })
    private abstract static class Public implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.GROUPS)})
    private abstract static class Group implements Model {}
}
