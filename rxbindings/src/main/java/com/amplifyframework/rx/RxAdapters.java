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

package com.amplifyframework.rx;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

/**
 * Utility method to convert between behaviors that use the {@link ResultListener}
 * and {@link StreamListener}, into {@link Single} and {@link Observable}.
 */
final class RxAdapters {
    @SuppressWarnings("checkstyle:all") private RxAdapters() {}

    static <T> Completable toCompletable(VoidResultListener<T> voidResultListener) {
        return Completable.defer(() -> Completable.create(emitter -> {
            ResultListener<T> listener =
                ResultListener.instance(result -> emitter.onComplete(), emitter::onError);
            voidResultListener.emitTo(listener);
        }));
    }

    static <T> Single<T> toSingle(CancelableResultListener<T> cancelableResultListener) {
        return Single.defer(() -> Single.create(emitter -> {
            final CompositeDisposable disposable = new CompositeDisposable();
            ResultListener<T> listener =
                ResultListener.instance(emitter::onSuccess, emitter::onError);
            final Cancelable cancelable = cancelableResultListener.emitTo(listener);
            disposable.add(Disposables.fromAction(cancelable::cancel));
        }));
    }

    static <T> Observable<T> toObservable(CancelableStreamListener<T> cancelableStreamListener) {
        return Observable.defer(() -> Observable.create(emitter -> {
            final CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            StreamListener<T> listener =
                StreamListener.instance(emitter::onNext, emitter::onError, emitter::onComplete);
            final Cancelable cancelable = cancelableStreamListener.streamTo(listener);
            disposable.add(Disposables.fromAction(cancelable::cancel));
        }));
    }

    /**
     * A behavior which emits to a result listener, but returns no value, itself.
     */
    interface VoidResultListener<T> {
        void emitTo(ResultListener<T> listener);
    }

    /**
     * Describes a behavior which emits results to a ResultListener,
     * and can be canceled via an {@link Cancelable}. Such a method
     * may be wrapped into an {@link Single} in a uniform way such as by the
     * {@link RxAdapters#toSingle(CancelableResultListener)} method.
     * @param <T> Type of object in in result listener's success response
     */
    interface CancelableResultListener<T> {
        Cancelable emitTo(ResultListener<T> listener);
    }

    /**
     * Describes a behavior which emits to an {@link StreamListener}, and can be
     * canceled by an {@link Cancelable}. Such a method may be wrapped into an
     * {@link Observable} in a uniform way such as by the
     * {@link RxAdapters#toObservable(CancelableStreamListener)} method.
     * @param <T> Type of object being emitted onto the subscriber's onNext.
     */
    interface CancelableStreamListener<T> {
        Cancelable streamTo(StreamListener<T> listener);
    }

}
