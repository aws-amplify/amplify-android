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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreCategory;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;
import io.reactivex.Observable;

final class RxDataStoreBinding implements RxDataStoreCategoryBehavior {
    private final DataStoreCategoryBehavior dataStore;

    RxDataStoreBinding() {
        this(Amplify.DataStore);
    }

    @VisibleForTesting
    RxDataStoreBinding(DataStoreCategory dataStore) {
        this.dataStore = dataStore;
    }

    @NonNull
    @Override
    public <T extends Model> Completable save(@NonNull T item) {
        return toCompletable((onResult, onError) -> dataStore.save(item, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull T item) {
        return toCompletable((onResult, onError) -> dataStore.delete(item, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<T> query(@NonNull Class<T> itemClass) {
        return toObservable((onResult, onError) -> dataStore.query(itemClass, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass, @NonNull QueryPredicate predicate) {
        return toObservable((onResult, onError) -> dataStore.query(itemClass, predicate, onResult, onError));
    }

    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return toObservable(dataStore::observe);
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, onStart, onItem, onError, onComplete));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull String uniqueId) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, uniqueId, onStart, onItem, onError, onComplete));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull QueryPredicate selectionCriteria) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, selectionCriteria, onStart, onItem, onError, onComplete));
    }

    private static <T extends Model> Observable<T> toObservable(
            RxAdapters.VoidResultEmitter<Iterator<T>, DataStoreException> method) {
        return RxAdapters.<Iterator<T>, DataStoreException>toSingle((onResult, onError) -> {
            method.emitTo(onResult, onError);
            return new NoOpCancelable();
        }).flatMapObservable(iterator -> Observable.create(emitter -> {
            while (iterator.hasNext()) {
                emitter.onNext(iterator.next());
            }
            emitter.onComplete();
        }));
    }

    private static <T> Observable<T> toObservable(DataStoreObserveMethod<T> method) {
        return RxAdapters.<Cancelable, T, DataStoreException>toObservable(((onStart, onItem, onError, onComplete) -> {
            AtomicReference<Cancelable> cancelableContainer = new AtomicReference<>();
            method.streamTo(cancelableContainer::set, onItem, onError, onComplete);
            return () -> {
                final Cancelable containedCancelable = cancelableContainer.get();
                if (containedCancelable != null) {
                    containedCancelable.cancel();
                }
            };
        }));
    }

    private static <T extends Model> Completable toCompletable(
            RxAdapters.VoidResultEmitter<DataStoreItemChange<T>, DataStoreException> method) {
        return RxAdapters.toCompletable(method);
    }

    interface DataStoreObserveMethod<T> {
        void streamTo(
            Consumer<Cancelable> onStart,
            Consumer<T> onItem,
            Consumer<DataStoreException> onError,
            Action onComplete);
    }
}
