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

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.syncengine.LastSyncMetadata;
import com.amplifyframework.util.Time;

import java.util.List;
import java.util.concurrent.TimeUnit;

/***
 * Class which returns the sync status of the local datastore.
 */
public class SyncStatus {

    private final SqlQueryProcessor sqlQueryProcessor;
    private final DataStoreConfiguration dataStoreConfiguration;

    /***
     * Provides sync status of the local datastore.
     * @param sqlQueryProcessor sql query processor.
     * @param dataStoreConfiguration data store configuration.
     */
    public SyncStatus(SqlQueryProcessor sqlQueryProcessor, DataStoreConfiguration dataStoreConfiguration) {
        this.sqlQueryProcessor = sqlQueryProcessor;
        this.dataStoreConfiguration = dataStoreConfiguration;
    }

    /***
     * Method returns sync status.
     * @param modelClassName model class name.
     * @param onObservationError invoked on error.
     * @return returns the sync status of true or false.
     */
    public boolean get(@NonNull String modelClassName,
                       @NonNull Consumer<DataStoreException> onObservationError) {
        LastSyncMetadata lastSyncMetadata;
        boolean syncStatus = false;
        try {
            lastSyncMetadata = getLastSyncMetaData(modelClassName, onObservationError);
            if (lastSyncMetadata.getLastSyncTime() != null) {
                syncStatus = (Time.now() - lastSyncMetadata.getLastSyncTime())
                        < TimeUnit.MINUTES.toMillis(dataStoreConfiguration.getSyncIntervalInMinutes());
            }
        } catch (DataStoreException exception) {
            onObservationError.accept(exception);
        }
        return syncStatus;
    }

    private LastSyncMetadata getLastSyncMetaData(@NonNull String modelClassName,
                                                 @NonNull Consumer<DataStoreException> onObservationError)
            throws DataStoreException {
        QueryPredicate hasMatchingModelClassName = QueryField.field("modelClassName").eq(modelClassName);
        List<LastSyncMetadata> syncedList = sqlQueryProcessor.queryOfflineData(LastSyncMetadata.class,
                                                             Where.matches(hasMatchingModelClassName),
                                                             onObservationError);
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
