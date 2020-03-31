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

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.async.Cancelable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

/**
 * Utility method to convert between behaviors that use the Amplify framework's native
 * {@link Consumer} and {@link Action}, into {@link Single} and {@link Observable}.
 */
final class RxAdapters {
    private RxAdapters() {}

    static <T, E extends Throwable> Completable toCompletable(VoidResultEmitter<T, E> voidResultEmitter) {
        return Completable.defer(() -> Completable.create(emitter ->
            voidResultEmitter.emitTo(result -> emitter.onComplete(), emitter::onError)
        ));
    }

    static <T, E extends Throwable> Single<T> toSingle(CancelableResultEmitter<T, E> cancelableResultEmitter) {
        return Single.defer(() -> Single.create(emitter -> {
            final CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            final Cancelable cancelable =
                cancelableResultEmitter.emitTo(emitter::onSuccess, emitter::onError);
            disposable.add(Disposables.fromAction(() -> {
                if (cancelable != null) {
                    cancelable.cancel();
                }
            }));
        }));
    }

    static <S, T, E extends Throwable> Observable<T> toObservable(
            CancelableStreamEmitter<S, T, E> cancelableStreamEmitter) {
        return Observable.defer(() -> Observable.create(emitter -> {
            final CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            final Cancelable cancelable = cancelableStreamEmitter.streamTo(
                NoOpConsumer.create(),
                emitter::onNext,
                emitter::onError,
                emitter::onComplete
            );
            disposable.add(Disposables.fromAction(() -> {
                if (cancelable != null) {
                    cancelable.cancel();
                }
            }));
        }));
    }

    /**
     * A behavior which emits to a result listener, but returns no value, itself.
     */
    interface VoidResultEmitter<T, E extends Throwable> {
        void emitTo(Consumer<T> onResult, Consumer<E> onError);
    }

    /**
     * Describes a behavior which emits results to a result or error {@link Consumer},
     * and can be canceled via an {@link Cancelable}. Such a method
     * may be wrapped into an {@link Single} in a uniform way such as by the
     * {@link RxAdapters#toSingle(CancelableResultEmitter)} method.
     * @param <T> Type of result accepted by result consumer
     * @param <E> Type of error accepted by error consumer
     */
    interface CancelableResultEmitter<T, E extends Throwable> {
        Cancelable emitTo(Consumer<T> onResult, Consumer<E> onError);
    }

    /**
     * Describes a behavior emits a notification of start to an {@link Consumer},
     * then emits 0..n values to a value {@link Consumer}, and finally terminated
     * either by calling a successful {@link Action}, or emitting an error to an
     * error {@link Consumer}. May be canceled via a returned {@link Cancelable}.
     * Such a method may be wrapped into an {@link Observable} in a uniform way such as
     * by the {@link RxAdapters#toObservable(CancelableStreamEmitter)} method.
     * @param <S> Type emitted to the start consumer
     * @param <T> Type of object being emitted to the value consumer
     * @param <E> Type emitted to error consumer
     */
    interface CancelableStreamEmitter<S, T, E> {
        Cancelable streamTo(Consumer<S> onStart, Consumer<T> onItem, Consumer<E> onError, Action onComplete);
    }
}
