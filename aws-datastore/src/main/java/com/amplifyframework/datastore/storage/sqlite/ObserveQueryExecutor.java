/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.ItemChangeMapper;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.Subject;

/***
 * Executes observe query operations.
 * @param <T> type of Model.
 */
public class ObserveQueryExecutor<T extends Model> implements Cancelable {
    private final Subject<StorageItemChange<? extends Model>> itemChangeSubject;
    private final SqlQueryProcessor sqlQueryProcessor;
    private final ExecutorService threadPool;
    private final SyncStatus syncStatus;
    private Disposable disposable;
    private final List<DataStoreItemChange<T>> changedItemList = new ArrayList<>();
    private Timer timer;
    private final int maxRecords;
    private final long maxTimeSec;
    private final ModelSorter<T> modelSorter;
    private boolean isCanceled = false;
    private boolean isSynced = false;
    private final Map<String, T> completeItemMap = new ConcurrentHashMap<>();

    /**
     * Class to manage observeQuery operations.
     * @param itemChangeSubject change subject.
     * @param sqlQueryProcessor sql query processor.
     * @param threadPool thread pool.
     * @param syncStatus sync status.
     * @param modelSorter model sorter.
     * @param dataStoreConfiguration datastore configuration.
     */
    public ObserveQueryExecutor(@NonNull Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                                @NonNull SqlQueryProcessor sqlQueryProcessor,
                                @NonNull ExecutorService threadPool,
                                @NonNull SyncStatus syncStatus,
                                @NonNull ModelSorter<T> modelSorter,
                                @NonNull DataStoreConfiguration dataStoreConfiguration) {
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
        this.syncStatus = syncStatus;
        this.maxRecords = dataStoreConfiguration.getObserveQueryMaxRecords();
        this.maxTimeSec = dataStoreConfiguration.getMaxTimeLapseForObserveQuery();
        this.modelSorter = modelSorter;
    }

    /***
     *Constructor for ObserveQueryManager.
     * @param itemChangeSubject change subject.
     * @param sqlQueryProcessor sql query processor.
     * @param threadPool thread pool.
     * @param syncStatus sync status.
     * @param modelSorter model sorter.
     * @param maxRecords max records for batch.
     * @param maxSecs max time lapse for batch.
     */
    public ObserveQueryExecutor(@NonNull Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                                @NonNull SqlQueryProcessor sqlQueryProcessor,
                                @NonNull ExecutorService threadPool,
                                @NonNull SyncStatus syncStatus,
                                @NonNull ModelSorter<T> modelSorter,
                                int maxRecords,
                                int maxSecs) {
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
        this.syncStatus = syncStatus;
        this.modelSorter = modelSorter;
        this.maxRecords = maxRecords;
        this.maxTimeSec = maxSecs;
    }

    /**
     * observes changes and returns datastore items.
     * @param itemClass item to be observed.
     * @param options options for query.
     * @param onObservationStarted invoked on observation start provides a cancellable.
     * @param onQuerySnapshot invoked when query snapshot is returned.
     * @param onObservationError invoked on observation error.
     * @param onObservationComplete invoked when subscription is complete.
     */
    public void observeQuery(
            @NonNull Class<T> itemClass,
            @NonNull ObserveQueryOptions options,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete) {
        Objects.requireNonNull(onObservationStarted);
        Objects.requireNonNull(onObservationError);
        Objects.requireNonNull(onObservationComplete);

        Consumer<Object> onItemChanged = value -> {

            @SuppressWarnings("unchecked")
            StorageItemChange<T> itemChanged = (StorageItemChange<T>) value;
            try {
                if (sqlQueryProcessor.modelExists(itemChanged.item(), options.getQueryPredicate())) {
                    updateCompleteItemMap(itemChanged);
                } else if (itemChanged.type() == StorageItemChange.Type.UPDATE) {
                    completeItemMap.remove(itemChanged.item().getPrimaryKeyString());
                } else if (itemChanged.type() == StorageItemChange.Type.DELETE) {
                    completeItemMap.remove(itemChanged.item().getPrimaryKeyString());
                }
                collect(itemChanged, onQuerySnapshot, itemClass, options, onObservationError);
            } catch (DataStoreException exception) {
                onObservationError.accept(exception);
            }
        };
        threadPool.submit(() -> queryLocalData(
            itemClass,
            options,
            value -> {
                disposable = itemChangeSubject
                        .filter(x -> x.item().getClass().isAssignableFrom(itemClass))
                        .subscribe(
                                onItemChanged::accept,
                                failure -> {
                                    if (failure instanceof DataStoreException) {
                                        onObservationError.accept((DataStoreException) failure);
                                        return;
                                    }
                                    onObservationError.accept(new DataStoreException(
                                            "Failed to observe items in storage adapter.",
                                            failure,
                                            "Inspect the failure details."
                                    ));
                                },
                                onObservationComplete::call
                        );
                onObservationStarted.accept(this);
                onQuerySnapshot.accept(value);
            },
            onObservationError
        ));
    }

