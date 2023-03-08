/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.datastore.DataStoreException;

import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RetryHandlerTest {

    /**
     * Test no retry on success.
     */
    @Test
    public void testNoRetryOnSuccess() {
        //arrange
        RetryHandler subject = new RetryHandler();
        String expectedValue = "Test value";
        Single<String> mockSingle = Single.just(expectedValue);

        //act and assert
        subject.retry(mockSingle, new ArrayList<>())
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValue(expectedValue)
                .assertComplete();
    }

    /**
     * Test no retry on Irrecoverable error.
     */
    @Test
    public void testNoRetryOnIrrecoverableError() {
        //arrange
        TestScheduler scheduler = new TestScheduler();
        RetryHandler subject = new RetryHandler();
        DataStoreException retryableException = new DataStoreException.
                GraphQLResponseException("PaginatedResult<ModelWithMetadata<BlogOwner>>",
                new ArrayList<>());
        ApiException nonRetryableException = new ApiException("Non recoverable", "This is intentional");
        AtomicInteger counter = new AtomicInteger();
        Single<String> single = Single.create(emitter -> {
            if (counter.incrementAndGet() >= 5) {
                emitter.onError(nonRetryableException);
            } else {
                emitter.onError(retryableException);
            }

            // Advance clock by next delay
            scheduler.advanceTimeBy(subject.jitteredDelayMillis(counter.get()), TimeUnit.MILLISECONDS);
        });
        List<Class<? extends Throwable>> nonRetryableExceptionList =
                Collections.singletonList(nonRetryableException.getClass());

        //act and assert
        subject.retry(single, nonRetryableExceptionList, scheduler)
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertError(nonRetryableException);

        assertEquals(5, counter.get());
    }

    /**
     * Test cancel retries on dispose.
     */
    @Test
    public void testCancelOnDispose() {
        //arrange
        TestScheduler scheduler = new TestScheduler();
        RetryHandler subject = new RetryHandler();
        Single<String> single = Single.just("some value").delay(3, TimeUnit.SECONDS, scheduler);

        //act and assert
        TestObserver<String> retry = subject.retry(single, Collections.emptyList()).test();

        retry
                .assertEmpty()
                .assertNotComplete();

        retry.dispose();

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        retry.assertEmpty();

        assertTrue(retry.isDisposed());
    }

    /**
     * Test retry on recoverable error.
     */
    @Test
    public void testRetryOnRecoverableError() {
        //arrange
        RetryHandler subject = new RetryHandler(0, Duration.ofMinutes(1).toMillis());
        DataStoreException expectedException =
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "");
        AtomicInteger count = new AtomicInteger(0);

        Single<Object> mockSingle = Single.create(emitter -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                emitter.onError(expectedException);
            } else {
                count.incrementAndGet();
                emitter.onSuccess(true);
            }
        });

        //act and assert
        subject.retry(mockSingle, new ArrayList<>())
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertNoErrors();

        assertEquals(2, count.get());
    }

    /**
     * Test it won't retry beyond the maxDelay.
     */
    @Test
    public void testDoesNotGoBeyondMaxDelay() {
        //arrange
        TestScheduler scheduler = new TestScheduler();
        RetryHandler subject = new RetryHandler(0, Duration.ofMinutes(5).toMillis());
        DataStoreException expectedException =
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "");
        AtomicInteger count = new AtomicInteger(0);

        Single<Object> mockSingle = Single.create(emitter -> {
            emitter.onError(expectedException);
            scheduler.advanceTimeBy(subject.jitteredDelayMillis(count.incrementAndGet()), TimeUnit.MILLISECONDS);
        });

        //act and assert
        subject.retry(mockSingle, new ArrayList<>(), scheduler)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(expectedException);

        assertEquals(10, count.get());
    }

    /**
     * test jittered delay method return the correct delay time.
     */
    @Test
    public void testJitteredDelaySec() {
        //arrange
        RetryHandler subject = new RetryHandler(0, Integer.MAX_VALUE);
        //act
        long delay = subject.jitteredDelayMillis(2);
        //assert
        assertEquals(Duration.ofSeconds(4).toMillis(), delay);
    }

    /**
     * test jittered delay method return no more than the max delay time.
     */
    @Test
    public void testJitteredDelaySecReturnsNoMoreThanMaxValue() {
        //arrange
        long maxDelayMs = Duration.ofSeconds(4).toMillis();
        RetryHandler subject = new RetryHandler(0, maxDelayMs);
        //act
        long delay = subject.jitteredDelayMillis(2);
        //assert
        assertEquals(maxDelayMs, delay);
    }

    /**
     * test jittered delay method returns powers of 2 when there's no jitter.
     */
    @Test
    public void testExponentialDelaysNoJitter() {
        int jitterFactor = 0;

        //arrange
        RetryHandler subject = new RetryHandler(jitterFactor, Integer.MAX_VALUE);

        IntStream.rangeClosed(0, 10).forEach(attempt -> {
            //act
            long delay = subject.jitteredDelayMillis(attempt);

            //assert
            assertEquals(Duration.ofSeconds((long) Math.pow(2, attempt)).toMillis(), delay, jitterFactor);
        });
    }

    /**
     * test jittered delay method returns powers of 2 plus a random amount of miliseconds between 0 and jitterFactor.
     */
    @Test
    public void testExponentialDelaysWithJitterIsWithinDelta() {
        int jitterFactor = 100;

        //arrange
        RetryHandler subject = new RetryHandler(jitterFactor, Integer.MAX_VALUE);

        IntStream.rangeClosed(0, 10).forEach(attempt -> {
            //act
            long delay = subject.jitteredDelayMillis(attempt);

            //assert
            assertEquals(Duration.ofSeconds((long) Math.pow(2, attempt)).toMillis(), delay, jitterFactor);
        });
    }

}
