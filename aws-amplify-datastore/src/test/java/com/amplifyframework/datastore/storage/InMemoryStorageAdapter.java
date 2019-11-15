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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.MutationEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * A simple in-memory implementation of the LocalStorageAdapter
 * contract. This intended for use as a stub in test code.
 */
public final class InMemoryStorageAdapter implements LocalStorageAdapter {

    private final List<Model> items;
    private final PublishSubject<MutationEvent<?>> publishSubject;

    private InMemoryStorageAdapter() {
        items = new ArrayList<>();
        publishSubject = PublishSubject.create();
    }

    /**
     * Creates an instance of the InMemoryStorageAdapter.
     * @return Instance of InMemoryStorageAdapter.
     */
    public static InMemoryStorageAdapter create() {
        return new InMemoryStorageAdapter();
    }

    /**
     * Setup the storage engine with the models.
     * For each {@link Model}, construct a
     * {@link ModelSchema}
     * and setup the necessities for persisting a {@link Model}.
     * This setUp is a pre-requisite for all other operations
     * of a LocalStorageAdapter.
     *
     * @param context  Android application context required to
     *                 interact with a storage mechanism in Android.
     * @param models   list of Model classes
     * @param listener the listener to be invoked to notify completion
     */
    @Override
    public void setUp(@NonNull Context context,
                      @NonNull List<Class<? extends Model>> models,
                      @NonNull ResultListener<List<ModelSchema>> listener) {

    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void save(@NonNull final T item,
                     @NonNull final ResultListener<MutationEvent<T>> listener) {
        items.add(item);
        MutationEvent<T> mutation = MutationEvent.<T>builder()
            .data(item)
            .dataClass((Class<T>) item.getClass())
            .mutationType(MutationEvent.MutationType.INSERT)
            .source(MutationEvent.Source.DATA_STORE)
            .build();
        publishSubject.onNext(mutation);
        listener.onResult(mutation);
    }

    @SuppressWarnings("unchecked") // (T) item *is* checked, via isAssignableFrom().
    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> queryClass,
            @NonNull final ResultListener<Iterator<T>> listener) {

        List<T> result = new ArrayList<>();
        for (Model item : items) {
            if (queryClass.isAssignableFrom((item.getClass()))) {
                result.add((T) item);
            }
        }
        listener.onResult(result.iterator());
    }

    @SuppressWarnings("unchecked") // item.getClass() -> Class<?>, but type is T. So cast as Class<T> is OK.
    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final ResultListener<MutationEvent<T>> listener) {

        while (items.iterator().hasNext()) {
            Model next = items.iterator().next();
            if (next.equals(item)) {
                items.remove(item);
                MutationEvent<T> mutation = MutationEvent.<T>builder()
                    .data(item)
                    .dataClass((Class<T>) item.getClass())
                    .mutationType(MutationEvent.MutationType.DELETE)
                    .source(MutationEvent.Source.DATA_STORE)
                    .build();
                listener.onResult(mutation);
                publishSubject.onNext(mutation);
            }
        }
    }

    @Override
    public Observable<MutationEvent<?>> observe() {
        return publishSubject;
    }

    /**
     * Get the items that are in the storage.
     * @return Items in storage
     */
    public List<Model> items() {
        return items;
    }
}
