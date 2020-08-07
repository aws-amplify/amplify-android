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

package com.amplifyframework.datastore.syncengine.events;

/**
 * Event payload emitted when the sync process starts.
 */
public final class SyncQueriesStartedEvent {
    private String[] models;

    /**
     * Constructs a SyncQueriesStartedEvent object.
     * @param models An array of model names.
     */
    public SyncQueriesStartedEvent(String[] models) {
        this.models = models;
    }

    /**
     * Getter for the list of models in the initial sync.
     * @return An array with the list of models (Ex. [Post, Blog]).
     */
    public String[] getModels() {
        return models;
    }
}
