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

package com.amplifyframework.kotlin.datastore

import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.ObserveQueryOptions
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.DataStoreCategoryBehavior
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStoreItemChange
import com.amplifyframework.datastore.DataStoreItemChange.Initiator.LOCAL
import com.amplifyframework.datastore.DataStoreItemChange.Type.CREATE
import com.amplifyframework.datastore.DataStoreItemChange.Type.DELETE
import com.amplifyframework.datastore.DataStoreQuerySnapshot
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that calls to the Kotlin DataStore APIs are correctly wired
 * to the underlying DataStoreCategoryBehavior delegate.
 */
@FlowPreview
@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class KotlinDataStoreFacadeTest {
    private val delegate = mockk<DataStoreCategoryBehavior>()
    private val dataStore = KotlinDataStoreFacade(delegate)

    /**
     * Verify that a call to save() falls through to the delegate.
     * When the delegate succeeds, so does the coroutine API.
     */
    @Test
    fun saveSucceeds() = runBlocking {
        val bart = BlogOwner.builder()
            .name("Bart Simpson")
            .build()
        every {
            delegate.save(eq(bart), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer]
                as Consumer<DataStoreItemChange<BlogOwner>>
            onResult.accept(
                DataStoreItemChange.builder<BlogOwner>()
                    .initiator(LOCAL)
                    .item(bart)
                    .itemClass(BlogOwner::class.java)
                    .type(CREATE)
                    .build()
            )
        }
        dataStore.save(bart)
        verify {
            delegate.save(eq(bart), any(), any(), any())
        }
    }

    /**
     * Verify that a call to save() falls through to the delegate.
     * When the delegate emits an error, the coroutine API should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun saveThrows() = runBlocking {
        val bart = BlogOwner.builder()
            .name("Bart Simpson")
            .build()
        val error = DataStoreException("uh", "oh")
        every {
            delegate.save(eq(bart), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.save(bart)
    }

    /**
     * When item-based delete() coroutine is called, it passes through to
     * the delegate When delegate succeeds, so too does the coroutine API.
     */
    @Test
    fun deleteItemSucceeds() = runBlocking {
        val bart = BlogOwner.builder()
            .name("Bart Simpson")
            .build()
        every {
            delegate.delete(eq(bart), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer]
                as Consumer<DataStoreItemChange<BlogOwner>>
            onResult.accept(
                DataStoreItemChange.builder<BlogOwner>()
                    .initiator(LOCAL)
                    .item(bart)
                    .itemClass(BlogOwner::class.java)
                    .type(DELETE)
                    .build()
            )
        }
        dataStore.delete(bart)
        verify {
            delegate.delete(eq(bart), any(), any(), any())
        }
    }

    /**
     * Verify that a call to the item-based delete() falls through to the delegate.
     * When the delegate emits an error, the coroutine API should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun deleteItemFails() = runBlocking {
        val bart = BlogOwner.builder()
            .name("Bart Simpson")
            .build()
        val error = DataStoreException("uh", "oh")
        every {
            delegate.delete(eq(bart), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.delete(bart)
    }

    /**
     * When class-based delete() coroutine is called, it passes through to
     * the delegate When delegate succeeds, so too does the coroutine API.
     */
    @Test
    fun deleteByClassSucceeds() = runBlocking {
        every {
            delegate.delete(eq(BlogOwner::class.java), any(), any(), any())
        } answers {
            val indexOfCompletionAction = 2
            val onCompletion = it.invocation.args[indexOfCompletionAction] as Action
            onCompletion.call()
        }
        dataStore.delete(BlogOwner::class)
        verify {
            delegate.delete(eq(BlogOwner::class.java), any(), any(), any())
        }
    }

    /**
     * Verify that a call to the class-based delete() falls through to the delegate.
     * When the delegate emits an error, the coroutine API should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun deleteByClassThrows() = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.delete(eq(BlogOwner::class.java), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onErrorArg = it.invocation.args[indexOfErrorConsumer]
            val onError = onErrorArg as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.delete(BlogOwner::class)
    }

    /**
     * When values are returned by the query delegate, they should be
     * emitted as a flow on the Kotlin facade.
     */
    @Test
    fun querySucceeds(): Unit = runBlocking {
        val clazz = BlogOwner::class.java
        val blogOwners = listOf(
            BlogOwner.builder()
                .name("Beatrice T. Smithers")
                .build(),
            BlogOwner.builder()
                .name("Chuck & JoJo, Husband & Wife")
                .build()
        )
        every {
            delegate.query(eq(clazz), any<QueryOptions>(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onError = it.invocation.args[indexOfResultConsumer] as Consumer<Iterator<BlogOwner>>
            onError.accept(blogOwners.iterator())
        }
        assertEquals(blogOwners, dataStore.query(BlogOwner::class).toList())
    }

    /**
     * When the delegate query() fails, the error should bubble up through
     * the Kotlin facade.
     */
    @Test(expected = DataStoreException::class)
    fun queryFails(): Unit = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.query(eq(BlogOwner::class.java), any<QueryOptions>(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.query(BlogOwner::class)
            .toList() // Sufficient to exhaust the call chain
    }

    /**
     * When the underlying observe() method starts and emits values,
     * the Kotlin flow API should emit them onto the flow. When the user
     * is no longer interested in the flow, cancel() should be called on the
     * underlying subscription.
     */
    @Test
    fun observeAllSucceeds(): Unit = runBlocking {
        val cancelable = mockk<Cancelable>()
        val itemCreated = DataStoreItemChange.builder<BlogOwner>()
            .item(
                BlogOwner.builder()
                    .name("Susan S. Sweeney")
                    .build()
            )
            .itemClass(BlogOwner::class.java)
            .initiator(LOCAL)
            .type(CREATE)
            .build()
        every {
            delegate.observe(any(), any(), any(), any())
        } answers {
            val onStartArg = it.invocation.args[/* index of on start = */ 0]
            val onNextArg = it.invocation.args[/* index of on next = */ 1]
            val onStart = onStartArg as Consumer<Cancelable>
            val onNext = onNextArg as Consumer<DataStoreItemChange<out Model>>
            onStart.accept(cancelable)
            onNext.accept(itemCreated)
        }
        every { cancelable.cancel() } answers {}

        val actualValue = dataStore.observe()
            .take(1) // Modify the flow so it will complete automatically after 1
            .first() // Then take the 1 item, thus completing the flow
        assertEquals(itemCreated, actualValue)

        verify { cancelable.cancel() } // AS a result of completing, cancel() is invoked.
    }

    /**
     * When the underlying observe() delegate emits an error,
     * the Kotlin flow should raise it to the user.
     */
    @Test(expected = DataStoreException::class)
    fun observeAllFails(): Unit = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.observe(any(), any(), any(), any())
        } answers {
            val indexOfOnError = 2 // 3 is Action onComplete
            val onError = it.invocation.args[indexOfOnError] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.observe().first() // sufficient to call through and cause error
    }

    /**
     * Test observe() delegate which accepts class and predicate.
     * When it emits changes, they should propagate to the Kotlin facade's Flow.
     */
    @Test
    fun observeByClassSucceeds(): Unit = runBlocking {
        val cancelable = mockk<Cancelable>()
        val itemCreated = DataStoreItemChange.builder<BlogOwner>()
            .item(
                BlogOwner.builder()
                    .name("Susan S. Sweeney")
                    .build()
            )
            .itemClass(BlogOwner::class.java)
            .initiator(LOCAL)
            .type(CREATE)
            .build()
        every {
            delegate.observe(
                eq(BlogOwner::class.java),
                any<QueryPredicate>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val onStartArg = it.invocation.args[/* index of on start = */ 2]
            val onNextArg = it.invocation.args[/* index of on next = */ 3]
            val onStart = onStartArg as Consumer<Cancelable>
            val onNext = onNextArg as Consumer<DataStoreItemChange<out Model>>
            onStart.accept(cancelable)
            onNext.accept(itemCreated)
        }
        every { cancelable.cancel() } answers {}

        val actualValue = dataStore.observe(BlogOwner::class, BlogOwner.NAME.contains("Susan"))
            .take(1) // Modify the flow so it will complete automatically after 1
            .first() // Then take the 1 item, thus completing the flow
        assertEquals(itemCreated, actualValue)

        verify { cancelable.cancel() } // AS a result of completing, cancel() is invoked.
    }

    /**
     * Tests the observe() delegate which accepts class and predicate.
     * When it raises an error, that error should bubble up through the Kotlin API's Flow.
     */
    @Test(expected = DataStoreException::class)
    fun observeByClassFails(): Unit = runBlocking {
        val error = DataStoreException("uh", "oh")
        val clazz = BlogOwner::class.java
        every {
            delegate.observe(eq(clazz), any<QueryPredicate>(), any(), any(), any(), any())
        } answers {
            val indexOfOnError = 4 // 5 is last arg, Action onComplete
            val onError = it.invocation.args[indexOfOnError] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.observe(BlogOwner::class, BlogOwner.NAME.contains("Susan"))
            .first() // sufficient to call through and cause error
    }

    /**
     * Test observeQuery() delegate which accepts class and predicate.
     * When it emits changes, they should propagate to the Kotlin facade's Flow.
     */
    @Test
    fun observeQuerySucceeds(): Unit = runBlocking {
        val cancelable = mockk<Cancelable>()
        val itemCreated = DataStoreQuerySnapshot(listOf(   BlogOwner.builder()
            .name("Susan S. Sweeney")
            .build()), true)
        every {
            delegate.observeQuery(
                eq(BlogOwner::class.java),
                any<ObserveQueryOptions>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val onStartArg = it.invocation.args[/* index of on start = */ 2]
            val onNextArg = it.invocation.args[/* index of on next = */ 3]
            val onStart = onStartArg as Consumer<Cancelable>
            val onNext = onNextArg as Consumer<DataStoreQuerySnapshot<out Model>>
            onStart.accept(cancelable)
            onNext.accept(itemCreated)
        }
        every { cancelable.cancel() } answers {}

        val actualValue = dataStore.observeQuery(BlogOwner::class, ObserveQueryOptions(BlogOwner.NAME.contains("Susan"), null))
            .take(1) // Modify the flow so it will complete automatically after 1
            .first() // Then take the 1 item, thus completing the flow
        assertEquals(itemCreated, actualValue)

        verify { cancelable.cancel() } // AS a result of completing, cancel() is invoked.
    }

    /**
     * Tests the observeQuery() delegate which accepts class and predicate.
     * When it raises an error, that error should bubble up through the Kotlin API's Flow.
     */
    @Test(expected = DataStoreException::class)
    fun observeQueryFails(): Unit = runBlocking {
        val error = DataStoreException("uh", "oh")
        val clazz = BlogOwner::class.java
        every {
            delegate.observe(eq(clazz), any<QueryPredicate>(), any(), any(), any(), any())
        } answers {
            val indexOfOnError = 4 // 5 is last arg, Action onComplete
            val onError = it.invocation.args[indexOfOnError] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.observe(BlogOwner::class, BlogOwner.NAME.contains("Susan"))
            .first() // sufficient to call through and cause error
    }

    /**
     * When the start() delegate succeeds, the coroutine API should, too.
     */
    @Test
    fun startSucceeds() = runBlocking {
        every {
            delegate.start(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        dataStore.start()
        verify {
            delegate.start(any(), any())
        }
    }

    /**
     * When the start() delegate emits an error,
     * the coroutine API should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun startThrows() = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.start(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.start()
    }

    /**
     * When the stop() delegate succeeds, the coroutine API should too.
     */
    @Test
    fun stopSucceeds() = runBlocking {
        every {
            delegate.stop(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        dataStore.stop()
        verify {
            delegate.stop(any(), any())
        }
    }

    /**
     * When the stop() delegate emits an error, the coroutine API
     * should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun stopThrows() = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.stop(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.stop()
    }

    /**
     * When the clear() delegate succeeds, the coroutine API should too.
     */
    @Test
    fun clearSucceeds() = runBlocking {
        every {
            delegate.clear(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        dataStore.clear()
        verify {
            delegate.clear(any(), any())
        }
    }

    /**
     * When the clear() delegate emits an error,
     * the coroutine API should throw it.
     */
    @Test(expected = DataStoreException::class)
    fun clearThrows() = runBlocking {
        val error = DataStoreException("uh", "oh")
        every {
            delegate.clear(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<DataStoreException>
            onError.accept(error)
        }
        dataStore.clear()
    }
}
