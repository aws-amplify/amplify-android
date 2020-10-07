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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An enumeration of the various error types that we expect
 * to see in the value of {@link AppSyncExtensions#getErrorType()}.
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/conflict-detection-and-sync.html#errors">
 *     AppSync Conflict Detection & Resolution Errors
 *     </a>
 */
public enum AppSyncErrorType {
    /**
     * Conflict detection finds a version mismatch and the conflict handler rejects the mutation.
     * Example: Conflict resolution with an Optimistic Concurrency conflict handler.
     * Or, Lambda conflict handler returned with REJECT.
     */
    CONFLICT_UNHANDLED("ConflictUnhandled");

    private final String errorType;

    AppSyncErrorType(String errorType) {
        this.errorType = errorType;
    }

    /**
     * Gets the error type string.
     * @return Error type string
     */
    @NonNull
    public String getErrorType() {
        return errorType;
    }

    /**
     * Enumerate an error type from a string.
     * @param maybeMatch A possibly matching error type
     * @return An AppSyncErrorType if the provided string matches a known error type,
     *         otherwise, null.
     */
    @Nullable
    public static AppSyncErrorType fromErrorType(@Nullable String maybeMatch) {
        for (AppSyncErrorType value : values()) {
            if (value.getErrorType().equals(maybeMatch)) {
                return value;
            }
        }
        return null;
    }
}
