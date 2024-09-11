/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync

import com.amplifyframework.apollo.appsync.util.AccessTokenProvider
import com.amplifyframework.apollo.appsync.util.ApolloRequestSigner
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.apollographql.apollo.api.http.HttpRequest
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApolloAmplifyConnectorTest {
    private val serverUrl = "https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql"

    private val outputs = mockk<AmplifyOutputsData.Data> {
        every { url } returns serverUrl
        every { awsRegion } returns "us-east-1"
        every { apiKey } returns "test-key"
    }

    @Test
    fun `reads endpoint from AmplifyOutputs`() {
        val connector = ApolloAmplifyConnector(outputs)
        connector.endpoint.serverUrl.toString() shouldBe serverUrl
    }

    @Test
    fun `reads region from AmplifyOutputs`() {
        val connector = ApolloAmplifyConnector(outputs)
        connector.region shouldBe "us-east-1"
    }

    @Test
    fun `reads api key from AmplifyOutputs`() {
        val connector = ApolloAmplifyConnector(outputs)
        connector.apiKey shouldBe "test-key"
    }

    @Test
    fun `handles null apiKey`() {
        every { outputs.apiKey } returns null
        val connector = ApolloAmplifyConnector(outputs)
        connector.apiKey.shouldBeNull()
    }

    @Test
    fun `apiKey authorizer uses configured API Key`() = runTest {
        val connector = ApolloAmplifyConnector(outputs)
        val authorizer = connector.apiKeyAuthorizer()
        authorizer.getHttpAuthorizationHeaders(mockk()) shouldContain ("X-Api-Key" to "test-key")
    }

    @Test
    fun `token authorizer gets token from Amplify`() = runTest {
        val provider = mockk<AccessTokenProvider> {
            coEvery { fetchLatestCognitoAuthToken() } returns "test-token"
        }
        val connector = ApolloAmplifyConnector(outputs, accessTokenProvider = provider)
        val authorizer = connector.cognitoUserPoolAuthorizer()
        authorizer.getHttpAuthorizationHeaders(mockk()) shouldContain ("authorization" to "test-token")
    }

    @Test
    fun `iam authorizer gets token from amplify`() = runTest {
        val request = mockk<HttpRequest>()
        val signer = mockk<ApolloRequestSigner> {
            coEvery { signAppSyncRequest(request, "us-east-1") } returns mapOf("Authorization" to "test-signature")
        }
        val connector = ApolloAmplifyConnector(outputs, requestSigner = signer)
        val authorizer = connector.iamAuthorizer()

        authorizer.getHttpAuthorizationHeaders(request) shouldContain ("Authorization" to "test-signature")
    }
}
