
/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.ApiException.ApiAuthException
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.annotations.AuthRule
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.auth.AuthorizationTypeIterator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.ExecutorService
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
class MultiAuthAppSyncGraphQLOperationTest {

    private lateinit var client: OkHttpClient
    private lateinit var onResponse: Consumer<GraphQLResponse<String>>
    private lateinit var onFailure: Consumer<ApiException>
    private lateinit var executorService: ExecutorService
    private lateinit var apiRequestDecoratorFactory: ApiRequestDecoratorFactory
    private lateinit var request: GraphQLRequest<String>
    private lateinit var responseFactory: GraphQLResponse.Factory
    private lateinit var authTypes: AuthorizationTypeIterator

    private lateinit var responseBody: ResponseBody
    private lateinit var responseMock: Response

    private lateinit var responseFactoryMock: GsonGraphQLResponseFactory
    private lateinit var graphQLResponseMock: GraphQLResponse<String>
    private lateinit var graphQLResponseErrorMock: GraphQLResponse.Error

    private lateinit var operation: MultiAuthAppSyncGraphQLOperation<String>

    @Before
    fun setUp() {
        authTypes = mockk(relaxed = true)
        client = mockk(relaxed = true)
        onResponse = mockk(relaxed = true)
        onFailure = mockk(relaxed = true)
        executorService = mockk(relaxed = true)
        apiRequestDecoratorFactory = mockk()
        responseFactory = mockk()
        request = mockk<AppSyncGraphQLRequest<String>> {
            every { modelSchema } returns ModelSchema.fromModelClass(ModelWithTwoAuthModes::class.java)
            every { authRuleOperation } returns ModelOperation.READ
            every { content } returns ""
        }

        responseBody = mockk<ResponseBody>(relaxed = true)
        responseMock = mockk<Response>(relaxed = true)

        responseFactoryMock = mockk<GsonGraphQLResponseFactory>(relaxed = true)
        graphQLResponseMock = mockk<GraphQLResponse<String>>(relaxed = true)
        graphQLResponseErrorMock = mockk<GraphQLResponse.Error> {
            every { message } returns "Unauthorized"
            every { extensions } returns mapOf("errorType" to "Unauthorized")
        }

        operation = MultiAuthAppSyncGraphQLOperation.Builder<String>()
            .client(client)
            .request(request)
            .responseFactory(responseFactoryMock)
            .onResponse(onResponse)
            .onFailure(onFailure)
            .apiRequestDecoratorFactory(apiRequestDecoratorFactory)
            .executorService(executorService)
            .endpoint("https://api.example.com")
            .apiName("TestAPI")
            .build()
    }

    @Test
    fun `should build operation successfully with all required parameters`() {

        assertNotNull(operation)
    }

    // should submit dispatchRequest on auth-related error when more auth types are available
    @Test
    fun `submit dispatchRequest when more auth types available`() {
        every { responseBody.string() } returns "{\"errors\":" +
            " [{\"message\": \"Auth error\"," +
            " \"extensions\": {\"errorType\": \"Unauthorized\"}}]}"
        every { responseMock.isSuccessful } returns true
        every { responseMock.body } returns responseBody
        every { responseFactoryMock.buildResponse<String>(any(), any(), any()) } returns graphQLResponseMock
        every { graphQLResponseMock.hasErrors() } returns true
        every { operation.hasAuthRelatedErrors(graphQLResponseMock) } returns true
        every { authTypes.hasNext() } returns true
        every { graphQLResponseMock.errors } returns listOf(graphQLResponseErrorMock)

        operation.setAuthTypes(authTypes)
        operation.OkHttpCallback().onResponse(mockk(relaxed = true), responseMock)

        verify {
            executorService.submit(any())
        }
    }

    @Test
    fun `should invoke onFailure when no more auth types and has auth errors`() {
        val exception = ApiAuthException(
            "Unable to successfully complete request with any of the compatible auth types.",
            "Check your application logs for detail."
        )

        every { responseBody.string() } returns "{\"errors\":" +
            " [{\"message\": \"Auth error\"," +
            " \"extensions\": {\"errorType\": \"Unauthorized\"}}]}"
        every { responseMock.isSuccessful } returns true
        every { responseMock.body } returns responseBody
        every { responseFactoryMock.buildResponse<String>(any(), any(), any()) } returns graphQLResponseMock
        every { graphQLResponseMock.hasErrors() } returns true
        every { operation.hasAuthRelatedErrors(graphQLResponseMock) } returns true
        every { authTypes.hasNext() } returns false // No more auth types available
        every { graphQLResponseMock.errors } returns listOf(graphQLResponseErrorMock)

        operation.setAuthTypes(authTypes)
        operation.OkHttpCallback().onResponse(mockk(relaxed = true), responseMock)

        verify {
            onFailure.accept(exception)
        }
    }

    @ModelConfig(
        authRules = [
            AuthRule(
                allow = AuthStrategy.OWNER,
                operations = [ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ]
            ), AuthRule(
                allow = AuthStrategy.PUBLIC,
                operations = [ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ]
            )
        ]
    )
    private class ModelWithTwoAuthModes : Model
}
