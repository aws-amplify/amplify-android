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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.plugin.Plugin;

import java.util.Iterator;

/**
 * A plugin which implements the required behavior of the {@link DataStoreCategory}.
 * @param <E> The class type of the escape hatch which a subclass shall
 *            make available, to perform low-level implementation-specific operations.
 */
public abstract class DataStorePlugin<E> implements DataStoreCategoryBehavior, Plugin<E> {
    @NonNull
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.DATASTORE;
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) throws AmplifyException {
    }

    /**
     * Query the DataStore to find all items of the requested model (by name).
     * NOTE: Private method and should not be part of {@link DataStoreCategory}
     * @param modelName name of the Model to query
     * @param options Filtering, paging, and sorting options
     * @param onQueryResults Called when a query successfully returns 0 or more results
     * @param onQueryFailure Called when a failure interrupts successful completion of a query
     */
    public void query(
            @NonNull String modelName,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<? extends Model>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
    }

    /**
     * Observe changes to a certain type of item(s) in the DataStore.
     * @param modelName The name of the model to observe
     * @param onObservationStarted Called when observation begins
     * @param onDataStoreItemChange Called 0..n times, whenever there is a change to an
     *                              item of the requested class
     * @param onObservationFailure Called if observation of the DataStore terminates
     *                             with a non-recoverable failure
     * @param onObservationCompleted Called when observation completes gracefully
     */
    public void observe(
            @NonNull String modelName,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
    }
}
