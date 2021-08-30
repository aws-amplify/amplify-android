package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.Subject;


public class ObserveQueryManager implements Cancelable {
    private Cancelable sqlAdapterCancelable;
    private Subject<StorageItemChange<? extends Model>> itemChangeSubject;
    private SqlQueryProcessor sqlQueryProcessor;
    private ExecutorService threadPool;
    private Disposable disposable;
    private boolean isCanceled = false;
    private boolean queryInProgress = false;

    public ObserveQueryManager(Subject<StorageItemChange<? extends Model>> itemChangeSubject,
                               SqlQueryProcessor sqlQueryProcessor,
                               ExecutorService threadPool ){
        this.itemChangeSubject = itemChangeSubject;
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.threadPool = threadPool;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    public <T extends Model> void observeQuery(
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
        Consumer<List<T>> onSuccess = value -> {
            if (isCanceled) return;
            DataStoreQuerySnapshot<T> dataStoreQuerySnapshot = new DataStoreQuerySnapshot<T>(value, false, null );
            onQuerySnapshot.accept(dataStoreQuerySnapshot);
        };
        // TODOPM: add debounce  + collect by time or number of records and filter
        // cancelable on sqlQueryProcessor
        Consumer<StorageItemChange<? extends Model>> onItemChanged = value -> {
            DataStoreQuerySnapshot<T> dataStoreQuerySnapshot = new DataStoreQuerySnapshot<T>(null, false, null );
            onQuerySnapshot.accept(dataStoreQuerySnapshot);
        };
        threadPool.submit(() -> {
            List<T> models = sqlQueryProcessor.queryOfflineData(itemClass, options, onObservationError);
            onSuccess.accept(models);
        });

        disposable = itemChangeSubject
                //.collect()
                //.filter()
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
}
