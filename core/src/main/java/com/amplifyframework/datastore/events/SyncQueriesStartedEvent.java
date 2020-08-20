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

import java.util.Arrays;

/**
 * Event payload emitted when the sync process starts.
 */
public final class SyncQueriesStartedEvent {
    private final String[] models;

    /**
     * Constructs a SyncQueriesStartedEvent object.
     * @param models An array of model names.
     */
    public SyncQueriesStartedEvent(String[] models) {
        this.models = Arrays.copyOf(models, models.length);
    }

    /**
     * Getter for the list of models in the initial sync.
     * @return An array with the list of models (Ex. [Post, Blog]).
     */
    public String[] getModels() {
        return models;
    }

    @NonNull
    @Override
    public String toString() {
        return "SyncQueriesStartedEvent{" +
            "models=" + Arrays.toString(models) +
            '}';
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(models);
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SyncQueriesStartedEvent that = (SyncQueriesStartedEvent) thatObject;
        return Arrays.equals(models, that.models);
    }
}
