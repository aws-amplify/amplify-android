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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange.Initiator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

final class SyncTimeRegistry {
    private final LocalStorageAdapter localStorageAdapter;

    SyncTimeRegistry(LocalStorageAdapter localStorageAdapter) {
        this.localStorageAdapter = localStorageAdapter;
    }

    Single<SyncTime> lookupLastSyncTime(@NonNull String modelClassName) {
        return Single.create(emitter -> {
            QueryPredicate hasMatchingModelClassName = QueryField.field("modelClassName").eq(modelClassName);

            localStorageAdapter.query(LastSyncMetadata.class, Where.matches(hasMatchingModelClassName), results -> {
                try {
                    LastSyncMetadata syncMetadata = extractSingleResult(modelClassName, results);
                    emitter.onSuccess(SyncTime.from(syncMetadata.getLastSyncTime()));
                } catch (DataStoreException queryResultFailure) {
                    emitter.onError(queryResultFailure);
                }
            }, emitter::onError);
        });
    }

    Completable saveLastDeltaSyncTime(@NonNull String modelClassName, @Nullable SyncTime syncTime) {
        LastSyncMetadata metadata = syncTime != null && syncTime.exists() ?
            LastSyncMetadata.deltaSyncedAt(modelClassName, syncTime.toLong()) :
            LastSyncMetadata.neverSynced(modelClassName);

        return Completable.create(emitter ->
            localStorageAdapter.save(
                metadata,
                Initiator.SYNC_ENGINE,
                QueryPredicates.all(),
                saveResult -> emitter.onComplete(),
                emitter::onError
            )
        );
    }

    Completable saveLastBaseSyncTime(@NonNull String modelClassName, @Nullable SyncTime syncTime) {
        LastSyncMetadata metadata = syncTime != null && syncTime.exists() ?
            LastSyncMetadata.baseSyncedAt(modelClassName, syncTime.toLong()) :
            LastSyncMetadata.neverSynced(modelClassName);

        return Completable.create(emitter ->
            localStorageAdapter.save(
                metadata,
                Initiator.SYNC_ENGINE,
                QueryPredicates.all(),
                saveResult -> emitter.onComplete(),
                emitter::onError
            )
        );
    }

    private LastSyncMetadata extractSingleResult(
            String modelClassName, Iterator<LastSyncMetadata> metadataIterator) throws DataStoreException {
        final List<LastSyncMetadata> lastSyncMetadata = new ArrayList<>();
        while (metadataIterator.hasNext()) {
            lastSyncMetadata.add(metadataIterator.next());
        }
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
