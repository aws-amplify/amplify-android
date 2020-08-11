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

package com.amplifyframework.datastore.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

/**
 * Event payload for the {@link com.amplifyframework.datastore.DataStoreChannelEventName#NETWORK_STATUS} event.
 */
public final class NetworkStatusEvent {
    private final boolean active;

    /**
     * Constructs a {@link NetworkStatusEvent} object.
     * @param isActive True if connection is active, false otherwise.
     */
    public NetworkStatusEvent(boolean isActive) {
        this.active = isActive;
    }

    /**
     * Getter for the active field.
     * @return The value of the active field.
     */
    public boolean getActive() {
        return active;
    }

    @Override
    public int hashCode() {
        int result = Boolean.valueOf(active).hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        NetworkStatusEvent that = (NetworkStatusEvent) thatObject;
        return ObjectsCompat.equals(active, that.active);
    }

    @NonNull
    @Override
    public String toString() {
        return "NetworkStatus{active=" + active + "}";
    }
}
