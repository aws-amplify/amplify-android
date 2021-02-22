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
import com.amplifyframework.rx.RxAdapters.CancelableBehaviors;
import com.amplifyframework.rx.RxAdapters.VoidBehaviors;
import com.amplifyframework.testutils.SimpleCancelable;
import com.amplifyframework.testutils.Sleep;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.observers.TestObserver;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link RxAdapters} utility methods.
 */
public final class RxAdaptersTest {
    /**
     * The {@link Completable} returned by
     * {@link VoidBehaviors#toCompletable(VoidBehaviors.ActionEmitter)}
     * will complete when {@link VoidBehaviors.ActionEmitter}'s acton call is invoked.
     */
    @Test
    public void completableFiresOnCompleteWhenResultEmitted() {
        VoidBehaviors
            .toCompletable((onResult, onError) -> onResult.call())
            .test()
            .assertComplete()
            .assertNoErrors();
    }

    /**
     * The {@link Completable} returned by
     * {@link VoidBehaviors#toCompletable(VoidBehaviors.ActionEmitter)}
     * will dispatch an error when {@link VoidBehaviors.ActionEmitter}'s
     * error consumer is invoked.
     */
    @Test
    public void completableFiresErrorWhenErrorEmitted() {
        Throwable expected = new Throwable(RandomString.string());
        VoidBehaviors
            .toCompletable((onResult, onError) -> onError.accept(expected))
            .test()
            .assertError(expected)
            .assertNotComplete();
    }

    /**
     * The {@link Completable} returned by
     * {@link VoidBehaviors#toCompletable(VoidBehaviors.ActionEmitter)}
     * is cancelable.
     */
    @SuppressWarnings("checkstyle:WhitespaceAround") // No-op VoidResultEmitter body
    @Test
    public void completableIsDisposable() {
        Completable completable = VoidBehaviors.toCompletable(((onResult, onError) -> {}));
        TestObserver<?> observer = completable.test();
        observer.dispose();
        assertTrue(observer.isDisposed());
    }

    /**
     * The {@link Single} returned by
     * {@link CancelableBehaviors#toSingle(CancelableBehaviors.ResultEmitter)}
     * will dispatch an error when the {@link CancelableBehaviors.ResultEmitter}'s error consumer is
     * invoked.
     */
    @Test
    public void singleFiresErrorWhenErrorEmitted() {
        Throwable expected = new Throwable(RandomString.string());
        CancelableBehaviors
            .toSingle((onResult, onError) -> {
                onError.accept(expected);
                return new NoOpCancelable();
            })
            .test()
            .assertError(expected)
            .assertNoValues();
    }

    /**
     * The {@link Single} returned by
     * {@link CancelableBehaviors#toSingle(CancelableBehaviors.ResultEmitter)}
     * will dispatch a result when the {@link CancelableBehaviors.ResultEmitter}'s value consumer
     * is invoked.
     */
    @Test
    public void singleFiresResultWhenEmitted() {
        String result = RandomString.string();
        CancelableBehaviors
            .toSingle((onResult, onError) -> {
                onResult.accept(result);
                return new NoOpCancelable();
            })
            .test()
            .assertValue(result)
            .assertComplete();
    }

    /**
     * {@link CancelableBehaviors#toSingle(CancelableBehaviors.ResultEmitter)}
     * returns a {@link Single} which can be canceled. The Amplify {@link Cancelable}
     * returned by that emitter will also show as canceled.
     */
    @Test
    public void underlyingOperationIsCanceledWhenSingleSubscriptionIsDisposed() {
        SimpleCancelable cancelable = new SimpleCancelable();
        TestObserver<?> observer =
            CancelableBehaviors.toSingle(((onResult, onError) -> cancelable)).test();
        observer.dispose();
        assertTrue(observer.isDisposed());
        assertTrue(cancelable.isCanceled());
    }

    /**
     * The {@link Observable} returned by
     * {@link CancelableBehaviors#toObservable(CancelableBehaviors.StreamEmitter)}
     * will contain a stream of values corresponding to those that have been passed via the emitter's
     * item consumer. When the emitters' completion action is invoked, the Observable completes.
     */
    @Test
    public void observableFiresValuesAndCompletesWhenEmitterDoes() {
        String first = RandomString.string();
        String second = RandomString.string();
        CancelableBehaviors
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
     * The {@link Observable} returned by
     * {@link CancelableBehaviors#toObservable(CancelableBehaviors.StreamEmitter)}
     * terminates with an error if the emitters' error Consumer is called.
     */
    @Test
    public void observableFiresErrorWhenErrorEmitted() {
        RuntimeException expected = new RuntimeException(RandomString.string());
        CancelableBehaviors
            .toObservable(((onStart, onItem, onError, onComplete) -> {
                throw expected;
            }))
            .test()
            .assertError(expected)
            .assertNoValues()
            .assertNotComplete();
    }

    /**
     * The {@link Observable} returned by
     * {@link CancelableBehaviors#toObservable(CancelableBehaviors.StreamEmitter)}
     * is cancelable.
     */
    @Test
    public void underlyingCancelIsCalledWhenObservableSubscriptionIsDisposed() {
        SimpleCancelable cancelable = new SimpleCancelable();
        TestObserver<?> observer =
            CancelableBehaviors.toObservable((onStart, onItem, onError, onComplete) -> cancelable)
                .test();
        observer.dispose();
        assertTrue(observer.isDisposed());
        assertTrue(cancelable.isCanceled());
    }

    /**
     * If a subscriber has been disposed, then the Rx actor should
     * not try to fire an error on it. Otherwise, it would cause an
     * {@link UndeliverableException}.
     *
     * Theoretically there are six of these to test (Void and Cancelable each
     * have adapters for Single, Completable, and Observable.) Let's just test it
     * once and assume the others work the same way.
     */
    @Test
    public void doesNotFireErrorWhenDisposed() {
        Completable completable = VoidBehaviors.toCompletable((onResult, onError) ->
            new Thread(() -> {
                Sleep.milliseconds(75);
                onError.accept(new Throwable(RandomString.string()));
            }).start()
        );
        Disposable disposable = completable.subscribe();
        disposable.dispose();
        Sleep.milliseconds(150);
    }
}
