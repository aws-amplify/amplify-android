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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.testutils.random.RandomString;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * Test that Subscription authorizer can correctly generate appropriate
 * headers for different types of authentication methods.
 */
@RunWith(RobolectricTestRunner.class)
public final class SubscriptionAuthorizerTest {
    private String authenticationSecret;
    private ApiAuthProviders apiAuthProviders;

    /**
     * Construct fake auth providers to override default behaviors for
     * subscription authorizer.
     */
    @Before
    public void setup() {
        authenticationSecret = RandomString.string();
        apiAuthProviders = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> authenticationSecret) // fake API key
                .awsCredentialsProvider(new FakeCredentialsProvider(
                        RandomString.string(),
                        RandomString.string()
                ))
                .cognitoUserPoolsAuthProvider(new FakeCognitoAuthProvider(
                        authenticationSecret, // fake token
                        RandomString.string()
                ))
                .oidcAuthProvider(() -> authenticationSecret) // fake token
                .build();
    }

    /**
     * Test that header generated for API key auth contains "x-api-key"
     * with the API key.
     * @throws ApiException if failure to construct headers
     * @throws JSONException if desired header is not present
     */
    @Test
    public void testHeaderForApiKey() throws ApiException, JSONException {
        ApiConfiguration config = ApiConfiguration.builder()
                .endpoint(RandomString.string())
                .region(RandomString.string())
                .authorizationType(AuthorizationType.API_KEY)
                .build();
        SubscriptionAuthorizer authorizer = new SubscriptionAuthorizer(config, apiAuthProviders);
        JSONObject header = authorizer.createHeadersForConnection(AuthorizationType.API_KEY);
        assertEquals(authenticationSecret, header.getString("x-api-key"));
    }

    /**
     * Test that header generated for Cognito User Pools auth contains
     * "Authorization" header with the token vended by Cognito.
     * @throws ApiException if failure to construct headers
     * @throws JSONException if desired header is not present
     */
    @Test
    public void testHeaderForCognitoUserPools() throws ApiException, JSONException {
        ApiConfiguration config = ApiConfiguration.builder()
                .endpoint(RandomString.string())
                .region(RandomString.string())
                .authorizationType(AuthorizationType.AMAZON_COGNITO_USER_POOLS)
                .build();
        SubscriptionAuthorizer authorizer = new SubscriptionAuthorizer(config, apiAuthProviders);
        JSONObject header = authorizer.createHeadersForConnection(AuthorizationType.AMAZON_COGNITO_USER_POOLS);
        assertEquals(authenticationSecret, header.getString("Authorization"));
    }

    /**
     * Test that header generated for OpenID Connect auth contains
     * "Authorization" header with the token vended by third-party.
     * @throws ApiException if failure to construct headers
     * @throws JSONException if desired header is not present
     */
    @Test
    public void testHeaderForOidc() throws ApiException, JSONException {
        ApiConfiguration config = ApiConfiguration.builder()
                .endpoint(RandomString.string())
                .region(RandomString.string())
                .authorizationType(AuthorizationType.OPENID_CONNECT)
                .build();
        SubscriptionAuthorizer authorizer = new SubscriptionAuthorizer(config, apiAuthProviders);
        JSONObject header = authorizer.createHeadersForConnection(AuthorizationType.OPENID_CONNECT);
        assertEquals(authenticationSecret, header.getString("Authorization"));
    }

    private static final class FakeCredentialsProvider implements AWSCredentialsProvider {
        private final String accessKey;
        private final String secretKey;

        private FakeCredentialsProvider(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        @Override
        public AWSCredentials getCredentials() {
            return new BasicAWSCredentials(accessKey, secretKey);
        }

        @Override
        public void refresh() {
            // No-op
        }
    }

    private static final class FakeCognitoAuthProvider implements CognitoUserPoolsAuthProvider {
        private final String token;
        private final String username;

        private FakeCognitoAuthProvider(String token, String username) {
            this.token = token;
            this.username = username;
        }

        @Override
        public String getLatestAuthToken() throws ApiException {
            return token;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
