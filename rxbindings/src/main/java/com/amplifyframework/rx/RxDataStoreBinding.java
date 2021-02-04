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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.NoOpAction;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreCategory;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.rx.RxAdapters.VoidBehaviors;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

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
    public <T extends Model> Completable save(@NonNull T item, @NonNull QueryPredicate predicate) {
        return toCompletable((onResult, onError) -> dataStore.save(item, predicate, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull T item) {
        return toCompletable((onResult, onError) -> dataStore.delete(item, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull T item, @NonNull QueryPredicate predicate) {
        return toCompletable((onResult, onError) ->
            dataStore.delete(item, predicate, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull Class<T> itemClass, @NonNull QueryPredicate predicate) {
        return toCompletable((onResult, onError) ->
                dataStore.delete(itemClass, predicate, NoOpAction.create(), onError));
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
        return toObservable((onResult, onError) ->
            dataStore.query(itemClass, predicate, onResult, onError));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass, @NonNull QueryOptions options) {
        return toObservable((onResult, onError) ->
            dataStore.query(itemClass, options, onResult, onError));
    }

    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return toObservable(dataStore::observe);
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(@NonNull Class<T> itemClass) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, onStart, onItem, onError, onComplete)
        );
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull String uniqueId) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, uniqueId, onStart, onItem, onError, onComplete)
        );
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull QueryPredicate selectionCriteria) {
        return toObservable((onStart, onItem, onError, onComplete) ->
            dataStore.observe(itemClass, selectionCriteria, onStart, onItem, onError, onComplete)
        );
    }

    @Override
    public Completable start() {
        return VoidBehaviors.toCompletable(dataStore::start);
    }

    @Override
    public Completable stop() {
        return VoidBehaviors.toCompletable(dataStore::stop);
    }

    @Override
    public Completable clear() {
        return VoidBehaviors.toCompletable(dataStore::clear);
    }

    private static <T extends Model> Observable<T> toObservable(
            VoidBehaviors.ResultEmitter<Iterator<T>, DataStoreException> method) {
        return VoidBehaviors.toSingle(method)
            .flatMapObservable(iterator -> Observable.create(emitter -> {
                while (iterator.hasNext()) {
                    emitter.onNext(iterator.next());
                }
                emitter.onComplete();
            }));
    }

    private static <T> Observable<T> toObservable(
            VoidBehaviors.StreamEmitter<Cancelable, T, DataStoreException> method) {
        // The provided behavior receives a cancelable in callback.
        // It is, in effect, like a cancelable behavior, we just have to remap the cancelable.
        return RxAdapters.CancelableBehaviors.<Cancelable, T, DataStoreException>toObservable(
            (onStart, onItem, onError, onComplete) -> {
                AtomicReference<Cancelable> cancelableContainer = new AtomicReference<>();
                method.streamTo(cancelableContainer::set, onItem, onError, onComplete);
                return () -> {
                    final Cancelable containedCancelable = cancelableContainer.get();
                    if (containedCancelable != null) {
                        containedCancelable.cancel();
                    }
                };
            }
        );
    }

    private static <T extends Model> Completable toCompletable(
            VoidBehaviors.ResultEmitter<DataStoreItemChange<T>, DataStoreException> method) {
        return VoidBehaviors.<DataStoreException>toCompletable((onComplete, onError) ->
                method.emitTo(result -> onComplete.call(), onError));
    }
}
