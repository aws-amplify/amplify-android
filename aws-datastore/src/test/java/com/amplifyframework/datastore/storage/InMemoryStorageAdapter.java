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
import androidx.annotation.Nullable;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

/**
 * A simple in-memory implementation of the LocalStorageAdapter
 * contract. This intended for use as a stub in test code.
 */
public final class InMemoryStorageAdapter implements LocalStorageAdapter {

    private final Map<String, Model> items;
    private final PublishSubject<StorageItemChange.Record> changeRecordStream;
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    private InMemoryStorageAdapter() {
        this.items = new HashMap<>();
        this.changeRecordStream = PublishSubject.create();
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
    }

    /**
     * Creates an instance of the InMemoryStorageAdapter.
     * @return Instance of InMemoryStorageAdapter.
     */
    public static InMemoryStorageAdapter create() {
        return new InMemoryStorageAdapter();
    }

    @SuppressWarnings("WhitespaceAround") // Looks better this way
    @Override
    public void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {}

    @Override
    public <T extends Model> void save(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final Consumer<StorageItemChange.Record> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        save(item, initiator, null, onSuccess, onError);
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void save(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @Nullable final QueryPredicate predicate,
            @NonNull final Consumer<StorageItemChange.Record> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        if (items.containsKey(item.getId())) {
            // Update
            Model savedItem = items.remove(item.getId());
            if (predicate == null || predicate.evaluate(savedItem)) {
                items.remove(item.getId());
                save(item, initiator, predicate, onSuccess, onError);
                return;
            }
            onError.accept(new DataStoreException(
                    "Conditional check failed.",
                    "Verify the saved models."));
        } else {
            // Insert
            items.put(item.getId(), item);
            StorageItemChange.Record save = StorageItemChange.<T>builder()
                    .item(item)
                    .itemClass((Class<T>) item.getClass())
                    .type(StorageItemChange.Type.SAVE)
                    .predicate(predicate)
                    .initiator(initiator)
                    .build()
                    .toRecord(storageItemChangeConverter);
            changeRecordStream.onNext(save);
            onSuccess.accept(save);
        }
    }

    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @NonNull final Consumer<Iterator<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        query(itemClass, null, onSuccess, onError);
    }

    @SuppressWarnings("unchecked") // (T) item *is* checked, via isAssignableFrom().
    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @Nullable final QueryPredicate predicate,
            @NonNull final Consumer<Iterator<T>> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        List<T> result = new ArrayList<>();
        for (Model item : items.values()) {
            if (itemClass.isAssignableFrom(item.getClass())
                    && (predicate == null || predicate.evaluate(item))) {
                result.add((T) item);
            }
        }
        onSuccess.accept(result.iterator());
    }

    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final Consumer<StorageItemChange.Record> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        delete(item, initiator, null, onSuccess, onError);
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @Nullable final QueryPredicate predicate,
            @NonNull final Consumer<StorageItemChange.Record> onSuccess,
            @NonNull final Consumer<DataStoreException> onError
    ) {
        if (items.containsKey(item.getId())
                && (predicate == null || predicate.evaluate(item))) {
            Model savedItem = items.remove(item.getId());
            StorageItemChange.Record deletion = StorageItemChange.<T>builder()
                    .item((T) savedItem)
                    .itemClass((Class<T>) savedItem.getClass())
                    .type(StorageItemChange.Type.DELETE)
                    .predicate(predicate)
                    .initiator(initiator)
                    .build()
                    .toRecord(storageItemChangeConverter);
            changeRecordStream.onNext(deletion);
            onSuccess.accept(deletion);
        } else {
            onError.accept(new DataStoreException(
                    "Item does not exist or conditional check failed.",
                    "Verify the saved models."));
        }
    }

    @NonNull
    @Override
    public Cancelable observe(
            @NonNull Consumer<StorageItemChange.Record> onNextItem,
            @NonNull Consumer<DataStoreException> onSubscriptionError,
            @NonNull Action onSubscriptionComplete) {
        Disposable disposable = changeRecordStream.subscribe(
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
        changeRecordStream.onComplete();
    }

    /**
     * Get the items that are in the storage.
     * @return Items in storage
     */
    public Map<String, Model> items() {
        return items;
    }
}
