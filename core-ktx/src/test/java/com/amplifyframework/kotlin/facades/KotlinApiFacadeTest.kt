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

package com.amplifyframework.kotlin.facades

import com.amplifyframework.api.ApiCategoryBehavior
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLOperation
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.kotlin.GraphQL.ConnectionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that the KotlinApiFacade calls through to the
 * underlying ApiCategoryBehavior appropriately.
 */
@FlowPreview
@ExperimentalCoroutinesApi
@Suppress("UNCHECKED_CAST")
class KotlinApiFacadeTest {
    private val delegate = mockk<ApiCategoryBehavior>()
    private val api = KotlinApiFacade(delegate)

    /**
     * When the underlying query() delegate emits a response,
     * it should be returned from the coroutine API.
     */
    @Test
    fun querySucceeds() = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedResponse = GraphQLResponse<String>("Hello", emptyList())
        every {
            delegate.query(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<GraphQLResponse<String>>
            onResult.accept(expectedResponse)
            mockk<GraphQLOperation<String>>()
        }
        assertEquals(expectedResponse, api.query(request))
    }

    /**
     * When the underlying query() delegate emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun queryThrows(): Unit = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.query(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk<GraphQLOperation<String>>()
        }
        api.query(request)
    }

    /**
     * When the underlying mutate() delegate emits a response,
     * it should be returned from the coroutine API.
     */
    @Test
    fun mutateSucceeds() = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedResponse = GraphQLResponse<String>("Hello", emptyList())
        every {
            delegate.mutate(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* indexOfResultConsumer = */ 1]
            val onResult = onResultArg as Consumer<GraphQLResponse<String>>
            onResult.accept(expectedResponse)
            mockk<GraphQLOperation<String>>()
        }
        assertEquals(expectedResponse, api.mutate(request))
    }

    /**
     * When the underlying mutate() emits an error, the coroutine
     * API should throw it.
     */
    @Test(expected = ApiException::class)
    fun mutateThrows(): Unit = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.mutate(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk<GraphQLOperation<String>>()
        }
        api.mutate(request)
    }

    /**
     * When the underlying subscribe() begins a subscription, the Kotlin API
     * should see a CONNECTED connection state, follows by values on the
     * subscriptionData Flow.
     */
    @Test
    fun subscribeSucceeds() = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedResponse = GraphQLResponse<String>("Howdy!", emptyList())
        val expectedOperation = mockk<GraphQLOperation<String>>()
        every {
            delegate.subscribe(eq(request), any(), any(), any(), any())
        } answers {
            val onStartArg = it.invocation.args[/* index of start consumer = */ 1]
            val onResultArg = it.invocation.args[/* index of result consumer = */ 2]
            val onStart = onStartArg as Consumer<String>
            val onResult = onResultArg as Consumer<GraphQLResponse<String>>
            onStart.accept("startToken")
            onResult.accept(expectedResponse)
            expectedOperation
        }
        every { expectedOperation.cancel() } answers {}

        val operation = api.subscribe(request)
        assertEquals(ConnectionState.CONNECTED, operation.connectionState.first())
        assertEquals(expectedResponse, operation.subscriptionData.first())
    }

    /**
     * When the underlying subscribe() API emits an error,
     * the coroutine API should throw it.
     */
    @Test(expected = ApiException::class)
    fun subscribeThrows(): Unit = runBlocking {
        val request = mockk<GraphQLRequest<String>>()
        val expectedFailure = ApiException("uh", "oh")

        val operation = mockk<GraphQLOperation<String>>()
        every { operation.cancel() } answers {}

        every {
            delegate.subscribe(eq(request), any(), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            operation
        }
        api.subscribe(request)
            .subscriptionData.first() // Wait for data that never comes, instead an error
    }

    /**
     * When the underlying get() emits a response,
     * it should be returned from the coroutine API.
     */
    @Test
    fun getSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.get(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.get(request))
    }

    /**
     * When the get() delegate emits an error, it should be
     * thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun getThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.get(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.get(request)
    }

    /**
     * When the underlying put() emits a response, it should
     * be returned from the Kotlin coroutine API.
     */
    @Test
    fun putSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.put(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.put(request))
    }

    /**
     * When the put() delegate emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun putThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.put(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.put(request)
    }

    /**
     * When the underlying post() delegate emits a response,
     * it should be returned from the coroutine API.
     */
    @Test
    fun postSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.post(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.post(request))
    }

    /**
     * When the post() delegate emits an exception,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun postThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.post(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.post(request)
    }

    /**
     * When the underlying head() emits a response, it should be
     * returned from the Kotlin coroutine API.
     */
    @Test
    fun deleteSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.delete(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.delete(request))
    }

    @Test(expected = ApiException::class)
    fun deleteThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.delete(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.delete(request)
    }

    /**
     * When the head() delegate emits a response, it should be
     * returned from the coroutine API.
     */
    @Test
    fun headSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.head(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.head(request))
    }

    /**
     * When the underlying head() emits an error, it should
     * be thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun headThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.head(eq(request), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.head(request)
    }

    /**
     * When the underlying patch() emits a response, the
     * coroutine API should return it.
     */
    @Test
    fun patchSucceeds() = runBlocking {
        val request = RestOptions.builder().build()
        val expectedResponse = RestResponse(200, "Nice one, bruf!".toByteArray())
        every {
            delegate.patch(eq(request), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 1]
            val onResult = onResultArg as Consumer<RestResponse>
            onResult.accept(expectedResponse)
            mockk()
        }
        assertEquals(expectedResponse, api.patch(request))
    }

    /**
     * When the underlying patch() emits an error, it should
     * be thrown from the coroutine API.
     */
    @Test(expected = ApiException::class)
    fun patchThrows(): Unit = runBlocking {
        val request = RestOptions.builder().build()
        val expectedFailure = ApiException("uh", "oh")
        every {
            delegate.patch(eq(request), any(), any())
        } answers {
            val onErrorArg = it.invocation.args[/* index of error consumer = */ 2]
            val onError = onErrorArg as Consumer<ApiException>
            onError.accept(expectedFailure)
            mockk()
        }
        api.patch(request)
    }
}
