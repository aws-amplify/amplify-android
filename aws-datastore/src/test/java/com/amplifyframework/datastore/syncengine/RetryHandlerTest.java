package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.datastore.DataStoreException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;

public class RetryHandlerTest {

    @Test
    public void testRetry() {
        //arrange
        RetryHandler subject = new RetryHandler();
        String expectedValue = "Test value";
        Single<String> mockSingle = Single.create(emitter -> emitter.onSuccess(expectedValue));

        //act and assert
        subject.retry(mockSingle, new ArrayList<>())
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValue(expectedValue)
                .isDisposed();
    }

    @Test
    public void testNoRetryOnIrrecoverableError() {
        //arrange
        RetryHandler subject = new RetryHandler();
        DataStoreException expectedException = new DataStoreException.GraphQLResponseException("PaginatedResult<ModelWithMetadata<BlogOwner>>", new ArrayList<>());
        Single<String> mockSingle = Single.error(expectedException);
        ArrayList<Class<? extends Throwable>> skipExceptionList = new ArrayList<>();
        skipExceptionList.add(DataStoreException.GraphQLResponseException.class);

        //act and assert
        subject.retry(mockSingle, skipExceptionList)
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertError(expectedException)
                .isDisposed();
    }

    @Test
    public void testRetryOnRecoverableError() {
        //arrange
        RetryHandler subject = new RetryHandler(8, 0, 1);
        DataStoreException expectedException = new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "");
        AtomicInteger count = new AtomicInteger(0);

        Single<Object> mockSingle = Single.error(expectedException)
                .doOnError(e -> count.incrementAndGet());

        //act and assert
        subject.retry(mockSingle, new ArrayList<>())
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(expectedException)
                .isDisposed();

        assertEquals(2, count.get());
    }

    @Test
    public void testJitteredDelaySec() {
        //arrange
        RetryHandler subject = new RetryHandler(8, 0, 1);
        //act
        long delay = subject.jitteredDelaySec(2);
        //assert
        assertEquals(4, delay);
    }

}
