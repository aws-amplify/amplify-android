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

package com.amplifyframework.rx;

import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.rx.RxAdapters.CancelableResultEmitter;
import com.amplifyframework.rx.RxAdapters.CancelableStreamEmitter;
import com.amplifyframework.rx.RxAdapters.VoidResultEmitter;
import com.amplifyframework.testutils.SimpleCancelable;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link RxAdapters} utility methods.
 */
public final class RxAdaptersTest {
    /**
     * The {@link Completable} returned by {@link RxAdapters#toCompletable(VoidResultEmitter)}
     * will complete when {@link VoidResultEmitter}'s result consumer is invoked.
     */
    @Test
    public void completableFiresOnCompleteWhenResultEmitted() {
        RxAdapters
            .toCompletable((onResult, onError) -> onResult.accept(RandomString.string()))
            .test()
            .assertComplete()
            .assertNoErrors();
    }

    /**
     * The {@link Completable} returned by {@link RxAdapters#toCompletable(VoidResultEmitter)}
     * will dispatch an error when {@link VoidResultEmitter}'s error consumer is invoked.
     */
    @Test
    public void completableFiresErrorWhenErrorEmitted() {
        Throwable expected = new Throwable(RandomString.string());
        RxAdapters
            .toCompletable((onResult, onError) -> onError.accept(expected))
            .test()
            .assertError(expected)
            .assertNotComplete();
    }

    /**
     * The {@link Completable} returned by {@link RxAdapters#toCompletable(VoidResultEmitter)}
     * is cancelable.
     */
    @SuppressWarnings("checkstyle:WhitespaceAround") // No-op VoidResultEmitter body
    @Test
    public void completableIsCancellable() {
        Completable completable = RxAdapters.toCompletable(((onResult, onError) -> {}));
        TestObserver<?> observer = completable.test();
        observer.cancel();
        assertTrue(observer.isCancelled());
    }

    /**
     * The {@link Single} returned by {@link RxAdapters#toSingle(CancelableResultEmitter)}
     * will dispatch an error when the {@link CancelableResultEmitter}'s error consumer is
     * invoked.
     */
    @Test
    public void singleFiresErrorWhenErrorEmitted() {
        Throwable expected = new Throwable(RandomString.string());
        RxAdapters
            .toSingle((onResult, onError) -> {
                onError.accept(expected);
                return new NoOpCancelable();
            })
            .test()
            .assertError(expected)
            .assertNoValues();
    }

    /**
     * The {@link Single} returned by {@link RxAdapters#toSingle(CancelableResultEmitter)}
     * will dispatch a result when the {@link CancelableResultEmitter}'s value consumer
     * is invoked.
     */
    @Test
    public void singleFiresResultWhenEmitted() {
        String result = RandomString.string();
        RxAdapters
            .toSingle((onResult, onError) -> {
                onResult.accept(result);
                return new NoOpCancelable();
            })
            .test()
            .assertValue(result)
            .assertComplete();
    }

    /**
     * {@link RxAdapters#toSingle(CancelableResultEmitter)} returns a {@link Single} which
     * can be canceled. The Amplify {@link Cancelable} returned by that emitter will also show
     * as canceled.
     */
    @Test
    public void singleIsCancelable() {
        SimpleCancelable cancelable = new SimpleCancelable();
        TestObserver<?> observer = RxAdapters.toSingle(((onResult, onError) -> cancelable))
            .test();
        observer.cancel();
        assertTrue(observer.isCancelled());
        assertTrue(cancelable.isCanceled());
    }

    /**
     * The {@link Observable} returned by {@link RxAdapters#toObservable(CancelableStreamEmitter)}
     * will contain a stream of values corresponding to those that have been passed via the emitter's
     * item consumer. When the emitters' completion action is invoked, the Observable completes.
     */
    @Test
    public void observableFiresValuesAndCompletesWhenEmitterDoes() {
        String first = RandomString.string();
        String second = RandomString.string();
        RxAdapters
            .toObservable((onStart, onItem, onError, onComplete) -> {
                onStart.accept(RandomString.string());
                onItem.accept(first);
                onItem.accept(second);
                onComplete.call();
                return new NoOpCancelable();
            })
            .test()
            .assertValues(first, second)
            .assertComplete()
            .assertNoErrors();
    }

    /**
     * The {@link Observable} returned by {@link RxAdapters#toObservable(CancelableStreamEmitter)}
     * terminates with an error if the emitters' error Consumer is called.
     */
    @Test
    public void observableFiresErrorWhenErrorEmitted() {
        RuntimeException expected = new RuntimeException(RandomString.string());
        RxAdapters
            .toObservable(((onStart, onItem, onError, onComplete) -> {
                throw expected;
            }))
            .test()
            .assertError(expected)
            .assertNoValues()
            .assertNotComplete()
            .assertTerminated();
    }

    /**
     * The {@link Observable} returned by {@link RxAdapters#toObservable(CancelableStreamEmitter)}
     * is cancelable.
     */
    @Test
    public void observableIsCancelable() {
        SimpleCancelable cancelable = new SimpleCancelable();
        TestObserver<?> observer =
            RxAdapters.toObservable((onStart, onItem, onError, onComplete) -> cancelable)
                .test();
        observer.cancel();
        assertTrue(observer.isCancelled());
        assertTrue(cancelable.isCanceled());
    }
}
