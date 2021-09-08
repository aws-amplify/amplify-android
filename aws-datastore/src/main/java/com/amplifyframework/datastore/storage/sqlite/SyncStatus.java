package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.syncengine.LastSyncMetadata;
import com.amplifyframework.util.Time;
import java.util.List;

public class SyncStatus {

    private final SqlQueryProcessor sqlQueryProcessor;
    private final DataStoreConfiguration dataStoreConfiguration;

    public SyncStatus(SqlQueryProcessor sqlQueryProcessor, DataStoreConfiguration dataStoreConfiguration){
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.dataStoreConfiguration = dataStoreConfiguration;
    }

    public boolean get(@NonNull String modelClassName, @NonNull Consumer<DataStoreException> onObservationError) throws DataStoreException {
        LastSyncMetadata lastSyncMetadata = getLastSyncMetaData(modelClassName, onObservationError);
        boolean syncStatus;
        if (lastSyncMetadata.getLastSyncType().equals("Base") || lastSyncMetadata.getLastSyncTime()==null){
            syncStatus = false;
        } else {
            syncStatus = (Time.now() - lastSyncMetadata.getLastSyncTime())
                    < (dataStoreConfiguration.getSyncIntervalInMinutes() * 60000);
        }
        return syncStatus;
    }

    private LastSyncMetadata getLastSyncMetaData(@NonNull String modelClassName,
                                                 @NonNull Consumer<DataStoreException> onObservationError) throws DataStoreException {
        QueryPredicate hasMatchingModelClassName = QueryField.field("modelClassName").eq(modelClassName);
        List<LastSyncMetadata> syncedList = sqlQueryProcessor.queryOfflineData(LastSyncMetadata.class, Where.matches(hasMatchingModelClassName), onObservationError);
        return extractSingleResult(modelClassName, syncedList);
    }

    private LastSyncMetadata extractSingleResult(String modelClassName,
                                                 List<LastSyncMetadata> lastSyncMetadata)
            throws DataStoreException {
        if (lastSyncMetadata.size() > 1) {
            throw new DataStoreException(
                    "Wanted 1 sync time for model = " + modelClassName + ", but found " + lastSyncMetadata.size() + ".",
                    "This is likely a bug. Please report it to AWS."
            );
        } else if (lastSyncMetadata.size() == 1) {
            return lastSyncMetadata.get(0);
        } else {
            return LastSyncMetadata.neverSynced(modelClassName);
        }
    }
}
