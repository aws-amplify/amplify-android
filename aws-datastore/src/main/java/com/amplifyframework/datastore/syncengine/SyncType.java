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

import com.amplifyframework.util.Time;

/**
 * Enum representing the type of initial sync.
 */
public enum SyncType {
    /**
     * Hydrate local store by retrieving the entire remote dataset.
     */
    BASE,

    /**
     * Hydrate local store by retrieving only a subset of the remote dataset.
     */
    DELTA;

    /**
     * Returns the type of sync based on when the last sync was executed and
     * the configured sync interval.
     * @param lastSyncTime Timestamp of when the last sync was executed.
     * @param syncIntervalMs The maximum elapsed time before forcing a full sync.
     * @return {@link #BASE} if (now - lastSyncTime) > syncIntervalMs, {@link #DELTA} otherwise.
     */
    public static SyncType fromSyncTimeAndThreshold(SyncTime lastSyncTime, long syncIntervalMs) {
        if (SyncTime.never().equals(lastSyncTime)) {
            return BASE;
        }
        long timeSinceLastSync = Time.now() - lastSyncTime.toLong();
        return timeSinceLastSync > syncIntervalMs ? BASE : DELTA;
    }
}
