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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;

/**
 * Class that defines inner classes and interfaces related to retry strategies.
 */
public final class RetryStrategy {
    /**
     * Simple defaults strategy that allows interruptions and has a max delay of 2^8.
     */
    public static final RxRetryStrategy RX_INTERRUPTIBLE_WITH_BACKOFF =
        new RxCompletableExponentialBackoffStrategy(2, 8, Arrays.asList(InterruptedException.class));

    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    private RetryStrategy() {}

    /**
     * Simple implementation of an exponential backoff startegy that can be used with RxCompletables.
     */
    static final class RxCompletableExponentialBackoffStrategy implements RxRetryStrategy {
        private final int waitBaseDelay;
        private final int maxExponent;
        private final List<Class<? extends Throwable>> skipExceptionTypes;

        /**
         * Single constructor for this class.
         * @param waitBaseDelay the base of the exponential expression.
         * @param maxExponent the maxExponent allowed. We'll use this to do a mod against the attempt
         *                    number being passed in.
         * @param skipExceptionTypes a list of exception types for which we don't want to retry.
         */
        RxCompletableExponentialBackoffStrategy(int waitBaseDelay,
                                                int maxExponent,
                                                List<Class<? extends Throwable>> skipExceptionTypes) {
            this.waitBaseDelay = waitBaseDelay;
            this.maxExponent = maxExponent;
            this.skipExceptionTypes = skipExceptionTypes;
        }

        @Override
        public boolean retryHandler(int attemptNumber, Throwable throwable) {
            LOG.verbose("Should retry? attempt number:" + attemptNumber + " exception type:" + throwable.getClass());
            if (skipExceptionTypes.contains(throwable.getClass())) {
                // If it's part of the skip list, don't retry.
                return false;
            } else {
                final long waitTimeSeconds = Double.valueOf(Math.pow(2, attemptNumber % maxExponent)).longValue();
                LOG.debug("Waiting " + waitTimeSeconds + " seconds before retrying");
                Completable.timer(TimeUnit.SECONDS.toMillis(waitTimeSeconds), TimeUnit.MILLISECONDS).blockingAwait();
                return true;
            }
        }
    }

    interface RxRetryStrategy {
        boolean retryHandler(int attemptNumber, Throwable throwable);
    }
}
