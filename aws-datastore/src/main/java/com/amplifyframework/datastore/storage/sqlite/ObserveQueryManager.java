package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.ItemChangeMapper;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.Subject;


public class ObserveQueryManager<T extends Model> implements Cancelable {
    private final Subject<StorageItemChange<? extends Model>> itemChangeSubject;
    private final SqlQueryProcessor sqlQueryProcessor;
    private final ExecutorService threadPool;
    private final SyncStatus syncStatus;
    private Disposable disposable;
    private boolean isCanceled = false;
    private final List<DataStoreItemChange<T>> changedItemList = new ArrayList<>();
    private final Map<String, T> completeItemMap = new HashMap<>();

    private Timer timer;
    private final int MAX_RECORDS;
    private final long MAX_TIME_SEC;


    //TODOPM: sort complete list and set sync statusF
    //TODOPM: Test race condition between obderve and query.
    public ObserveQueryManager(@NonNull Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                               @NonNull SqlQueryProcessor sqlQueryProcessor,
                               @NonNull ExecutorService threadPool,
                               @NonNull SyncStatus syncStatus,
                               @NonNull DataStoreConfiguration dataStoreConfiguration
    ){
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
        this.syncStatus = syncStatus;
        this.MAX_RECORDS = dataStoreConfiguration.getObserveQueryMaxRecords();
        this.MAX_TIME_SEC = dataStoreConfiguration.getMaxTimeLapseForObserveQuery();
    }

    public ObserveQueryManager(@NonNull Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                               @NonNull SqlQueryProcessor sqlQueryProcessor,
                               @NonNull ExecutorService threadPool,
                               @NonNull SyncStatus syncStatus,
                               int maxRecords,
                               int maxSecs){
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
        this.syncStatus = syncStatus;
        this.MAX_RECORDS = maxRecords;
        this.MAX_TIME_SEC = maxSecs;
    }

    /**
     * {@inheritDoc}
     */
    public void observeQuery(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete) {
        Objects.requireNonNull(onObservationStarted);
        Objects.requireNonNull(onObservationError);
        Objects.requireNonNull(onObservationComplete);
        onObservationStarted.accept(this);

        Consumer<Object> onItemChanged = value -> {
            @SuppressWarnings("unchecked")   StorageItemChange<T> itemChanged = (StorageItemChange<T>) value;
            updateCompleteItemList( itemChanged );
            collect(itemChanged, onQuerySnapshot, itemClass, onObservationError);
        };
        threadPool.submit(() -> {
            List<T> models =  sqlQueryProcessor.queryOfflineData(itemClass, options, onObservationError);
            for ( T model : models ) {
                completeItemMap.put(model.getId(), model);
            }
            callOnQuerySnapshot(onQuerySnapshot, itemClass, onObservationError);
        });

        disposable = itemChangeSubject
               .filter(x-> x.item().getClass().isAssignableFrom(itemClass) && sqlQueryProcessor.modelExists(x.item(),options.getQueryPredicate()) )
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
    }

    @Override
    public void cancel() {
        isCanceled =  true;
        if (disposable != null){
           disposable.dispose();
        }
    }


    private void collect(StorageItemChange<T> changedItem,
                         @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                         Class<T> itemClass,
                         Consumer<DataStoreException> onObservationError) {
        try {
            changedItemList.add( ItemChangeMapper.map(changedItem));
            setTimerIfNeeded(onQuerySnapshot, itemClass, onObservationError);

            if (changedItemList.size()>= MAX_RECORDS){
                callOnQuerySnapshot(onQuerySnapshot, itemClass, onObservationError);
                changedItemList.clear();
                timer.cancel();
                timer = null;
            }
        } catch (DataStoreException e) {
            e.printStackTrace();
        }
    }

    private void callOnQuerySnapshot(@NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                     Class<T> itemClass,
                                     Consumer<DataStoreException> onObservationError) {
        try {
            DataStoreQuerySnapshot<T> dataStoreQuerySnapshot =
                    new DataStoreQuerySnapshot<T>(new ArrayList<T>(completeItemMap.values()),
                            syncStatus.get(itemClass.getName(), onObservationError),
                            changedItemList);
            getListConsumer(onQuerySnapshot).accept(dataStoreQuerySnapshot);
        } catch (DataStoreException exception){
            onObservationError.accept(exception);
        }
    }

    private void updateCompleteItemList(StorageItemChange<T> itemChanged){
        T item = itemChanged.item();
        if (itemChanged.type() == StorageItemChange.Type.DELETE){
            completeItemMap.remove(item.getId());
        } else {
            completeItemMap.put(item.getId(), item);
        }
    }

    private void setTimerIfNeeded(Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
                                  Class<T> itemClass,
                                  Consumer<DataStoreException> onObservationError) {
        if (timer == null){
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    callOnQuerySnapshot(onQuerySnapshot, itemClass, onObservationError);
                }
            }, TimeUnit.SECONDS.toMillis(MAX_TIME_SEC));
        }
    }

    @NonNull
    private Consumer<DataStoreQuerySnapshot<T>> getListConsumer(Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot) {
        return value -> {
            if (isCanceled) return;
            onQuerySnapshot.accept(value);
        };
    }
}
