
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
import com.amplifyframework.api.aws.auth.RequestDecorator
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.annotations.AuthRule
import com.amplifyframework.core.model.annotations.ModelConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.ExecutorService
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiAuthAppSyncGraphQLOperationTest {

    private val client = mockk<OkHttpClient>(relaxed = true)
    private val onResponse2 = mockk<Consumer<GraphQLResponse<ModelWithTwoAuthModes>>>(relaxed = true)
    private val onResponse1 = mockk<Consumer<GraphQLResponse<ModelWithOneAuthMode>>>(relaxed = true)
    private val onFailure = mockk<Consumer<ApiException>>(relaxed = true)
    private val executorService = mockk<ExecutorService>(relaxed = true)
    private val request2 = ModelQuery[ModelWithTwoAuthModes::class.java, "1"]
    private val request1 = ModelQuery[ModelWithOneAuthMode::class.java, "1"]
    private val responseFactoryMock = mockk<GsonGraphQLResponseFactory>()
    private val apiRequestDecoratorFactory = mockk<ApiRequestDecoratorFactory>()
    private val requestDecorator = mockk<RequestDecorator>()
    private val decoratedOkHttpRequest = mockk<Request>()
    private val mockCall = mockk<okhttp3.Call>()

    @Test
    fun `submit dispatchRequest when more auth types available then fail`() {
        val operation = MultiAuthAppSyncGraphQLOperation.Builder<ModelWithTwoAuthModes>()
            .client(client)
            .request(request2)
            .responseFactory(responseFactoryMock)
            .onResponse(onResponse2)
            .onFailure(onFailure)
            .apiRequestDecoratorFactory(apiRequestDecoratorFactory)
            .executorService(executorService)
            .endpoint("https://amazon.com")
            .apiName("TestAPI")
            .build()

        val response = buildResponse()
        val exception = buildAuthException()

        val gqlErrors = buildGQLErrors()
        val gqlResponse = GraphQLResponse<ModelWithTwoAuthModes>(null, mutableListOf(gqlErrors))
        gqlResponse.errors.replaceAll { gqlErrors }
        every { responseFactoryMock.buildResponse<ModelWithTwoAuthModes>(any(), any(), any()) } returns gqlResponse

        every { apiRequestDecoratorFactory.forAuthType(any()) } returns requestDecorator

        every { requestDecorator.decorate(any()) } returns decoratedOkHttpRequest

        every { executorService.submit(any()) } answers {
            firstArg<Runnable>().run()
            mockk()
        }
        // Mocks Callback
        every { client.newCall(decoratedOkHttpRequest) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            val callback = firstArg<Callback>()
            callback.onResponse(mockk(relaxed = true), response)
        }
        operation.start()

        verify(exactly = 2) {
            executorService.submit(any())
        }
        verify {
            onFailure.accept(exception)
        }
    }

    @Test
    fun `should invoke onFailure with single auth type and has auth error`() {
        val operation = MultiAuthAppSyncGraphQLOperation.Builder<ModelWithOneAuthMode>()
            .client(client)
            .request(request1)
            .responseFactory(responseFactoryMock)
            .onResponse(onResponse1)
            .onFailure(onFailure)
            .apiRequestDecoratorFactory(apiRequestDecoratorFactory)
            .executorService(executorService)
            .endpoint("https://amazon.com")
            .apiName("TestAPI")
            .build()

        val response = buildResponse()
        val exception = buildAuthException()

        val gqlErrors = buildGQLErrors()
        val gqlResponse = GraphQLResponse<ModelWithTwoAuthModes>(null, mutableListOf(gqlErrors))
        gqlResponse.errors.replaceAll { gqlErrors }

        every { responseFactoryMock.buildResponse<ModelWithTwoAuthModes>(any(), any(), any()) } returns gqlResponse

        every { apiRequestDecoratorFactory.forAuthType(any()) } returns requestDecorator

        every { requestDecorator.decorate(any()) } returns decoratedOkHttpRequest

        every { executorService.submit(any()) } answers {
            firstArg<Runnable>().run()
            mockk()
        }
        // Mocks Callback
        every { client.newCall(decoratedOkHttpRequest) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            val callback = firstArg<Callback>()
            callback.onResponse(mockk(relaxed = true), response)
        }

        operation.start()

        verify(exactly = 1) {
            executorService.submit(any())
            onFailure.accept(exception)
        }
    }

    private fun buildResponse(): Response {
        val responseBody = "{\"errors\":" +
            " [{\"message\": \"Auth error\"," +
            " \"extensions\": {\"errorType\": \"Unauthorized\"}}]}"
        return Response.Builder()
            .code(200)
            .body(responseBody.toResponseBody())
            .request(Request(url = "https://amazon.com".toHttpUrl()))
            .protocol(Protocol.HTTP_1_0)
            .message("testing for submit dispatch request when more auth types available")
            .build()
    }

    private fun buildAuthException(): ApiAuthException = ApiAuthException(
        "Unable to successfully complete request with any of the compatible auth types.",
        "Check your application logs for detail."
    )

    private fun buildGQLErrors(): GraphQLResponse.Error {
        val extensions: MutableMap<String, Any?> = HashMap()
        extensions["errorType"] = "Unauthorized"
        return GraphQLResponse.Error("Unauthorized", null, null, extensions)
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

    @ModelConfig(
        authRules = [
            AuthRule(
                allow = AuthStrategy.OWNER,
                operations = [ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ]
            )
        ]
    )
    private class ModelWithOneAuthMode : Model
}
