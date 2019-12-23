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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreItemChange;

import java.util.Iterator;

import io.reactivex.Completable;
import io.reactivex.Observable;

/* package-local */ @SuppressWarnings({"WeakerAccess", "unused"})
final class RxDataStoreBinding implements RxDataStore {
    private final DataStoreCategoryBehavior dataStore;

    RxDataStoreBinding() {
        this(Amplify.DataStore);
    }

    RxDataStoreBinding(DataStoreCategoryBehavior dataStore) {
        this.dataStore = dataStore;
    }

    @NonNull
    @Override
    public <T extends Model> Completable save(@NonNull T item) {
        return toCompletable(listener -> dataStore.save(item, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Completable save(@NonNull T item, @NonNull QueryPredicate predicate) {
        return toCompletable(listener -> dataStore.save(item, predicate, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull T item) {
        return toCompletable(listener -> dataStore.delete(item, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<T> query(@NonNull Class<T> itemClass) {
        return toObservable(listener -> dataStore.query(itemClass, listener));
    }

    @NonNull
    @Override
    public <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass, @NonNull QueryPredicate predicate) {
        return toObservable(listener -> dataStore.query(itemClass, predicate, listener));
    }

    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return dataStore.observe();
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass) {
        return dataStore.observe(itemClass);
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull String uniqueId) {
        return dataStore.observe(itemClass, uniqueId);
    }

    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass, @NonNull QueryPredicate selectionCriteria) {
        return dataStore.observe(itemClass, selectionCriteria);
    }

    @SuppressWarnings("WhitespaceAround")
    private static <T> Observable<T> toObservable(
            RxAdapters.VoidResultListener<Iterator<T>> method) {
        return RxAdapters.<Iterator<T>>toSingle(listener -> {
            method.emitTo(listener);
            return () -> {};
        }).flatMapObservable(iterator -> Observable.create(emitter -> {
            while (iterator.hasNext()) {
                emitter.onNext(iterator.next());
            }
            emitter.onComplete();
        }));
    }

    private static <T extends Model> Completable toCompletable(
            RxAdapters.VoidResultListener<DataStoreItemChange<T>> method) {
        return RxAdapters.toCompletable(method);
    }
}
