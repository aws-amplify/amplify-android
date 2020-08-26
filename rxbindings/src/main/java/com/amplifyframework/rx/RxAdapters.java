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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.async.Cancelable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;

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
            final Cancelable cancelable =
                cancelableResultEmitter.emitTo(emitter::onSuccess, emitter::onError);
            emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
        }));
    }

    static <S, T, E extends Throwable> Observable<T> toObservable(
            CancelableStreamEmitter<S, T, E> cancelableStreamEmitter) {
        return Observable.defer(() -> Observable.create(emitter -> {
            Cancelable cancelable = cancelableStreamEmitter.streamTo(
                NoOpConsumer.create(),
                emitter::onNext,
                emitter::onError,
                emitter::onComplete
            );
            emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
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

    /**
     * Describes behavior which emits a completion notification via an {@link Action},
     * or alternately, emits an error to a {@link Consumer}.
     * @param <E> Type of error emitted
     */
    interface VoidCompletionEmitter<E> {
        void emitTo(Action onComplete, Consumer<E> onError);
    }

    /**
     * Interface that defines a contract for callback-style operations to be
     * mapped to reactive-style operation types that offer a more idiomatic way
     * of using reactive programming with the Amplify framework.
     * @param <T> The type that represents the result of a given operation.
     * @param <P> The type that represents the progress of a given operation.
     * @param <E> The type of exception of a given operation.
     */
    interface RxProgressAwareCallbackMapper<T, P, E> {
        Cancelable emitTo(Consumer<P> onProgress, Consumer<T> onItem, Consumer<E> onError);
    }

    /**
     * Interface that should be implemented by reactive-style operations
     * wishing to return a {@link Single} as its result.
     * @param <T> The type that represents the result of a given operation.
     */
    interface RxSingleOperation<T> extends Cancelable {
        /**
         * Maps the result of a callback-style operation to a {@link Single}.
         * @return A {@link Single} that emits a result or an error.
         */
        Single<T> observeResult();
    }

    /**
     * Interface that should be implemented by reactive-style operations
     * that emit progress information during processing.
     * @param <P> The type that represents the progress of a given operation.
     */
    interface RxProgressAwareOperation<P> {
        /**
         * Returns an observable that the caller can subscribe to
         * receive progress information about the operation.
         * @return An observable that emits progress information.
         */
        Observable<P> observeProgress();
    }

    /**
     * A generic implementation of an operation that emits
     * progress information and returns a single.
     * @param <T> The type that represents the result of a given operation.
     * @param <P> The type that represents the progress of a given operation.
     */
    static class RxProgressAwareSingle<T, P>
        implements RxAdapters.RxSingleOperation<T>, RxProgressAwareOperation<P> {

        private PublishSubject<P> progressSubject;
        private ReplaySubject<T> resultSubject;
        private Cancelable amplifyOperation;

        RxProgressAwareSingle(RxProgressAwareCallbackMapper<T, P, ? extends AmplifyException> callbacks) {
            progressSubject = PublishSubject.create();
            resultSubject = ReplaySubject.create();
            amplifyOperation = callbacks.emitTo(progressSubject::onNext,
                                                resultSubject::onNext,
                                                resultSubject::onError);
        }

        @Override
        public void cancel() {
            amplifyOperation.cancel();
            resultSubject.onComplete();
            progressSubject.onComplete();
        }

        @Override
        public Single<T> observeResult() {
            Single<T> objectSingle = Single.create(emitter -> {
                resultSubject.subscribe(emitter::onSuccess, emitter::onError);
            });
            return objectSingle;
        }

        @Override
        public Observable<P> observeProgress() {
            return progressSubject;
        }
    }
}
