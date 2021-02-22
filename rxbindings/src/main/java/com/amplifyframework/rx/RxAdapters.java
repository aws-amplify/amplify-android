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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Utilities for modeling Amplify category behaviors, and converting
 * those category behaviors into Rx objects (Observable, Single, Completable).
 */
final class RxAdapters {
    private RxAdapters() {}

    /**
     * Cancelable behaviors are those Amplify category behaviors which return a cancelable
     * operation. For example, most behaviors in Storage and Predictions will return
     * a cancelable operation, whereas DataStore and Auth do not.
     */
    static final class CancelableBehaviors {
        private CancelableBehaviors() {}

        static <E extends Throwable> Completable toCompletable(ActionEmitter<E> behavior) {
            return Completable.create(subscriber -> {
                Cancelable cancelable = behavior.emitTo(subscriber::onComplete, subscriber::tryOnError);
                subscriber.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
            });
        }

        static <T, E extends Throwable> Single<T> toSingle(ResultEmitter<T, E> behavior) {
            return Single.create(subscriber -> {
                Cancelable cancelable = behavior.emitTo(subscriber::onSuccess, subscriber::tryOnError);
                subscriber.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
            });
        }

        static <S, T, E extends Throwable> Observable<T> toObservable(StreamEmitter<S, T, E> behavior) {
            return Observable.create(subscriber -> {
                Cancelable cancelable = behavior.streamTo(
                    NoOpConsumer.create(),
                    subscriber::onNext,
                    subscriber::tryOnError,
                    subscriber::onComplete
                );
                subscriber.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
            });
        }

        /**
         * Describes a behavior which emits a notification of start to an {@link Consumer},
         * then emits 0..n values to a value {@link Consumer}, and finally terminated
         * either by calling a successful {@link Action}, or emitting an error to an
         * error {@link Consumer}. May be canceled via a returned {@link Cancelable}.
         * Such a method may be wrapped into an {@link Observable}
         * by using the {@link CancelableBehaviors#toObservable(StreamEmitter)} utility.
         * @param <S> Type emitted to the start consumer
         * @param <T> Type of object being emitted to the value consumer
         * @param <E> Type emitted to error consumer
         */
        interface StreamEmitter<S, T, E> {
            Cancelable streamTo(Consumer<S> onStart, Consumer<T> onItem, Consumer<E> onError, Action onComplete);
        }

        /**
         * Describes a behavior which emits results to a result or error {@link Consumer},
         * and can be canceled via an {@link Cancelable}. Such a method
         * may be wrapped into an {@link Single} in a uniform way by using the
         * {@link CancelableBehaviors#toSingle(ResultEmitter)} utility.
         * @param <T> Type of result accepted by result consumer
         * @param <E> Type of error accepted by error consumer
         */
        interface ResultEmitter<T, E extends Throwable> {
            Cancelable emitTo(Consumer<T> onResult, Consumer<E> onError);
        }

        /**
         * A behavior which terminates in a completion action or an error.
         * Returns a cancelable when the behavior starts. Such a method
         * may be wrapped into an {@link Completable} in a uniform way
         * by using the {@link CancelableBehaviors#toCompletable(ActionEmitter)}
         * utility.
         * @param <E> Type of error emitted
         */
        interface ActionEmitter<E> {
            Cancelable emitTo(Action onComplete, Consumer<E> onError);
        }
    }

    /**
     * Void behaviors are those Amplify category behaviors which have a void return type.
     * For example, most behaviors in Auth and DataStore have a void return. Unlike
     * {@link CancelableBehaviors}, such behaviors may not be canceled once begun.
     */
    static final class VoidBehaviors {
        private VoidBehaviors() {}

        static <E extends Throwable> Completable toCompletable(ActionEmitter<E> behavior) {
            return Completable.create(subscriber -> behavior.emitTo(subscriber::onComplete, subscriber::tryOnError));
        }

        static <T, E extends Throwable> Single<T> toSingle(ResultEmitter<T, E> behavior) {
            return Single.create(subscriber -> behavior.emitTo(subscriber::onSuccess, subscriber::tryOnError));
        }

        static <S, T, E extends Throwable> Observable<T> toObservable(StreamEmitter<S, T, E> behavior) {
            return Observable.create(subscriber -> {
                behavior.streamTo(
                    NoOpConsumer.create(),
                    subscriber::onNext,
                    subscriber::tryOnError,
                    subscriber::onComplete
                );
            });
        }

        /**
         * A behavior which streams items to a consumer, and then ends with an error or completion signal.
         * The behavior does not itself return anything synchronously.
         * The behavior begins by emitting to a start consumer.
         * @param <S> Type of token emitted on successful start
         * @param <T> Type of item output to stream consumer
         * @param <E> Type of error emitted
         */
        interface StreamEmitter<S, T, E extends Throwable> {
            void streamTo(Consumer<S> onStart, Consumer<T> onItem, Consumer<E> onError, Action onComplete);
        }

        /**
         * A behavior which emits to a result listener, but returns no value, itself.
         * @param <T> Type of result emitted
         * @param <E> Type of error emitted
         */
        interface ResultEmitter<T, E extends Throwable> {
            void emitTo(Consumer<T> onResult, Consumer<E> onError);
        }

        /**
         * Describes behavior which emits a completion notification via an {@link Action},
         * or alternately, emits an error to a {@link Consumer}.
         * @param <E> Type of error emitted
         */
        interface ActionEmitter<E> {
            void emitTo(Action onComplete, Consumer<E> onError);
        }
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
}
