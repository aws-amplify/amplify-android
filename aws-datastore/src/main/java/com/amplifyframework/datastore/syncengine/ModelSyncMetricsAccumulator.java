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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.events.ModelSyncedEvent;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that encapsulates the logic of keeping track of sync metrics
 * by operation type for a given model.
 */
final class ModelSyncMetricsAccumulator {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final Map<StorageItemChange.Type, AtomicInteger> syncMetrics;
    private final String modelClassName;

    /**
     * Constructor that sets up an observer to watch for mutations
     * made by the sync process on the local DataStore.
     * @param modelClassName Name of the model for sync.
     */
    ModelSyncMetricsAccumulator(String modelClassName) {
        syncMetrics = new ConcurrentHashMap<>();
        syncMetrics.put(StorageItemChange.Type.CREATE, new AtomicInteger(0));
        syncMetrics.put(StorageItemChange.Type.UPDATE, new AtomicInteger(0));
        syncMetrics.put(StorageItemChange.Type.DELETE, new AtomicInteger(0));
        this.modelClassName = modelClassName;
    }

    /**
     * Creates an instance to {@link ModelSyncedEvent} using the existing metrics.
     * @param syncType The sync type (BASE or DELTA) to be emitted with the event.
     * @return An instance of {@link ModelSyncedEvent}.
     */
    public ModelSyncedEvent toModelSyncedEvent(SyncType syncType) {
        return new ModelSyncedEvent(modelClassName,
                                    SyncType.BASE.equals(syncType),
                                    syncMetrics.get(StorageItemChange.Type.CREATE).get(),
                                    syncMetrics.get(StorageItemChange.Type.UPDATE).get(),
                                    syncMetrics.get(StorageItemChange.Type.DELETE).get());
    }

    /**
     * Increments the counter for a given change type.
     * @param itemChange The change type to increment.
     */
    public void increment(StorageItemChange<? extends Model> itemChange) {
        syncMetrics.get(itemChange.type()).incrementAndGet();
    }
}