    private void queryLocalData(@NonNull Class<T> itemClass,
                                @NonNull ObserveQueryOptions options,
                                @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                @NonNull Consumer<DataStoreException> onObservationError) {
        List<T> models = sqlQueryProcessor.queryOfflineData(itemClass,
                Where.matchesAndSorts(options.getQueryPredicate(),
                        options.getSortBy()), onObservationError);
        Consumer<DataStoreException> onQueryError = value -> {
            cancel();
            onObservationError.accept(value);
        };
        for (T model : models) {
            completeItemMap.put(model.getPrimaryKeyString(), model);
        }
        callOnQuerySnapshot(onQuerySnapshot, itemClass, onQueryError, models);
    }

    /***
     * Cancel observe query operation and subscription.
     */
    @Override
    public void cancel() {
        isCanceled = true;
        resetTimer();
        completeItemMap.clear();
        changedItemList.clear();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void resetTimer() {
        if (timer != null) {
            timer.purge();
        }
        timer = null;
    }

    /***
     * Get if observe query subscription is cancelled.
     * @return boolean.
     */
    boolean getIsCancelled() {
        return isCanceled;
    }

    /***
     * Get map of total items in observe query subscription.
     * @return Map.
     */
    Map<String, T> getCompleteMap() {
        return Immutable.of(completeItemMap);
    }

    /***
     * Get list of changed items in observe query subscription.
     * @return List
     */
    List<DataStoreItemChange<T>> getChangeList() {
        return Immutable.of(changedItemList);
    }

    private void collect(StorageItemChange<T> changedItem,
                         @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                         Class<T> itemClass,
                         ObserveQueryOptions options,
                         Consumer<DataStoreException> onObservationError) {
        try {
            changedItemList.add(ItemChangeMapper.map(changedItem));
            setTimerIfNeeded(onQuerySnapshot, itemClass, options, onObservationError);

            if (changedItemList.size() >= maxRecords || isSynced) {
                resetTimer();
                processQuerySnapshot(onQuerySnapshot, itemClass, options, onObservationError);
                changedItemList.clear();
            }
        } catch (DataStoreException exception) {
            onObservationError.accept(exception);
        }
    }

    private void processQuerySnapshot(@NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                      Class<T> itemClass,
                                      ObserveQueryOptions options,
                                      Consumer<DataStoreException> onObservationError) {
        List<T> completeList = new ArrayList<>(completeItemMap.values());
        sortIfNeeded(options, completeList, itemClass, onObservationError);
        callOnQuerySnapshot(onQuerySnapshot, itemClass, onObservationError, completeList);
    }

    private void callOnQuerySnapshot(Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                     Class<T> itemClass,
                                     Consumer<DataStoreException> onObservationError,
                                     List<T> completeList) {
        isSynced = syncStatus.get(itemClass.getSimpleName(), onObservationError);
        DataStoreQuerySnapshot<T> dataStoreQuerySnapshot = new DataStoreQuerySnapshot<>(completeList,
                isSynced);
        getListConsumer(onQuerySnapshot).accept(dataStoreQuerySnapshot);
        changedItemList.clear();
    }

    private void sortIfNeeded(ObserveQueryOptions options,
                              List<T> completeList,
                              Class<T> itemClass,
                              Consumer<DataStoreException> onObservationError) {
        if (options != null && options.getSortBy() != null && options.getSortBy().size() > 0) {
            modelSorter.sort(options, completeList, itemClass, onObservationError);
        }
    }

    private void updateCompleteItemMap(StorageItemChange<T> itemChanged) {
        T item = itemChanged.item();
        if (itemChanged.type() == StorageItemChange.Type.DELETE) {
            completeItemMap.remove(item.getPrimaryKeyString());
        } else {
            completeItemMap.put(item.getPrimaryKeyString(), item);
        }
    }

    private void setTimerIfNeeded(Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                  Class<T> itemClass,
                                  ObserveQueryOptions options,
                                  Consumer<DataStoreException> onObservationError) {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    processQuerySnapshot(onQuerySnapshot, itemClass, options, onObservationError);
                    resetTimer();
                }
            }, TimeUnit.SECONDS.toMillis(maxTimeSec));
        }
    }

    @NonNull
    private Consumer<DataStoreQuerySnapshot<T>> getListConsumer(Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot) {
        return value -> {
            if (isCanceled) {
                return;
            }
            onQuerySnapshot.accept(value);
        };
    }
}
