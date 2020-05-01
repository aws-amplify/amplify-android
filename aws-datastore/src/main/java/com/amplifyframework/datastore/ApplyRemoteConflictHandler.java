/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * A default implementation of the {@link DataStoreConflictHandler},
 * which discards the local data, in favor of whatever was on the server.
 */
public final class ApplyRemoteConflictHandler implements DataStoreConflictHandler {
    private final DataStoreErrorHandler dataStoreErrorHandler;

    private ApplyRemoteConflictHandler(DataStoreErrorHandler dataStoreErrorHandler) {
        this.dataStoreErrorHandler = dataStoreErrorHandler;
    }

    /**
     * Creates a new instance of the {@link ApplyRemoteConflictHandler}.
     * This handler discards local data in preference of remote data.
     * @param dataStoreErrorHandler Handler of unrecoverable errors
     * @return A {@link ApplyRemoteConflictHandler}
     */
    @NonNull
    public static ApplyRemoteConflictHandler instance(@NonNull DataStoreErrorHandler dataStoreErrorHandler) {
        Objects.requireNonNull(dataStoreErrorHandler);
        return new ApplyRemoteConflictHandler(dataStoreErrorHandler);
    }

    @Override
    public <T extends Model> void resolveConflict(
            @NonNull DataStoreConflictData<T> conflictData,
            @NonNull Consumer<DataStoreConflictHandlerResult> onConflictResolved) {
        final T local = conflictData.getLocal();
        final T remote = conflictData.getRemote();
        Amplify.DataStore.delete(local,
            deleted -> Amplify.DataStore.save(remote,
                saved -> onConflictResolved.accept(DataStoreConflictHandlerResult.APPLY_REMOTE),
                dataStoreErrorHandler
            ),
            dataStoreErrorHandler
        );
    }
}
