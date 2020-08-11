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

/**
 * Hub event payload emitted when the initial sync completes for a given model.
 */
public final class ModelSyncedEvent {
    private final int added;
    private final int updated;
    private final int deleted;
    private final String model;
    private final boolean isFullSync;
    private final boolean isDeltaSync;

    /**
     * Constructs a ModelSyncEvent object.
     * @param model The of the model.
     * @param syncType Indicates whether is a full or delta sync.
     * @param added Number of records added during the sync attempt.
     * @param updated Number of records updated during the sync attempt.
     * @param deleted Number of records deleted during the sync attempt.
     */
    public ModelSyncedEvent(String model,
                            boolean isFullSync,
                            int added,
                            int updated,
                            int deleted) {
        this.added = added;
        this.updated = updated;
        this.deleted = deleted;
        this.model = model;
        this.isFullSync = isFullSync;
        this.isDeltaSync = !isFullSync;
    }

    /**
     * Getter for the model name.
     * @return The model name (ex. Post).
     */
    public String getModel() {
        return model;
    }

    /**
     * Getter for the number of records added during the sync.
     * @return Number of records added.
     */
    public int getAdded() {
        return added;
    }

    /**
     * Getter for the number of records updated during the sync.
     * @return Number of records updated.
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * Getter for the number of records deleted during the sync.
     * @return Number of records deleted.
     */
    public int getDeleted() {
        return deleted;
    }

    /**
     * Getter for the isFullSync property.
     * @return True if it's a full sync, false otherwise.
     */
    public boolean isFullSync() {
        return isFullSync;
    }

    /**
     * Getter for the isDeltaSync property.
     * @return True if it's a delta sync, false otherwise.
     */
    public boolean isDeltaSync() {
        return isDeltaSync;
    }

    @NonNull
    @Override
    public String toString() {
        return "ModelSyncedEvent{" +
            "model=" + model +
            ", added=" + added +
            ", updated=" + updated +
            ", deleted=" + deleted +
            '}';
    }
}
