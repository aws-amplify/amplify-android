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

package com.amplifyframework.datastore.storage;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * A simple in-memory implementation of the LocalStorageAdapter
 * contract. This intended for use as a stub in test code.
 */
public final class InMemoryStorageAdapter implements LocalStorageAdapter {
    private final List<Model> items;
    private final Subject<StorageItemChange<? extends Model>> itemChangeStream;

    private InMemoryStorageAdapter() {
        this.items = new ArrayList<>();
        this.itemChangeStream = PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
    }

    /**
     * Creates an instance of the InMemoryStorageAdapter.
     * @return Instance of InMemoryStorageAdapter.
     */
    public static InMemoryStorageAdapter create() {
        return new InMemoryStorageAdapter();
    }

    @Override
    public void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void save(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final QueryPredicate predicate,
            @NonNull final Consumer<StorageItemChange<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError) {
        StorageItemChange.Type type = StorageItemChange.Type.CREATE;
        final int index = indexOf(item);
        if (index > -1) {
            // There is an existing record with that ID; this is an update.
            type = StorageItemChange.Type.UPDATE;
            Model savedItem = items.get(index);
            if (!predicate.evaluate(savedItem)) {
                onError.accept(new DataStoreException(
                    "Conditional check failed.",
                    "Verify that there is a saved model that matches the provided predicate."));
                return;
            } else {
                items.remove(index);
            }
        }

        items.add(item);
        StorageItemChange<T> change = StorageItemChange.<T>builder()
            .item(item)
            .itemClass((Class<T>) item.getClass())
            .type(type)
            .predicate(predicate)
            .initiator(initiator)
            .build();
        itemChangeStream.onNext(change);
        onSuccess.accept(change);
    }

    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @NonNull final Consumer<Iterator<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        query(itemClass, Where.matchesAll(), onSuccess, onError);
    }

    @SuppressWarnings("unchecked") // (T) item *is* checked, via isAssignableFrom().
    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @NonNull final QueryOptions options,
            @NonNull final Consumer<Iterator<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        final List<T> result = new ArrayList<>();
        final QueryPredicate predicate = options.getQueryPredicate();
        for (Model item : items) {
            if (itemClass.isAssignableFrom(item.getClass()) && predicate.evaluate(item)) {
                result.add((T) item);
            }
        }
        onSuccess.accept(result.iterator());
    }

    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final Consumer<StorageItemChange<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        delete(item, initiator, QueryPredicates.all(), onSuccess, onError);
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final QueryPredicate predicate,
            @NonNull final Consumer<StorageItemChange<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        final int index = indexOf(item);
        if (index < 0) {
            onError.accept(new DataStoreException(
                    "This item was not found in the datastore: " + item.toString(),
                    "Use save() function to create models to store."
            ));
            return;
        }
        Model savedItem = items.remove(index);

        if (!predicate.evaluate(savedItem)) {
            onError.accept(new DataStoreException(
                    "Conditional check failed.",
                    "Verify that there is a saved model that matches the provided predicate."));
            return;
        }
        StorageItemChange<T> deletion = StorageItemChange.<T>builder()
            .item((T) savedItem)
            .itemClass((Class<T>) savedItem.getClass())
            .type(StorageItemChange.Type.DELETE)
            .predicate(predicate)
            .initiator(initiator)
            .build();
        itemChangeStream.onNext(deletion);
        onSuccess.accept(deletion);
    }

    @NonNull
    @Override
    public Cancelable observe(
            @NonNull Consumer<StorageItemChange<? extends Model>> onNextItem,
            @NonNull Consumer<DataStoreException> onSubscriptionError,
            @NonNull Action onSubscriptionComplete) {
        Disposable disposable = itemChangeStream.subscribe(
            onNextItem::accept,
            failure -> {
                if (failure instanceof DataStoreException) {
                    onSubscriptionError.accept((DataStoreException) failure);
                } else {
                    onSubscriptionError.accept(new DataStoreException(
                        "Failed to observe changes to in-memory storage adapter.",
                        failure, "Inspect the details."
                    ));
                }
            },
            onSubscriptionComplete::call
        );
        return disposable::dispose;
    }

    @Override
    public void terminate() {
        items.clear();
        itemChangeStream.onComplete();
    }

    @Override
    public void clear(@NonNull Action onComplete,
                      @NonNull Consumer<DataStoreException> onError) {
        items.clear();
        onComplete.call();
    }

    private int indexOf(Model item) {
        int index = 0;
        for (Model savedItem : items) {
            if (savedItem.getClass().equals(item.getClass())
                    && savedItem.getId().equals(item.getId())) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
