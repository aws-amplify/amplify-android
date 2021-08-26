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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;

/**
 * Class for retrying call on failure on a single.
 */
public class RetryHandler {

    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final int MAX_EXPONENT_VALUE = 8;
    private static final int JITTER_FACTOR_VALUE = 100;
    private static final int MAX_ATTEMPTS_VALUE = 3;
    private static final int MAX_DELAY_S_VALUE = 5 * 60;
    private final int maxExponent;
    private final int jitterFactor;
    private final int maxAttempts;
    private final int maxDelayS;

    /**
     * Constructor to inject constants for unit testing.
     * @param maxExponent maxExponent backoff can go to.
     * @param jitterFactor jitterFactor for backoff.
     * @param maxAttempts max attempt for retrying.
     * @param maxDelayS max delay for retrying.
     */
    public RetryHandler(int maxExponent,
                        int jitterFactor,
                        int maxAttempts,
                        int maxDelayS) {

        this.maxExponent = maxExponent;
        this.jitterFactor = jitterFactor;
        this.maxAttempts = maxAttempts;
        this.maxDelayS = maxDelayS;
    }

    /**
     * Parameter less constructor.
     */
    public RetryHandler() {
        maxExponent = MAX_EXPONENT_VALUE;
        jitterFactor = JITTER_FACTOR_VALUE;
        maxAttempts = MAX_ATTEMPTS_VALUE;
        maxDelayS = MAX_DELAY_S_VALUE;
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
            Long delayInSeconds,
            int attemptsLeft,
            List<Class<? extends Throwable>> skipExceptions) {
        single.delaySubscription(delayInSeconds, TimeUnit.SECONDS)
                .subscribe(emitter::onSuccess, error -> {
                    if (!emitter.isDisposed()) {
                        LOG.verbose("Retry attempts left " + attemptsLeft + ". exception type:" + error.getClass());
                        if (attemptsLeft == 0 || ErrorInspector.contains(error, skipExceptions)) {
                            emitter.onError(error);
                        } else {
                            call(single, emitter, jitteredDelaySec(attemptsLeft),
                                    attemptsLeft - 1, skipExceptions);
                        }
                    } else {
                        LOG.verbose("The subscribing channel is disposed.");
                    }
                });
    }


    /**
     * Method returns jittered delay time in seconds.
     * @param attemptsLeft number of attempts left.
     * @return delay in seconds.
     */
    long jitteredDelaySec(int attemptsLeft) {
        int numAttempt = maxAttempts - (maxAttempts - attemptsLeft);
        double waitTimeSeconds =
                Math.min(maxDelayS, Math.pow(2, ((numAttempt) % maxExponent))
                        + jitterFactor * Math.random());
        LOG.debug("Wait time is " + waitTimeSeconds + " seconds before retrying");
        return (long) waitTimeSeconds;
    }
}
