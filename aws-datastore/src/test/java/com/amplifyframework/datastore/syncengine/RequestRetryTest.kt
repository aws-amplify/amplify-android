package com.amplifyframework.datastore.syncengine

import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.ErrorType
import io.reactivex.rxjava3.core.Single
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class RequestRetryTest : TestCase() {


    @Test
    fun testRetry() {
        //arrange
        val subject = RequestRetry()
        val expectedValue = "PaginatedResult<ModelWithMetadata<BlogOwner>>"
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
        val subject = RequestRetry()
        val expectedException = DataStoreException.GraphQLResponseException("PaginatedResult<ModelWithMetadata<BlogOwner>>", listOf())
        val mockSingle = Single.error<String>( expectedException)

        //act and assert
        subject.retry(mockSingle, listOf(DataStoreException.GraphQLResponseException::class.java))
            .test()
            .awaitDone(1,TimeUnit.SECONDS)
            .assertError(expectedException)
            .isDisposed
    }

    @Test
    fun testRetryOnRecoverableError() {
        //arrange
        val subject = RequestRetry(jitterFactor = 0, maxAttempts = 1)
        val expectedException = DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>","")
        val mockSingle = Single.error<String>( expectedException)

        //act and assert
        subject.retry(mockSingle, listOf())
            .test()
            .awaitDone(10,TimeUnit.SECONDS)
            .assertError(expectedException)
            .isDisposed
    }

}