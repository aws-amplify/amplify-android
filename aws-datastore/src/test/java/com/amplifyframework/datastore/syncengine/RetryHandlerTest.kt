package com.amplifyframework.datastore.syncengine

import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.SingleEmitter
import junit.framework.TestCase
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RetryHandlerTest : TestCase() {


    @Test
    fun testRetry() {
        //arrange
        val countDownLatch = CountDownLatch(1)
        val mockAppSync = mockk<AppSync>(relaxed = true)
        every { mockAppSync.sync<BlogOwner>(any(),any(), any()) } answers  {
            countDownLatch.countDown()
            mockk()
        }

        val subject = RequestRetry()
        val emitter = mockk<SingleEmitter<PaginatedResult<ModelWithMetadata<BlogOwner>>>>(relaxed = true)
        every { emitter.setDisposable(any()) } answers {}

        //act
        subject.retry(RetryHandler(emitter,
            mockAppSync,
            mockk(),
            mockk(),
            mockk()))

        //assert
        assertTrue(countDownLatch.await(3, TimeUnit.SECONDS))
    }
}