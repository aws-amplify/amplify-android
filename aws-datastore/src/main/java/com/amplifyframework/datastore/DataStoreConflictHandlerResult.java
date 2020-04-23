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

/**
 * The possible results from a conflict resolution. Either the local data was discarded, kept, or
 * some new model instance is used which neither purely local or remote in origin.
 */
public enum DataStoreConflictHandlerResult {

    /**
     * Conflict handled by discarding the local (client-side) changes, preferring whatever
     * was on the server.
     */
    APPLY_REMOTE,

    /**
     * The conflict handled by retrying the local change which attempts to persist
     * it again on the backend.
     */
    RETRY_LOCAL,

    /**
     * Conflict handled by returning a new `Model` instance that should be used
     * instead of the local and remote changes.
     */
    RETRY
}
