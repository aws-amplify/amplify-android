/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.aws.auth.AuthRuleRequestDecorator
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.annotations.AuthRule
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.util.MockExecutorService
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.ExecutorService
import org.junit.Test

/**
 * Unit tests for the [MultiAuthSubscriptionOperation] class.
 */
class MultiAuthSubscriptionOperationTest {

    private val executor = MockExecutorService()
    private val endpoint = MockSubscriptionEndpoint()
    private val decorator: AuthRuleRequestDecorator = mockk(relaxed = true)

    private val onStart: (String) -> Unit = mockk(relaxed = true)
    private val onNext: (GraphQLResponse<String>) -> Unit = mockk(relaxed = true)
    private val onError: (ApiException) -> Unit = mockk(relaxed = true)
    private val onComplete: () -> Unit = mockk(relaxed = true)

//region start tests

    @Test
    fun `start emits exception if already cancelled`() {
        val operation = createOperation()
        operation.isCanceled = true
        operation.start()
        verify {
            onError(any())
        }
    }

//endregion
//region cancellation tests

    @Test
    fun `cancel cancels the subscription future`() {
        executor.autoRunTasks = false
        val operation = createOperation()
        operation.start()
        operation.cancel()
        operation.subscriptionFuture.isCancelled.shouldBeTrue()
    }

    @Test
    fun `cancel does nothing if not started`() {
        executor.autoRunTasks = false
        val operation = createOperation()
        operation.cancel()
        executor.queue.shouldBeEmpty()
    }

    @Test
    fun `cancel releases subscription`() {
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnStart()
        operation.cancel()
        endpoint.verifyReleased()
    }

//endregion
//region error tests

    @Test
    fun `operation is cancelled if authtypes are exhausted`() {
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnStart()
        endpoint.invokeOnError(ApiAuthException("", "")) // error on first auth type
        endpoint.invokeOnStart()
        endpoint.invokeOnError(ApiAuthException("", "")) // error on second auth type
        endpoint.verifyReleased()
    }

    @Test
    fun `operation emits ApiAuthException if authtypes are exhausted`() {
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnError(ApiAuthException("", "")) // error on first auth type
        endpoint.invokeOnError(ApiAuthException("", "")) // error on second auth type
        verify {
            onError(any<ApiAuthException>())
        }
    }

    @Test
    fun `next authtype is tried if requestDecorator throws ApiAuthException`() {
        every { decorator.decorate(any<GraphQLRequest<*>>(), any()) } throws ApiAuthException("", "")
        val operation = createOperation()
        operation.start()
        executor.numTasksQueued shouldBe 2 // One for each auth type
        endpoint.verifyNumSubscriptionRequests(1) // Only one should send a subscription request
    }

    @Test
    fun `next authtype is tried if response contains auth errors`() {
        val response = mockAuthErrorResponse()
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnResponse(response)
        executor.numTasksQueued shouldBe 2 // One for each auth type
        endpoint.verifyNumSubscriptionRequests(2)
    }

    @Test
    fun `error is emitted if requestDecorator throws ApiException`() {
        val exception = ApiException("", "")
        every { decorator.decorate(any<GraphQLRequest<*>>(), any()) } throws exception
        val operation = createOperation()
        operation.start()
        verify {
            onError(exception)
        }
    }

//endregion
//region Success tests

    @Test
    fun `onStart is called`() {
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnStart()
        verify {
            onStart(endpoint.subscriptionId)
        }
    }

    @Test
    fun `onNext is called`() {
        val response = mockk<GraphQLResponse<String>>() {
            every { hasErrors() } returns false
        }
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnResponse(response)
        verify {
            onNext(response)
        }
    }

    @Test
    fun `onComplete is called`() {
        val operation = createOperation()
        operation.start()
        endpoint.invokeOnComplete()
        verify {
            onComplete()
        }
    }

//endregion

    private fun createOperation(
        subscriptionEndpoint: SubscriptionEndpoint = endpoint.endpoint,
        onSubscriptionStart: (String) -> Unit = onStart,
        onNextItem: (GraphQLResponse<String>) -> Unit = onNext,
        onSubscriptionError: (ApiException) -> Unit = onError,
        onSubscriptionComplete: () -> Unit = onComplete,
        executorService: ExecutorService = executor,
        requestDecorator: AuthRuleRequestDecorator = decorator,
        graphQlRequest: AppSyncGraphQLRequest<String> = mockk {
            every { modelSchema } returns ModelSchema.fromModelClass(ModelWithTwoAuthModes::class.java)
            every { authRuleOperation } returns ModelOperation.READ
            every { content } returns ""
        }
    ): MultiAuthSubscriptionOperation<String> {
        val operation = MultiAuthSubscriptionOperation.builder<String>()
            .subscriptionEndpoint(subscriptionEndpoint)
            .onSubscriptionStart(onSubscriptionStart)
            .onNextItem(onNextItem)
            .onSubscriptionError(onSubscriptionError)
            .onSubscriptionComplete(onSubscriptionComplete)
            .executorService(executorService)
            .requestDecorator(requestDecorator)
            .graphQlRequest(graphQlRequest)
            .responseFactory(mockk())
            .build()
        return operation
    }

    private fun mockAuthErrorResponse() = mockk<GraphQLResponse<String>> {
        every { hasErrors() } returns true
        every { errors } returns listOf(
            mockk { every { extensions } returns mapOf("errorType" to "Unauthorized") }
        )
    }

    private class MockSubscriptionEndpoint {
        val endpoint = mockk<SubscriptionEndpoint>() {
            every { releaseSubscription(any()) } just Runs
        }
        val subscriptionId = "subId"

        var onStart: Consumer<String>? = null
        var onResponse: Consumer<GraphQLResponse<String>>? = null
        var onError: Consumer<ApiException>? = null
        var onComplete: Action? = null

        init {
            every { endpoint.requestSubscription<String>(any(), any(), any(), any(), any(), any()) } answers {
                onStart = arg(2)
                onResponse = arg(3)
                onError = arg(4)
                onComplete = arg(5)
            }
        }

        fun invokeOnStart() = onStart?.accept(subscriptionId)
        fun invokeOnResponse(response: GraphQLResponse<String>) = onResponse?.accept(response)
        fun invokeOnError(error: ApiException) = onError?.accept(error)
        fun invokeOnComplete() = onComplete?.call()

        fun verifyReleased() = verify {
            endpoint.releaseSubscription(subscriptionId)
        }

        fun verifyNumSubscriptionRequests(expected: Int) = verify(exactly = expected) {
            endpoint.requestSubscription<String>(any(), any(), any(), any(), any(), any())
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
