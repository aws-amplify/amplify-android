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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

/**
 * A simple in-memory implementation of the LocalStorageAdapter
 * contract. This intended for use as a stub in test code.
 */
public final class InMemoryStorageAdapter implements LocalStorageAdapter {

    private final List<Model> items;
    private final PublishSubject<StorageItemChange.Record> changeRecordStream;
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    private InMemoryStorageAdapter() {
        this.items = new ArrayList<>();
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

    @Override
    public void initialize(
            @NonNull Context context,
            @NonNull ResultListener<List<ModelSchema>, DataStoreException> listener) {
    }

    @Override
    public <T extends Model> void save(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final ResultListener<StorageItemChange.Record, DataStoreException> itemSaveListener) {
        save(item, initiator, null, itemSaveListener);
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void save(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @Nullable final QueryPredicate predicate,
            @NonNull final ResultListener<StorageItemChange.Record, DataStoreException> itemSaveListener) {
        items.add(item);
        StorageItemChange.Record save = StorageItemChange.<T>builder()
                .item(item)
                .itemClass((Class<T>) item.getClass())
                .type(StorageItemChange.Type.SAVE)
                .predicate(predicate)
                .initiator(initiator)
                .build()
                .toRecord(storageItemChangeConverter);
        changeRecordStream.onNext(save);
        itemSaveListener.onResult(save);
    }

    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @NonNull final ResultListener<Iterator<T>, DataStoreException> queryResultsListener) {
        query(itemClass, null, queryResultsListener);
    }

    @SuppressWarnings("unchecked") // (T) item *is* checked, via isAssignableFrom().
    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> itemClass,
            @Nullable final QueryPredicate predicate,
            @NonNull final ResultListener<Iterator<T>, DataStoreException> queryResultsListener) {
        List<T> result = new ArrayList<>();
        for (Model item : items) {
            if (itemClass.isAssignableFrom((item.getClass()))) {
                result.add((T) item);
            }
        }
        queryResultsListener.onResult(result.iterator());
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final StorageItemChange.Initiator initiator,
            @NonNull final ResultListener<StorageItemChange.Record, DataStoreException> itemDeletionListener) {
        for (Model savedItem : items) {
            if (savedItem.getId().equals(item.getId())) {
                items.remove(item);
                StorageItemChange.Record deletion = StorageItemChange.<T>builder()
                    .item((T) savedItem)
                    .itemClass((Class<T>) savedItem.getClass())
                    .type(StorageItemChange.Type.DELETE)
                    .initiator(initiator)
                    .build()
                    .toRecord(storageItemChangeConverter);
                changeRecordStream.onNext(deletion);
                itemDeletionListener.onResult(deletion);
                return;
            }
        }
    }

    @NonNull
    @Override
    public Cancelable observe(
            StreamListener<StorageItemChange.Record, DataStoreException> itemChangeListener) {
        Disposable disposable = changeRecordStream.subscribe(
            itemChangeListener::onNext,
            failure -> {
                if (failure instanceof DataStoreException) {
                    itemChangeListener.onError((DataStoreException) failure);
                } else {
                    itemChangeListener.onError(new DataStoreException(
                        "Failed to observe changes to in-memory storage adapter.",
                        failure, "Inspect the details."
                    ));
                }
            },
            itemChangeListener::onComplete
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
    public List<Model> items() {
        return items;
    }
}
