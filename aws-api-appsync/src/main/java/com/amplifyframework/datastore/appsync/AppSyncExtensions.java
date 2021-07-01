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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.util.Immutable;

import java.util.Map;

/**
 * Model representing a AppSync specific extensions map.  This data may be included on a
 * GraphQLResponse.Error object, and is represented as a Map&lt;String, Object&gt;.  This class can be
 * used to create a strongly typed model by passing the Map&lt;String, Object&gt; to the constructor.
 */
public final class AppSyncExtensions {
    private static final String ERROR_TYPE_KEY = "errorType";
    private static final String ERROR_INFO_KEY = "errorInfo";
    private static final String DATA_KEY = "data";

    private final AppSyncErrorType errorType;
    private final String errorInfo;
    private final Map<String, Object> data;

    /**
     * Constructs an {@link AppSyncExtensions} from extensions data found via
     * {@link GraphQLResponse.Error#getExtensions()}.
     * @param extensions As from {@link GraphQLResponse.Error#getExtensions()}
     */
    @SuppressWarnings("unchecked")
    public AppSyncExtensions(Map<String, Object> extensions) {
        this.errorType = AppSyncErrorType.enumerate((String) extensions.get(ERROR_TYPE_KEY));
        this.errorInfo = (String) extensions.get(ERROR_INFO_KEY);
        this.data = (Map<String, Object>) extensions.get(DATA_KEY);
    }

    /**
     * Constructs {@link AppSyncExtensions} from the a specific set of fields that
     * is AppSync is known to send in a GraphQL response. Unlike {@link #AppSyncExtensions(Map)},
     * this constructor makes no assumptions about how the extension data was identified/obtained.
     * @param errorType The type of error, as described by AppSync
     * @param errorInfo Info about the error, as described by AppSync
     * @param data Additional error data, as defined by AppSync
     */
    public AppSyncExtensions(String errorType, String errorInfo, Map<String, Object> data) {
        this.errorType = AppSyncErrorType.enumerate(errorType);
        this.errorInfo = errorInfo;
        this.data = data;
    }

    /**
     * Returns the specific AppSync error type, often related to handling conflicts.
     * https://docs.aws.amazon.com/appsync/latest/devguide/conflict-detection-and-sync.html#errors
     *
     * @return errorType
     */
    @Nullable
    public AppSyncErrorType getErrorType() {
        return errorType;
    }

    /**
     * Returns more info about the error.
     *
     * @return errorInfo
     */
    @Nullable
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * For conflict unhandled errors, returns a map containing the same fields as the model type.
     *
     * @return data
     */
    @Nullable
    public Map<String, Object> getData() {
        return Immutable.of(data);
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AppSyncExtensions that = (AppSyncExtensions) thatObject;

        return ObjectsCompat.equals(this.errorType, that.errorType) &&
            ObjectsCompat.equals(this.errorInfo, that.errorInfo) &&
            ObjectsCompat.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        int result = errorType.hashCode();
        result = 31 * result + (errorInfo != null ? errorInfo.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "AppSyncExtensions{" +
            "errorType='" + errorType + '\'' +
            ", errorInfo='" + errorInfo + '\'' +
            ", data=" + data +
            '}';
    }

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
        CONFLICT_UNHANDLED("ConflictUnhandled"),

        /**
         * This error is not for general use unless you have consulted directly with AWS.  When DataStore encounters
         * this error, it will ignore the error and allow DataStore to continue running.  This error is subject to be
         * deprecated/removed in the future.
         */
        OPERATION_DISABLED("OperationDisabled"),

        /**
         * An Unauthorized error will occur if the provided credentials are not authorized for the requested operation.
         */
        UNAUTHORIZED("Unauthorized");

        private final String value;

        AppSyncErrorType(String value) {
            this.value = value;
        }

        /**
         * Gets the string value of the error type.
         * Note that this returns a different string from {@link #toString()},
         * which is used for debugging, not as a field accessor.
         * @return Error type as a string
         */
        @NonNull
        public String getValue() {
            return value;
        }

        /**
         * Enumerate an error type from a string.
         * @param maybeMatch A possibly matching error type
         * @return An AppSyncErrorType if the provided string matches a known error type,
         *         otherwise, null.
         */
        @Nullable
        public static AppSyncErrorType enumerate(@Nullable String maybeMatch) {
            for (AppSyncErrorType value : AppSyncErrorType.values()) {
                if (value.getValue().equals(maybeMatch)) {
                    return value;
                }
            }
            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return "AppSyncErrorType{" +
                "value='" + value + '\'' +
                '}';
        }
    }
}
