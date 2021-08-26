package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import java.util.Objects;


public class ObserveQueryManager implements Cancelable {
    private LocalStorageAdapter sqLiteStorageAdapter;
    private Cancelable sqlAdapterCancelable;

    public ObserveQueryManager(LocalStorageAdapter sqLiteStorageAdapter ){
        this.sqLiteStorageAdapter = sqLiteStorageAdapter;
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
        sqlAdapterCancelable = sqLiteStorageAdapter.observeQuery(itemClass, options, onObservationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
    }


    @Override
    public void cancel() {
        if (sqlAdapterCancelable != null){
            sqlAdapterCancelable.cancel();
        }
    }
}
