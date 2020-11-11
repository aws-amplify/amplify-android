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

package com.amplifyframework.testutils.sync;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.VoidResult;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A utility to facilitate synchronous calls to the Amplify DataStore category.
 * This is not appropriate for production use, but is valuable in test code.
 * Test code wants to perform a series of sequential verifications, with the assumption
 * that a DataStore operation has completed with some kind of terminal result.
 */
public final class SynchronousDataStore {
    private final DataStoreCategoryBehavior asyncDelegate;

    private SynchronousDataStore(DataStoreCategoryBehavior asyncDelegate) {
        this.asyncDelegate = asyncDelegate;
    }

    /**
     * Creates a synchronizing wrapper around the provided {@link DataStoreCategoryBehavior}.
     * @param asyncDelegate This performs actual DataStore operations
     * @return A synchronizing wrapper around the provided {@link DataStoreCategoryBehavior}
     */
    @NonNull
    public static SynchronousDataStore delegatingTo(@NonNull DataStoreCategoryBehavior asyncDelegate) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousDataStore(asyncDelegate);
    }

    /**
     * Saves an item into the DataStore.
     * @param item Item to save
     * @param <T> The type of item being saved
     * @throws DataStoreException On failure saving item into DataStore
     */
    public <T extends Model> void save(@NonNull T item) throws DataStoreException {
        awaitDataStoreItemChange((onResult, onError) ->
            asyncDelegate.save(item, onResult, onError));
    }

    /**
     * Search for an item in the DataStore by its class type and ID.
     * @param clazz Class of item being accessed
     * @param itemId Unique ID of the item being accessed
     * @param <T> The type of item being accessed
     * @return An item with the provided class and ID, if present in DataStore
     * @throws NoSuchElementException If there is no matching item in the DataStore
     * @throws DataStoreException On failure querying data store
     */
    @NonNull
    public <T extends Model> T get(@NonNull Class<T> clazz, @NonNull String itemId) throws DataStoreException {
        for (T value : list(clazz)) {
            if (value.getId().equals(itemId)) {
                return value;
            }
        }
        throw new NoSuchElementException("No item in DataStore with class = " + clazz + " and id = " + itemId);
    }

    /**
     * Lists all DataStore items of a given class.
     * @param clazz Class of item being listed
     * @param <T> The type of item being listed
     * @return A list of DataStore items of the given class, possibly empty
     * @throws DataStoreException On failure querying data store
     */
    @NonNull
    public <T extends Model> List<T> list(@NonNull Class<T> clazz) throws DataStoreException {
        final Iterator<T> iterator =
            awaitIterator((onResult, onError) -> asyncDelegate.query(clazz, onResult, onError));
        List<T> items = new ArrayList<>();
        while (iterator.hasNext()) {
            items.add(iterator.next());
        }
        return Immutable.of(items);
    }

    /**
     * Calls the start method of the underlying DataStore implementation.
     * @throws DataStoreException On failure to start data store.
     */
    public void start() throws DataStoreException {
        await((onComplete, onError) -> asyncDelegate.start(() -> onComplete.accept(VoidResult.instance()), onError));
    }

    /**
     * Call the clear method of the underlying DataStore implementation.
     * @throws DataStoreException On failure to clear data store.
     */
    public void clear() throws DataStoreException {
        await((onComplete, onError) -> asyncDelegate.clear(() -> onComplete.accept(VoidResult.instance()), onError));
    }

    /**
     * Calls the save method of the underlying DataStore implementation.
     * @throws DataStoreException On failure to stop data store.
     */
    public void stop() throws DataStoreException {
        await((onComplete, onError) -> asyncDelegate.stop(() -> onComplete.accept(VoidResult.instance()), onError));
    }

    // Syntax fluff to get rid of type bounds at location of call
    @SuppressWarnings("UnusedReturnValue")
    private <T extends Model> DataStoreItemChange<T> awaitDataStoreItemChange(
            Await.ResultErrorEmitter<DataStoreItemChange<T>, DataStoreException> resultErrorEmitter)
            throws DataStoreException {
        return Await.result(resultErrorEmitter);
    }

    // Syntax fluff to get rid of type bounds at location of call
    private <T extends Model> Iterator<T> awaitIterator(
            Await.ResultErrorEmitter<Iterator<T>, DataStoreException> resultErrorEmitter)
            throws DataStoreException {
        return Await.result(resultErrorEmitter);
    }

    // Syntax fluff to get rid of type bounds at location of call
    private void await(Await.ResultErrorEmitter<VoidResult, DataStoreException> resultErrorEmitter)
            throws DataStoreException {
        Await.result(resultErrorEmitter);
    }
}
