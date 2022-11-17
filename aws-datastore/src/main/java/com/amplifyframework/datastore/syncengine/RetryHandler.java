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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.utils.ErrorInspector;
import com.amplifyframework.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;

/**
 * Class for retrying call on failure on a single.
 */
public class RetryHandler {

    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long JITTER_MS_VALUE = 100;
    private static final int MAX_ATTEMPTS_VALUE = 3;
    @SuppressWarnings("checkstyle:magicnumber")
    private static final long MAX_DELAY_MS_VALUE = Duration.ofMinutes(5).toMillis();
    private final long jitterMs;
    private final int maxAttempts;
    private final long maxDelayMs;

    /**
     * Constructor to inject constants for unit testing.
     * @param jitterMs jitterMs for backoff.
     * @param maxAttempts max attempt for retrying.
     * @param maxDelayMs max delay for retrying.
     */
    public RetryHandler(long jitterMs,
                        int maxAttempts,
                        long maxDelayMs) {

        this.jitterMs = jitterMs;
        this.maxAttempts = maxAttempts;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Parameter less constructor.
     */
    public RetryHandler() {
        jitterMs = JITTER_MS_VALUE;
        maxAttempts = MAX_ATTEMPTS_VALUE;
        maxDelayMs = MAX_DELAY_MS_VALUE;
    }

    /**
     * retry.
     * @param single single to be retried.
     * @param skipExceptions exceptions which should not be retried.
     * @param <T> The type for single.
     * @return single of type T.
     */
    public <T> Single<T> retry(Single<T> single, List<Class<? extends Throwable>> skipExceptions) {
        return Single.create(emitter -> call(single, emitter, 0L, maxAttempts, skipExceptions));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private <T> void call(
            Single<T> single,
            SingleEmitter<T> emitter,
            long delayInMilliseconds,
            int attemptsLeft,
            List<Class<? extends Throwable>> skipExceptions) {
        single.delaySubscription(delayInMilliseconds, TimeUnit.SECONDS)
                .subscribe(emitter::onSuccess, error -> {
                    if (!emitter.isDisposed()) {
                        LOG.verbose("Retry attempts left " + attemptsLeft + ". exception type:" + error.getClass());
                        if (attemptsLeft == 0 || ErrorInspector.contains(error, skipExceptions)) {
                            emitter.onError(error);
                        } else {
                            call(single, emitter, jitteredDelayMillis(attemptsLeft),
                                    attemptsLeft - 1, skipExceptions);
                        }
                    } else {
                        LOG.verbose("The subscribing channel is disposed.");
                    }
                });
    }


    /**
     * Method returns jittered delay time in milliseconds.
     *
     * @param attemptsLeft number of attempts left.
     * @return delay in milliseconds.
     */
    long jitteredDelayMillis(int attemptsLeft) {
        int numAttempt = maxAttempts - (maxAttempts - attemptsLeft);

        long waitTimeMilliseconds = (long) Math.min(
                maxDelayMs,
                Duration.ofSeconds((long) Math.pow(2, numAttempt)).toMillis() + (jitterMs * Math.random())
        );

        LOG.debug("Wait time is " + waitTimeMilliseconds + " milliseconds before retrying");

        return waitTimeMilliseconds;
    }
}
