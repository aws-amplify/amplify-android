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

package com.amplifyframework.api.aws.auth;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.EndpointType;

import org.junit.Test;

import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the behavior of {@link ApiRequestDecoratorFactory} for different
 * scenarios involving custom Authorization Provider overrides.
 */
public final class ApiRequestDecoratorFactoryTest {
    private static final String X_API_KEY = "x-api-key";
    private static final String AUTHORIZATION = "authorization";

    /**
     * Test cases for when no custom provider is given
     * for {@link com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider}.
     * This is the recommended path, and a customer should
     * never have to provide a custom provider.
     * @throws ApiException From API configuration
     */
    @Test
    public void testApiKeyOverrideNotProvided() throws ApiException {
        final String apiKey = "API_KEY_1";
        ApiAuthProviders providers = ApiAuthProviders.noProviderOverrides();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            apiKey);
        Request request = new Request.Builder().url("https://localhost/").build();
        Request decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request);
        assertEquals(apiKey, decoratedRequest.header(X_API_KEY));
    }

    /**
     * Test cases for when no API key is provided by the config file..
     */
    @Test
    public void testApiKeyNotProvidedInConfiguration() {
        ApiAuthProviders providers = ApiAuthProviders.noProviderOverrides();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            null);
        Request request = new Request.Builder().url("https://localhost/").build();
        assertThrows(
            ApiException.ApiAuthException.class,
            () -> factory.forAuthType(AuthorizationType.API_KEY).decorate(request)
        );
    }

    /**
     * If API key is not mentioned in API configuration AND auth mode is
     * API_KEY BUT a valid custom API key provider is provided via
     * ApiAuthProvider, then intercept succeeds.
     * @throws ApiException.ApiAuthException From factory.forAuthType
     */
    @Test
    public void testApiKeyProvidedInterceptSucceeds() throws ApiException.ApiAuthException {
        ApiAuthProviders providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "CUSTOM_API_KEY")
                .build();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            null);
        Request request = new Request.Builder().url("https://localhost/").build();
        Request decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request);
        assertEquals("CUSTOM_API_KEY", decoratedRequest.header(X_API_KEY));
    }

    /**
     * Test cases for when a custom implementation of
     * {@link com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider}
     * is provided to the plugin to override the default method
     * of obtaining API key directly from configuration.
     * @throws ApiException From API configuration
     */
    @Test
    public void testApiKeyOverrideProvided() throws ApiException {
        ApiAuthProviders providers = ApiAuthProviders.builder()
                .apiKeyAuthProvider(() -> "CUSTOM_API_KEY")
                .build();

        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY");
        Request request = new Request.Builder().url("https://localhost/").build();
        Request decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request);
        assertEquals("CUSTOM_API_KEY", decoratedRequest.header(X_API_KEY));
    }

    /**
     * If a custom OIDC provider is not provided AND there is
     * an API that uses {@link AuthorizationType#OPENID_CONNECT}
     * as its auth mechanism, then the process will fail at runtime
     * while sending a request with that API.
     */
    @Test
    public void testOidcOverrideNotProvided() {
        ApiAuthProviders providers = ApiAuthProviders.noProviderOverrides();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.OPENID_CONNECT,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY");
        Request request = new Request.Builder().url("https://localhost/").build();
        assertThrows(
            ApiException.ApiAuthException.class,
            () -> factory.forAuthType(AuthorizationType.OPENID_CONNECT).decorate(request)
        );
    }

    /**
     * Test to confirm that passing any custom implementation of
     * {@link com.amplifyframework.api.aws.sigv4.OidcAuthProvider}
     * prevents crashes while intercepting requests.
     * @throws ApiException From API configuration
     */
    @Test
    public void testOidcOverrideProvided() throws ApiException {
        final String oidcJwtToken = "OIDC_JWT_TOKEN";

        ApiAuthProviders providers = ApiAuthProviders.builder()
                .oidcAuthProvider(() -> oidcJwtToken)
                .build();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.OPENID_CONNECT,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY");
        Request request = new Request.Builder().url("https://localhost/").build();
        Request decoratedRequest = factory.forAuthType(AuthorizationType.OPENID_CONNECT).decorate(request);
        assertEquals(oidcJwtToken, decoratedRequest.header(AUTHORIZATION));
    }

    /**
     * If a custom auth provider is not provided AND there is
     * an API that uses {@link AuthorizationType#AWS_LAMBDA}
     * as its auth mechanism, then the process will fail at runtime
     * while sending a request with that API.
     */
    @Test
    public void testCustomOverrideNotProvided() {
        ApiAuthProviders providers = ApiAuthProviders.noProviderOverrides();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_LAMBDA,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY");
        Request request = new Request.Builder().url("https://localhost/").build();
        assertThrows(
            ApiException.ApiAuthException.class,
            () -> factory.forAuthType(AuthorizationType.AWS_LAMBDA).decorate(request)
        );
    }

    /**
     * Test to confirm that passing any custom implementation of
     * {@link com.amplifyframework.api.aws.sigv4.CustomAuthProvider}
     * prevents crashes while intercepting requests.
     * @throws ApiException From API configuration
     */
    @Test
    public void testCustomOverrideProvided() throws ApiException {
        final String customToken = "CUSTOM_TOKEN";

        ApiAuthProviders providers = ApiAuthProviders.builder()
                .customAuthProvider(() -> customToken)
                .build();
        ApiRequestDecoratorFactory factory = new ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_LAMBDA,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY");
        Request request = new Request.Builder().url("https://localhost/").build();
        Request decoratedRequest = factory.forAuthType(AuthorizationType.AWS_LAMBDA).decorate(request);
        assertEquals(customToken, decoratedRequest.header(AUTHORIZATION));
    }

}
