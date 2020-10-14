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
 * A hook to handle a conflict between local and remote copies of a model.
 * The DataStore customer can implement their own version of this handler,
 * and provide that handler to the {@link DataStoreConfiguration} while constructing
 * the DataStore plugin using
 * {@link AWSDataStorePlugin#AWSDataStorePlugin(DataStoreConfiguration)}.
 */
public interface DataStoreConflictHandler {
    /**
     * This callback method is invoked when the DataStore detects a conflict between
     * a local and remote version of a model instance. Such a conflict may occur
     * while the system is trying to upload a local change to the remote system.
     *
     * All code paths in the implementation of this handler must terminate by
     * calling the provided consumer with a conflict resolution decision.
     *
     * @param conflictData Data about the conflict, including the local and remote
     *                     copies of the model that are in conflict
     * @param decisionConsumer An implementation of the DataStoreConflictHandler must
     *                         end by invoking one of the resolutions on this handle.
     * @param <T> The type of model for which a conflict was observed
     */
    <T extends Model> void onConflictDetected(
            @NonNull ConflictData<T> conflictData,
            @NonNull Consumer<ConflictResolutionDecision<T>> decisionConsumer);
}
