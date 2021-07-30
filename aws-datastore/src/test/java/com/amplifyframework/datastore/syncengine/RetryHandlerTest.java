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

import com.amplifyframework.datastore.DataStoreException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;

public class RetryHandlerTest {

    /**
     * Test no retry on success.
     */
    @Test
    public void testNoRetryOnSuccess() {
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

    /**
     * Test no retry on Irrecoverable error.
     */
    @Test
    public void testNoRetryOnIrrecoverableError() {
        //arrange
        RetryHandler subject = new RetryHandler();
        DataStoreException expectedException = new DataStoreException.
                GraphQLResponseException("PaginatedResult<ModelWithMetadata<BlogOwner>>",
                new ArrayList<>());
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

    /**
     * Test retry on recoverable error.
     */
    @Test
    public void testRetryOnRecoverableError() {
        //arrange
        RetryHandler subject = new RetryHandler(8, 0, 1);
        DataStoreException expectedException =
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "");
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

    /**
     * test jittered delay method return the correct delay time.
     */
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
