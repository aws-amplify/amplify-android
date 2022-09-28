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

import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreErrorHandler;
import com.amplifyframework.datastore.DataStoreException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryHandlerTest {

    /**
     * Test no retry on success.
     */
    @Test
    public void testNoRetryOnSuccess() throws DataStoreException {
        //arrange
        DataStoreConfigurationProvider configurationProvider = mock(DataStoreConfigurationProvider.class);
        DataStoreConfiguration config = mock(DataStoreConfiguration.class);
        when(configurationProvider.getConfiguration())
                .thenReturn(config);
        when(config.getErrorHandler())
                .thenReturn(mock(DataStoreErrorHandler.class));
        RetryHandler subject =
                new RetryHandler(configurationProvider);
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
    public void testNoRetryOnIrrecoverableError() throws DataStoreException {
        //arrange
        DataStoreConfigurationProvider configurationProvider = mock(DataStoreConfigurationProvider.class);
        DataStoreConfiguration config = mock(DataStoreConfiguration.class);
        when(configurationProvider.getConfiguration())
                .thenReturn(config);
        DataStoreErrorHandler errorHandler = mock(DataStoreErrorHandler.class);
        when(config.getErrorHandler())
                .thenReturn(errorHandler);
        RetryHandler subject = new RetryHandler(configurationProvider);
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
        verify(errorHandler, times(1)).accept(any());
    }

    /**
     * Test retry on recoverable error.
     */
    @Test
    public void testRetryOnRecoverableError() throws DataStoreException {
        //arrange
        DataStoreConfigurationProvider configurationProvider = mock(DataStoreConfigurationProvider.class);
        DataStoreConfiguration config = mock(DataStoreConfiguration.class);
        when(configurationProvider.getConfiguration())
                .thenReturn(config);
        when(config.getErrorHandler())
                .thenReturn(mock(DataStoreErrorHandler.class));
        RetryHandler subject = new RetryHandler(8, 0, 1,
                1, configurationProvider);
        DataStoreException expectedException =
                new DataStoreException("PaginatedResult<ModelWithMetadata<BlogOwner>>", "");
        AtomicInteger count = new AtomicInteger(0);

        Single<Object> mockSingle = Single.error(expectedException)
                .doOnError(e -> count.incrementAndGet());

        //act and assert
        subject.retry(mockSingle, new ArrayList<>())
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .isDisposed();
    }

    /**
     * test jittered delay method return the correct delay time.
     */
    @Test
    public void testJitteredDelaySec() throws DataStoreException {
        //arrange
        DataStoreConfigurationProvider configurationProvider = mock(DataStoreConfigurationProvider.class);
        DataStoreConfiguration config = mock(DataStoreConfiguration.class);
        when(configurationProvider.getConfiguration())
                .thenReturn(config);
        when(config.getErrorHandler())
                .thenReturn(mock(DataStoreErrorHandler.class));
        RetryHandler subject = new RetryHandler(8, 0, 1,
                5, configurationProvider );
        //act
        long delay = subject.jitteredDelaySec(2);
        //assert
        assertEquals(4, delay);
    }

    /**
     * test jittered delay method return no more than the max delay time.
     */
    @Test
    public void testJitteredDelaySecReturnsNoMoreThanMaxValue() throws DataStoreException {
        //arrange
        DataStoreConfigurationProvider configurationProvider = mock(DataStoreConfigurationProvider.class);
        DataStoreConfiguration config = mock(DataStoreConfiguration.class);
        when(configurationProvider.getConfiguration())
                .thenReturn(config);
        when(config.getErrorHandler())
                .thenReturn(mock(DataStoreErrorHandler.class));
        RetryHandler subject = new RetryHandler(8, 0, 1,
                1, configurationProvider);
        //act
        long delay = subject.jitteredDelaySec(2);
        //assert
        assertEquals(1, delay);
    }

}
