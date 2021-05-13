/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test functionality related to retrieving configure API auth providers.
 */
public class ApiAuthProvidersTest {
    private static final AWSCognitoAuthPlugin AUTH_PLUGIN = new AWSCognitoAuthPlugin();
    private static final ApiConfiguration BASIC_API_CONFIG = ApiConfiguration.builder()
                                                                             .endpoint("https://test.endpoint.com")
                                                                             .region("us-east-1")
                                                                             .authorizationType(
                                                                                AuthorizationType.API_KEY
                                                                             )
                                                                             .build();
    private static final CognitoUserPoolsAuthProvider DUMMY_COGNITO_PROVIDER =
        new CognitoUserPoolsAuthProvider() {
            @Override
            public String getLatestAuthToken() throws ApiException {
                return null;
            }

            @Override
            public String getUsername() {
                return null;
            }
        };

    /**
     * Test setup.
     * @throws AmplifyException not expected.
     */
    @Before
    public void setup() throws AmplifyException {
        Amplify.removePlugin(AUTH_PLUGIN);
    }

    /**
     * Verifies that API_KEY is returned as one of the available auth types if:
     * - An API key auth provider is explicitly set on the ApiAuthProviders instance OR
     * - The provided API configuration has an API key specified.
     */
    @Test
    public void testApiKey() {
        // Create an API config with an API key
        ApiConfiguration configWithKey = ApiConfiguration.builder()
                                                         .endpoint("https://test.endpoint.com")
                                                         .region("us-east-1")
                                                         .authorizationType(AuthorizationType.API_KEY)
                                                         .apiKey("dummy")
                                                         .build();

        // Create an ApiAuthProviders instance with an API key provider set.
        ApiAuthProviders withProvider = ApiAuthProviders.builder().apiKeyAuthProvider(() -> null).build();

        // Create an empty ApiAuthProviders instance.
        ApiAuthProviders withoutProvider = ApiAuthProviders.noProviderOverrides();

        Set<AuthorizationType> expectedAuthTypesApiKeyOnly = Collections.singleton(AuthorizationType.API_KEY);

        // No API Key provider and no API key in the config should result in 0 auth types being returned.
        Set<AuthorizationType> actualAuthTypes = withoutProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(0, actualAuthTypes.size());

        // No API Key provider but there's an API key in the config should result in 1 auth type (API_KEY)
        actualAuthTypes = withoutProvider.getAvailableAuthorizationTypes(configWithKey);
        assertEquals(expectedAuthTypesApiKeyOnly, actualAuthTypes);

        // API Key provider set (with or without a API Key in the API config) shoult result in 1 auth type (API_KEY)
        actualAuthTypes = withProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(expectedAuthTypesApiKeyOnly, actualAuthTypes);
        actualAuthTypes = withProvider.getAvailableAuthorizationTypes(configWithKey);
        assertEquals(expectedAuthTypesApiKeyOnly, actualAuthTypes);
    }

    /**&
     * Verifies that API_KEY is returned as one of the available auth types if:
     * - The cognitoUserPoolsAuthProvider field is explicitly set on the instance OR
     * - The Amplify Auth plugin is present.
     * @throws AmplifyException not expected.
     */
    @Test
    public void testUserPoolConfig() throws AmplifyException {
        // Create one ApiAuthProviders object with the user pool provided explicitly defined.
        ApiAuthProviders withProvider = ApiAuthProviders.builder()
                                                        .cognitoUserPoolsAuthProvider(DUMMY_COGNITO_PROVIDER)
                                                        .build();
        // Create an empty ApiAuthProviders instance.
        ApiAuthProviders withoutProvider = ApiAuthProviders.noProviderOverrides();

        // withProvider should return 1 item (AMAZON_COGNITO_USER_POOLS) because cognitoUserPoolsAuthProvider
        // was set.
        Set<AuthorizationType> results = withProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(1, results.size());
        Assert.assertTrue(results.contains(AuthorizationType.AMAZON_COGNITO_USER_POOLS));

        // withoutProvider should return 0. No cognitoUserPoolsAuthProvider was not set.
        results = withoutProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(0, results.size());

        // Add the auth plugin to Amplify.
        Amplify.addPlugin(AUTH_PLUGIN);

        // withoutProvider should return 1 because we added an auth plugin.
        results = withoutProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(1, results.size());
        Assert.assertTrue(results.contains(AuthorizationType.AMAZON_COGNITO_USER_POOLS));
    }

    /**
     * Verify that AWS_IAM is returned if an AWS credentials provider is set.
     */
    @Test
    public void testIAM() {
        // Create one ApiAuthProviders object with an AWS credentials provider.
        ApiAuthProviders withProvider = ApiAuthProviders.builder()
                                                        .awsCredentialsProvider(new AWSCredentialsProvider() {
                                                            @Override
                                                            public AWSCredentials getCredentials() {
                                                                return null;
                                                            }

                                                            @Override
                                                            public void refresh() {

                                                            }
                                                        })
                                                        .build();
        // Create an empty ApiAuthProviders instance.
        ApiAuthProviders withoutProvider = ApiAuthProviders.noProviderOverrides();

        // withoutProvider should return 0 auth types.
        Set<AuthorizationType> results = withoutProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(0, results.size());

        // withProvider should return 1 auth type because it was explicitly set via awsCredentialsProvider.
        results = withProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(1, results.size());
        Assert.assertTrue(results.contains(AuthorizationType.AWS_IAM));
    }

    /**
     * Verify that OPENID_CONNECT is return is an OIDC provider is set.
     */
    @Test
    public void testOIDC() {
        // Create one ApiAuthProviders object with an OIDC provider
        ApiAuthProviders withProvider = ApiAuthProviders.builder()
                                                        .oidcAuthProvider(() -> null)
                                                        .build();
        // Create an empty ApiAuthProviders instance.
        ApiAuthProviders withoutProvider = ApiAuthProviders.noProviderOverrides();

        // withoutProvider should return 0 auth types.
        Set<AuthorizationType> results = withoutProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(0, results.size());

        // withProvider should return 1 auth type because it was explicitly set via oidcAuthProvider.
        results = withProvider.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(1, results.size());
        Assert.assertTrue(results.contains(AuthorizationType.OPENID_CONNECT));
    }

    /**
     * Test that returns multiple auth types if multiple providers are configured.
     */
    @Test
    public void testMultipleProviders() {
        ApiAuthProviders withMultipleProviders = ApiAuthProviders.builder()
                                                                 .apiKeyAuthProvider(() -> null)
                                                                 .cognitoUserPoolsAuthProvider(DUMMY_COGNITO_PROVIDER)
                                                                 .build();
        Set<AuthorizationType> results = withMultipleProviders.getAvailableAuthorizationTypes(BASIC_API_CONFIG);
        assertEquals(2, results.size());
        assertTrue(results.contains(AuthorizationType.AMAZON_COGNITO_USER_POOLS));
        assertTrue(results.contains(AuthorizationType.API_KEY));
    }
}
