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

package com.amplifyframework.api.aws;

import com.amplifyframework.api.ApiException;

import com.amazonaws.internal.StaticCredentialsProvider;
import org.junit.Test;

import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the behavior of {@link InterceptorFactory} for different
 * scenarios involving custom Authorization Provider overrides and
 * {@link ApiConfiguration} instances.
 */
public final class AppSyncSigV4SignerInterceptorFactoryTest {
    private static final String X_API_KEY = "x-api-key";
    private static final String AUTHORIZATION = "authorization";

    private static ApiAuthProviders providers;
    private static InterceptorFactory factory;

    /**
     * Test cases for when no custom provider is given
     * for {@link com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider}.
     * This is the recommended path, and a customer should
     * never have to provide a custom provider.
     */
    @Test
    public void testApiKeyOverrideNotProvided() {
        final String apiKey1 = "API_KEY_1";
        final String apiKey2 = "API_KEY_2";

        providers = ApiAuthProviders.builder()
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .oidcAuthProvider(() -> "OIDC_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        // Uses API key from one of the APIs
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .apiKey(apiKey1)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals(apiKey1, res.request().header(X_API_KEY));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }

        // Uses another API key from one of the APIs while reusing factory
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .apiKey(apiKey2)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals(apiKey2, res.request().header(X_API_KEY));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }
    }

    /**
     * Test cases for when no API key is provided inside
     * {@link ApiConfiguration} object.
     */
    @Test
    public void testApiKeyNotProvidedInConfiguration() {
        providers = ApiAuthProviders.builder()
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .oidcAuthProvider(() -> "OIDC_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        // If API key is not mentioned in API configuration AND
        // auth mode is API_KEY AND no custom API key provider
        // is provided via ApiAuthProvider, then intercept fails.
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            fail("Factory-created interceptor should successfully intercept.");
        } catch (Exception error) {
            assertTrue(error instanceof IllegalArgumentException);
        }

        providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "CUSTOM_API_KEY")
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .oidcAuthProvider(() -> "OIDC_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        // If API key is not mentioned in API configuration AND
        // auth mode is API_KEY BUT a valid custom API key provider
        // is provided via ApiAuthProvider, then intercept succeeds.
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals("CUSTOM_API_KEY", res.request().header(X_API_KEY));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }
    }

    /**
     * Test cases for when a custom implementation of
     * {@link com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider}
     * is provided to the plugin to override the default method
     * of obtaining API key directly from configuration.
     */
    @Test
    public void testApiKeyOverrideProvided() {
        providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "CUSTOM_API_KEY")
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .oidcAuthProvider(() -> "OIDC_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        // Even if API key is written in the ApiConfiguration, the interceptor
        // obtains its API key from custom provider and ignores the config
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .apiKey("API_KEY_INSIDE_CONFIG")
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals("CUSTOM_API_KEY", res.request().header(X_API_KEY));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }

        // Even if API key isn't written in the ApiConfiguration, the interceptor
        // obtains its API key from custom provider without crashing
        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.API_KEY)
                    .apiKey("ANOTHER_API_KEY_INSIDE_CONFIG")
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals("CUSTOM_API_KEY", res.request().header(X_API_KEY));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }
    }

    /**
     * If a custom OIDC provider is not provided AND there is
     * an API that uses {@link AuthorizationType#OPENID_CONNECT}
     * as its auth mechanism, then the process will fail at runtime
     * while sending a request with that API.
     */
    @Test
    public void testOidcOverrideNotProvided() {
        providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "API_KEY")
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.OPENID_CONNECT)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            fail("Factory-created interceptor should fail to intercept.");
        } catch (Exception error) {
            assertTrue(error.getCause() instanceof ApiException.AuthorizationTypeNotConfiguredException);
        }
    }

    /**
     * Test to confirm that passing any custom implementation of
     * {@link com.amplifyframework.api.aws.sigv4.OidcAuthProvider}
     * prevents crashes while intercepting requests.
     */
    @Test
    public void testOidcOverrideProvided() {
        providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "API_KEY")
                .awsCredentialsProvider(new StaticCredentialsProvider(null))
                .cognitoUserPoolsAuthProvider(() -> "COGNITO_USER_POOLS_JWT_TOKEN")
                .oidcAuthProvider(() -> "OIDC_JWT_TOKEN")
                .build();
        factory = new AppSyncSigV4SignerInterceptorFactory(null, providers);

        try {
            ApiConfiguration config = ApiConfiguration.builder()
                    .endpoint("")
                    .region("")
                    .authorizationType(AuthorizationType.OPENID_CONNECT)
                    .build();
            Response res = factory.create(config).intercept(new MockChain());
            assertEquals("OIDC_JWT_TOKEN", res.request().header(AUTHORIZATION));
        } catch (Exception error) {
            fail("Factory-created interceptor should successfully intercept.");
        }
    }
}
