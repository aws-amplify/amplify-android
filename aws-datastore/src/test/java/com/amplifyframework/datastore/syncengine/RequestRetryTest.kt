package com.amplifyframework.datastore.syncengine

import com.amplifyframework.datastore.DataStoreException
import io.reactivex.rxjava3.core.Single
import junit.framework.TestCase
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RequestRetryTest : TestCase() {


    @Test
    fun testRetry() {
        //arrange
        val subject = RetryHandler()
        val expectedValue = "Test value"
        val mockSingle = Single.create<String> {emitter-> emitter.onSuccess(expectedValue)   }

        //act and assert
        subject.retry(mockSingle, listOf())
            .test()
            .awaitDone(1,TimeUnit.SECONDS)
            .assertNoErrors()
            .assertValue(expectedValue)
            .isDisposed
    }

    @Test
    fun testRetryError() {
        //arrange
        val subject = RetryHandler()
        val expectedException = DataStoreException.GraphQLResponseException("PaginatedResult<ModelWithMetadata<BlogOwner>>", listOf())
        val mockSingle = Single.error<String>( expectedException)

        //act and assert
        subject.retry(mockSingle, listOf(DataStoreException.GraphQLResponseException::class.java))
            .test()
            .awaitDone(10,TimeUnit.SECONDS)
            .assertError(expectedException)
            .isDisposed
    }

    @Test
    fun testRetryOnRecoverableError() {
        //arrange
        val subject = RetryHandler(jitterFactor = 0, maxAttempts = 1)
        val expectedException = DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>","")
        val count = AtomicInteger(0)

        val mockSingle = Single.error<String> {
            count.incrementAndGet()
            expectedException
        }
        //act and assert
        subject.retry(mockSingle, listOf())
            .test()
            .awaitDone(10,TimeUnit.SECONDS)
            .assertError(expectedException)
            .isDisposed

        assertEquals(2,count.get())
    }


    @Test
    fun testJitteredDelaySec() {
        //arrange
        val subject = RetryHandler(jitterFactor = 0, maxAttempts = 1)
        //act
        val delay = subject.jitteredDelaySec(2)
        //assert
        assertEquals(4,delay)
    }


}