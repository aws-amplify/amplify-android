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

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Time;

/**
 * Represents the time at which a type of model was last sync'd.
 * Why isn't that just an @Nullable Long (capital 'L')?
 * Rx doesn't allow nulls, so we have to encapsulate the Long value into this
 * class. TODO: move this to become a static inner class of {@link LastSyncMetadata}?
 */
final class SyncTime {
    private final Long time;

    private SyncTime(Long time) {
        this.time = time;
    }

    @SuppressWarnings("unused")
    static SyncTime at(long time) {
        return new SyncTime(time);
    }

    @SuppressWarnings("unused")
    static SyncTime never() {
        return new SyncTime(null);
    }

    static SyncTime from(@Nullable Long lastSyncTime) {
        return new SyncTime(lastSyncTime);
    }

    public static SyncTime now() {
        return new SyncTime(Time.now());
    }

    boolean exists() {
        return time != null;
    }

    long toLong() {
        if (time == null) {
            throw new IllegalStateException("No last sync time!");
        }
        return time;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SyncTime that = (SyncTime) thatObject;

        return ObjectsCompat.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return time != null ? time.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "LastSyncTime{" +
            "time=" + time +
            '}';
    }
}
