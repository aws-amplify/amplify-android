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

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.syncengine.ConflictData;
import com.amplifyframework.datastore.syncengine.ConflictResolutionDecision;

/**
 * A collection of factory methods to create some default conflict handlers.
 */
public final class DataStoreConflictHandlers {
    private DataStoreConflictHandlers() {}

    /**
     * Factory to obtain a handler that always applies the remote copy
     * of the data.
     * @return A DataStore conflict handler that always applies the remote
     *         copy of the data.
     */
    public static DataStoreConflictHandler alwaysApplyRemote() {
        return new AlwaysApplyRemoteHandler();
    }

    /**
     * Factory to obtain a handler that always retries the local copy
     * of the data.
     * @return A DataStore conflict handler that always retries the
     *         local copy of the data.
     */
    public static DataStoreConflictHandler alwaysRetryLocal() {
        return new AlwaysRetryLocalHandler();
    }

    /**
     * An implementation of the {@link DataStoreConflictHandler} that
     * always decides to apply the remote copy of the data.
     */
    public static final class AlwaysApplyRemoteHandler implements DataStoreConflictHandler {
        @Override
        public <T extends Model> void onConflictDetected(
                @NonNull ConflictData<T> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<T>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.applyRemote());
        }
    }

    /**
     * An implementation of the {@link DataStoreConflictHandler} that
     * always decides to retry the local copy of the data.
     */
    public static final class AlwaysRetryLocalHandler implements DataStoreConflictHandler {
        @Override
        public <T extends Model> void onConflictDetected(
                @NonNull ConflictData<T> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<T>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.retryLocal());
        }
    }
}
