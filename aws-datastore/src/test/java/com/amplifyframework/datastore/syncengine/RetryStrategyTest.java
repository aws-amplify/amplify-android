/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetryStrategyTest {

    /**
     * Retry handler should return true (meaning will retry)
     * if the exception is not an {@link InterruptedException}.
     */
    @Test
    public void retryOnRetryableException() {
        boolean shouldRetry =
            RetryStrategy.RX_INTERRUPTIBLE_WITH_BACKOFF.retryHandler(1, new TimeoutException("Try again..."));
        assertTrue(shouldRetry);
    }

    /**
     * Retry handler should return false (meaning will not retry)
     * if the exception is an {@link InterruptedException}.
     */
    @Test
    public void dontRetryOnInterruptedException() {
        boolean shouldRetry =
            RetryStrategy.RX_INTERRUPTIBLE_WITH_BACKOFF.retryHandler(1, new InterruptedException("Try again..."));
        assertFalse(shouldRetry);
    }
}
