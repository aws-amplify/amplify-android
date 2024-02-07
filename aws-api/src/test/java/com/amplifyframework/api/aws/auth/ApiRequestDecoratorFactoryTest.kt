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
package com.amplifyframework.api.aws.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.ApiException.ApiAuthException
import com.amplifyframework.api.aws.ApiAuthProviders
import com.amplifyframework.api.aws.AuthorizationType
import com.amplifyframework.api.aws.EndpointType
import com.amplifyframework.api.aws.sigv4.FunctionAuthProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the behavior of [ApiRequestDecoratorFactory] for different
 * scenarios involving custom Authorization Provider overrides.
 */
class ApiRequestDecoratorFactoryTest {
    /**
     * Test cases for when no custom provider is given
     * for [com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider].
     * This is the recommended path, and a customer should
     * never have to provide a custom provider.
     * @throws ApiException From API configuration
     */
    @Test
    @Throws(ApiException::class)
    fun testApiKeyOverrideNotProvided() {
        val apiKey = "API_KEY_1"
        val providers = ApiAuthProviders.noProviderOverrides()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            apiKey
        )
        val request: Request = Builder().url("https://localhost/").build()
        val decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request)
        Assert.assertEquals(apiKey, decoratedRequest.header(X_API_KEY))
    }

    /**
     * Test cases for when no API key is provided by the config file..
     */
    @Test
    fun testApiKeyNotProvidedInConfiguration() {
        val providers = ApiAuthProviders.noProviderOverrides()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            null
        )
        val request: Request = Builder().url("https://localhost/").build()
        Assert.assertThrows(
            ApiAuthException::class.java
        ) { factory.forAuthType(AuthorizationType.API_KEY).decorate(request) }
    }

    /**
     * If API key is not mentioned in API configuration AND auth mode is
     * API_KEY BUT a valid custom API key provider is provided via
     * ApiAuthProvider, then intercept succeeds.
     * @throws ApiException.ApiAuthException From factory.forAuthType
     */
    @Test
    @Throws(ApiAuthException::class)
    fun testApiKeyProvidedInterceptSucceeds() {
        val providers = ApiAuthProviders.builder()
            .apiKeyAuthProvider { "CUSTOM_API_KEY" }
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            null
        )
        val request: Request = Builder().url("https://localhost/").build()
        val decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request)
        Assert.assertEquals("CUSTOM_API_KEY", decoratedRequest.header(X_API_KEY))
    }

    /**
     * Test cases for when a custom implementation of
     * [com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider]
     * is provided to the plugin to override the default method
     * of obtaining API key directly from configuration.
     * @throws ApiException From API configuration
     */
    @Test
    @Throws(ApiException::class)
    fun testApiKeyOverrideProvided() {
        val providers = ApiAuthProviders.builder()
            .apiKeyAuthProvider { "CUSTOM_API_KEY" }
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.API_KEY,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/").build()
        val decoratedRequest = factory.forAuthType(AuthorizationType.API_KEY).decorate(request)
        Assert.assertEquals("CUSTOM_API_KEY", decoratedRequest.header(X_API_KEY))
    }

    /**
     * If a custom OIDC provider is not provided AND there is
     * an API that uses [AuthorizationType.OPENID_CONNECT]
     * as its auth mechanism, then the process will fail at runtime
     * while sending a request with that API.
     */
    @Test
    fun testOidcOverrideNotProvided() {
        val providers = ApiAuthProviders.noProviderOverrides()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.OPENID_CONNECT,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/").build()
        Assert.assertThrows(
            ApiAuthException::class.java
        ) { factory.forAuthType(AuthorizationType.OPENID_CONNECT).decorate(request) }
    }

    /**
     * Test to confirm that passing any custom implementation of
     * [com.amplifyframework.api.aws.sigv4.OidcAuthProvider]
     * prevents crashes while intercepting requests.
     * @throws ApiException From API configuration
     */
    @Test
    @Throws(ApiException::class)
    fun testOidcOverrideProvided() {
        val oidcJwtToken = "OIDC_JWT_TOKEN"
        val providers = ApiAuthProviders.builder()
            .oidcAuthProvider { oidcJwtToken }
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.OPENID_CONNECT,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/").build()
        val decoratedRequest =
            factory.forAuthType(AuthorizationType.OPENID_CONNECT).decorate(request)
        Assert.assertEquals(oidcJwtToken, decoratedRequest.header(AUTHORIZATION))
    }

    /**
     * If a custom auth provider is not provided AND there is
     * an API that uses [AuthorizationType.AWS_LAMBDA]
     * as its auth mechanism, then the process will fail at runtime
     * while sending a request with that API.
     */
    @Test
    fun testCustomOverrideNotProvided() {
        val providers = ApiAuthProviders.noProviderOverrides()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_LAMBDA,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/").build()
        Assert.assertThrows(
            ApiAuthException::class.java
        ) { factory.forAuthType(AuthorizationType.AWS_LAMBDA).decorate(request) }
    }

    /**
     * Test to confirm that passing any custom implementation of
     * [FunctionAuthProvider]
     * prevents crashes while intercepting requests.
     * @throws ApiException From API configuration
     */
    @Test
    @Throws(ApiException::class)
    fun testCustomOverrideProvided() {
        val customToken = "CUSTOM_TOKEN"
        val providers = ApiAuthProviders.builder()
            .functionAuthProvider { customToken }
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_LAMBDA,
            "",
            EndpointType.GRAPHQL,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/").build()
        val decoratedRequest = factory.forAuthType(AuthorizationType.AWS_LAMBDA).decorate(request)
        Assert.assertEquals(customToken, decoratedRequest.header(AUTHORIZATION))
    }

    @Test
    fun testProvidedRestContentTypeHeaderUsed() {
        val expectedContentType = "text/plain"
        val credentialsProvider = object : CredentialsProvider {
            override suspend fun resolve(attributes: Attributes): Credentials {
                return Credentials("testA", "testB")
            }
        }
        val providers = ApiAuthProviders.builder()
            .awsCredentialsProvider(credentialsProvider)
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_IAM,
            "",
            EndpointType.REST,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/")
            .post("hello".toByteArray().toRequestBody())
            .header("Content-Type", expectedContentType)
            .build()

        val decoratedRequest = factory.forAuthType(AuthorizationType.AWS_IAM).decorate(request)

        assertEquals(expectedContentType.toMediaType(), decoratedRequest.body!!.contentType())
    }

    @Test
    fun testProvidedRestContentTypeIgnoreCaseHeaderUsed() {
        val expectedContentType = "tExT/pLaIn"
        val credentialsProvider = object : CredentialsProvider {
            override suspend fun resolve(attributes: Attributes): Credentials {
                return Credentials("testA", "testB")
            }
        }
        val providers = ApiAuthProviders.builder()
            .awsCredentialsProvider(credentialsProvider)
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_IAM,
            "",
            EndpointType.REST,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/")
            .post("hello".toByteArray().toRequestBody())
            .header("CoNtENT-tYpE", expectedContentType)
            .build()

        val decoratedRequest = factory.forAuthType(AuthorizationType.AWS_IAM).decorate(request)

        assertEquals(expectedContentType.toMediaType(), decoratedRequest.body!!.contentType())
    }

    @Test
    fun testDefaultRestContentTypeHeaderUsed() {
        val expectedContentType = "application/json"
        val credentialsProvider = object : CredentialsProvider {
            override suspend fun resolve(attributes: Attributes): Credentials {
                return Credentials("testA", "testB")
            }
        }
        val providers = ApiAuthProviders.builder()
            .awsCredentialsProvider(credentialsProvider)
            .build()
        val factory = ApiRequestDecoratorFactory(
            providers,
            AuthorizationType.AWS_IAM,
            "",
            EndpointType.REST,
            "CONFIG_API_KEY"
        )
        val request: Request = Builder().url("https://localhost/")
            .post("hello".toByteArray().toRequestBody())
            .build()

        val decoratedRequest = factory.forAuthType(AuthorizationType.AWS_IAM).decorate(request)

        assertEquals(expectedContentType.toMediaType(), decoratedRequest.body!!.contentType())
    }

    companion object {
        private const val X_API_KEY = "x-api-key"
        private const val AUTHORIZATION = "authorization"
    }
}
