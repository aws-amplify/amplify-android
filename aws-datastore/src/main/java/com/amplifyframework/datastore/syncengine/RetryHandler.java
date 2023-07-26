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
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.utils.ErrorInspector;
import com.amplifyframework.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Class for retrying call on failure on a single.
 */
public class RetryHandler {

    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private static final long JITTER_MS_VALUE = 100;
    @SuppressWarnings("checkstyle:magicnumber")
    private static final long MAX_DELAY_MS_VALUE = Duration.ofMinutes(5).toMillis();
    private final long jitterMs;
    private final long maxDelayMs;

    /**
     * Constructor to inject constants for unit testing.
     *
     * @param jitterMs   jitterMs for backoff.
     * @param maxDelayMs max delay for retrying.
     */
    public RetryHandler(long jitterMs,
                        long maxDelayMs) {

        this.jitterMs = jitterMs;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Parameter less constructor.
     */
    public RetryHandler() {
        jitterMs = JITTER_MS_VALUE;
        maxDelayMs = MAX_DELAY_MS_VALUE;
    }

    /**
     * Creates a {@link Single} that wraps a given one to make it retryable.
     *
     * @param single                 single to be retried.
     * @param nonRetryableExceptions exceptions which should not be retried.
     * @param <T>                    The type for single.
     * @return single of type T.
     */
    public <T> Single<T> retry(Single<T> single, List<Class<? extends Throwable>> nonRetryableExceptions) {
        return retry(single, nonRetryableExceptions, Schedulers.computation());
    }

    /**
     * Creates a {@link Single} that wraps a given one to make it retryable.
     *
     * @param single                 single to be retried.
     * @param nonRetryableExceptions exceptions which should not be retried.
     * @param scheduler              Scheduler to run the timer, useful for Unit Testing.
     * @param <T>                    The type for single.
     * @return A new single with retries support.
     */
    protected <T> Single<T> retry(Single<T> single, List<Class<? extends Throwable>> nonRetryableExceptions,
                                  Scheduler scheduler) {

        AtomicInteger numAttempt = new AtomicInteger();
        AtomicBoolean isInProgress = new AtomicBoolean();

        Observable<T> observable = Observable.fromSingle(single);

        Observable<T> retryableObservable = observable.doOnSubscribe(ignore -> {
            LOG.info("Starting attempt #" + (numAttempt.get() + 1));
        }).doOnNext(ignore -> {
            isInProgress.set(true);
            LOG.info("Success on attempt #" + (numAttempt.get() + 1));
        }).retryWhen(errors -> {
            return errors.flatMap(error -> {
                if (!ErrorInspector.contains(error, nonRetryableExceptions)) {
                    long delay = jitteredDelayMillis(numAttempt.get());

                    LOG.warn("Attempt #" + (numAttempt.get() + 1) + " failed.", error);

                    if (delay > maxDelayMs) {
                        LOG.warn("No more attempts left.");
                        return Observable.error(error);
                    }

                    return Observable.timer(delay, TimeUnit.MILLISECONDS, scheduler).doOnSubscribe(ignore -> {
                        LOG.debug("Retrying in " + delay + " milliseconds.");

                        numAttempt.getAndIncrement();
                    });
                }

                LOG.warn("Non-retryable exception.", error);
                return Observable.error(error);
            });
        }).doOnDispose(() -> {
            if (!isInProgress.get()) {
                LOG.info("The subscribing channel is disposed, canceling retries.");
            }
        });

        return retryableObservable.firstOrError();
    }


    /**
     * Method returns a jittered 2^numAttempt delay time in milliseconds.
     *
     * @param numAttempt Attempt number.
     * @return delay in milliseconds.
     */
    long jitteredDelayMillis(int numAttempt) {
        return (long) (Duration.ofSeconds((long) Math.pow(2, numAttempt)).toMillis() + (jitterMs * Math.random()));
    }

}
