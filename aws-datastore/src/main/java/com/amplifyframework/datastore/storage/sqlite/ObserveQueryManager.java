package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.ItemChangeMapper;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.Subject;


public class ObserveQueryManager<T extends Model> implements Cancelable {
    private final Subject<StorageItemChange<? extends Model>> itemChangeSubject;
    private final SqlQueryProcessor sqlQueryProcessor;
    private final ExecutorService threadPool;
    private Disposable disposable;
    private boolean isCanceled = false;
    private final List<DataStoreItemChange<T>> changedItemList = new ArrayList<DataStoreItemChange<T>>();
    private final List<T> completeItemList = new ArrayList<T>();

    private long startTime;
    private int MAX_RECORDS = 1000;
    private int MAX_TIME_SEC = 2;


//TODOPM: sort complete list ans set sync status
    public ObserveQueryManager(Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                               SqlQueryProcessor sqlQueryProcessor,
                               ExecutorService threadPool ){
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
    }

    public ObserveQueryManager(Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                               SqlQueryProcessor sqlQueryProcessor,
                               ExecutorService threadPool,
                               int maxRecords,
                               int maxSecs){
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
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
            setStartTimeWhenNeeded();
            @SuppressWarnings("unchecked")   StorageItemChange<T> itemChanged = (StorageItemChange<T>) value;
            completeItemList.add(itemChanged.item());
            collect(itemChanged, onQuerySnapshot);
        };
        threadPool.submit(() -> {
            List<T> models =  sqlQueryProcessor.queryOfflineData(itemClass, options, onObservationError);
            completeItemList.addAll(models);
            DataStoreQuerySnapshot<T> dataStoreQuerySnapshot = new DataStoreQuerySnapshot<T>(completeItemList, false, changedItemList );
            getListConsumer(onQuerySnapshot).accept(dataStoreQuerySnapshot);
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


    private void collect(StorageItemChange<T> changedItem, @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot) {
        try {
            changedItemList.add( ItemChangeMapper.map(changedItem));
            if (changedItemList.size()>= MAX_RECORDS || (Instant.now().getEpochSecond()- startTime) >= MAX_TIME_SEC){
                DataStoreQuerySnapshot<T> dataStoreQuerySnapshot = new DataStoreQuerySnapshot<T>(completeItemList, false, changedItemList );
                getListConsumer(onQuerySnapshot).accept(dataStoreQuerySnapshot);
                changedItemList.clear();
                startTime = 0;
            }
        } catch (DataStoreException e) {
            e.printStackTrace();
        }
    }

    private void setStartTimeWhenNeeded(){
        if (startTime == 0){
            startTime = Instant.now().getEpochSecond();
        }
    }

    @NonNull
    private Consumer<DataStoreQuerySnapshot<T>> getListConsumer(@NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot) {
        return value -> {
            if (isCanceled) return;
            onQuerySnapshot.accept(value);
        };
    }

}
