
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
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.annotations.AuthRule
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.auth.AuthorizationTypeIterator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutorService

class MultiAuthAppSyncGraphQLOperationTest {

    private val client = mockk<OkHttpClient>(relaxed = true)
    private val onResponse = mockk<Consumer<GraphQLResponse<ModelWithTwoAuthModes>>>(relaxed = true)
    private val onFailure = mockk<Consumer<ApiException>>(relaxed = true)
    private val executorService = mockk<ExecutorService>(relaxed = true)
    private val authTypes = mockk<AuthorizationTypeIterator>(relaxed = true)
    private val request = ModelQuery[ModelWithTwoAuthModes::class.java, "1"]

    private val responseFactoryMock = mockk<GsonGraphQLResponseFactory>()
    private lateinit var operation: MultiAuthAppSyncGraphQLOperation<ModelWithTwoAuthModes>

    @Before
    fun setup() {
        operation = MultiAuthAppSyncGraphQLOperation.Builder<ModelWithTwoAuthModes>()
            .client(client)
            .request(request)
            .responseFactory(responseFactoryMock)
            .onResponse(onResponse)
            .onFailure(onFailure)
            .apiRequestDecoratorFactory(mockk<ApiRequestDecoratorFactory>())
            .executorService(executorService)
            .endpoint("https://amazon.com")
            .apiName("TestAPI")
            .build()

    }

    // should submit dispatchRequest on auth-related error when more auth types are available
    @Test
    fun `submit dispatchRequest when more auth types available`() {

        val body = "{\"errors\":" +
                " [{\"message\": \"Auth error\"," +
                " \"extensions\": {\"errorType\": \"Unauthorized\"}}]}"
        val responseBody = body.toResponseBody()

        val response = Response.Builder()
            .code(200)
            .body(responseBody)
            .request(Request(url = "https://amazon.com".toHttpUrl()))
            .protocol(Protocol.HTTP_1_0)
            .message("testing for submit dispatch request when more auth types available")
            .build()

        val exception = ApiAuthException(
            "Unable to successfully complete request with any of the compatible auth types.",
            "Check your application logs for detail."
        )

        // Build the expected response.
        val extensions: MutableMap<String, Any?> = HashMap()
        extensions["errorType"] = "Unauthorized"
        val gqlErrors = GraphQLResponse.Error("Unauthorized", null, null, extensions)
        val gqlResponse = GraphQLResponse<ModelWithTwoAuthModes>(null, mutableListOf(gqlErrors))
        gqlResponse.errors.replaceAll {  gqlErrors }

        every { responseFactoryMock.buildResponse<ModelWithTwoAuthModes>(any(), any(), any()) } returns gqlResponse

        every { authTypes.hasNext() } returnsMany listOf(true,false)
        operation.setAuthTypes(authTypes)

        val runnableSlot = slot<Runnable>()
        every { executorService.submit(capture(runnableSlot)) } answers {
            runnableSlot.captured.run()
            mockk(relaxed = true)
        }
        operation.OkHttpCallback().onResponse(mockk(), response)

        // Made sure that it goes thru DispatchRequest then failed
        verify(exactly = 1){
            executorService.submit(capture(runnableSlot))
            onFailure.accept(exception)
        }
    }

    @Test
    fun `should invoke onFailure when no more auth types and has auth errors`() {
        val body = "{\"errors\":" +
                " [{\"message\": \"Auth error\"," +
                " \"extensions\": {\"errorType\": \"Unauthorized\"}}]}"
        val responseBody = body.toResponseBody()

        val response = Response.Builder()
            .code(200)
            .body(responseBody)
            .request(Request(url = "https://amazon.com".toHttpUrl()))
            .protocol(Protocol.HTTP_1_0)
            .message("testing for submit dispatch request when more auth types available")
            .build()

        val exception = ApiAuthException(
            "Unable to successfully complete request with any of the compatible auth types.",
            "Check your application logs for detail."
        )

        // Build the expected response.
        val extensions: MutableMap<String, Any?> = HashMap()
        extensions["errorType"] = "Unauthorized"
        val gqlErrors = GraphQLResponse.Error("Unauthorized", null, null, extensions)
        val gqlResponse = GraphQLResponse<ModelWithTwoAuthModes>(null, mutableListOf(gqlErrors))
        gqlResponse.errors.replaceAll {  gqlErrors }

        every { responseFactoryMock.buildResponse<ModelWithTwoAuthModes>(any(), any(), any()) } returns gqlResponse
        every { authTypes.hasNext() } returns false // No more auth types available

        operation.setAuthTypes(authTypes)
        operation.OkHttpCallback().onResponse(mockk(relaxed = true), response)

        verify(exactly = 1) {
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