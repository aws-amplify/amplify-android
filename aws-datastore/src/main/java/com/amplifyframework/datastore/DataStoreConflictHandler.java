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

/**
 * Handles conflicts between models observed during model synchronization in the DataStore.
 * Such conflicts may arise between version of models kept locally, and competing versions
 * found in the remote AppSync system.
 */
public interface DataStoreConflictHandler {
    /**
     * Resolves a DataStore conflict.
     * @param conflictData Data about the conflict.
     * @param onResult A callback that should be invoked when the conflict handling is complete.
     * @param <T> The type of model for which a conflict was observed
     */
    <T extends Model> void resolveConflict(
        @NonNull DataStoreConflictData<T> conflictData,
        @NonNull Consumer<DataStoreConflictHandlerResult> onResult);
}
