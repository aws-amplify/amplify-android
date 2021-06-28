/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown by DataStore category plugins.
 */
public class DataStoreException extends AmplifyException {

    private static final long serialVersionUID = 1L;
    private ErrorType type = ErrorType.RECOVERABLE_ERROR;
    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param throwable The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public DataStoreException(
            @NonNull final String message,
            final Throwable throwable,
            @NonNull final String recoverySuggestion
    ) {
        super(message, throwable, recoverySuggestion);
    }

    public DataStoreException(
            @NonNull final String message,
            final Throwable throwable,
            @NonNull final String recoverySuggestion,
            ErrorType type
    ) {
        super(message, throwable, recoverySuggestion);
        this.type = type;
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     * @param type ErrorType suggesting if its a recoverable error
     */
    public DataStoreException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion,
            ErrorType type
    ) {
        super(message, recoverySuggestion);
        this.type = type;
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public DataStoreException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion
    ) {
        super(message, recoverySuggestion);
        this.type = ErrorType.RECOVERABLE_ERROR;
    }

    public ErrorType getType() {
        return type;
    }

    /**
     * Exception thrown by DataStore category plugins used to represent a GraphQLResponse containing errors.
     */
    public static final class GraphQLResponseException extends DataStoreException {
        private static final long serialVersionUID = 1L;

        private final List<GraphQLResponse.Error> errors;

        /**
         * Constructs a new exception using the provided message and list of errors.
         * @param message Explains the reason for the exception
         * @param errors List of errors from GraphQLResponse
         */
        public GraphQLResponseException(String message, @NonNull List<GraphQLResponse.Error> errors) {
            super(message, "See attached list of GraphQLResponse.Error objects.",
                    ErrorType.IRRECOVERABLE_ERROR);
            this.errors = Objects.requireNonNull(errors);
        }

        /**
         * Returns the errors.
         * @return the errors.
         */
        @NonNull
        public List<GraphQLResponse.Error> getErrors() {
            return Immutable.of(errors);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            if (!super.equals(object)) {
                return false;
            }
            GraphQLResponseException that = (GraphQLResponseException) object;
            return ObjectsCompat.equals(errors, that.errors);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(super.hashCode(), errors);
        }

        @Override
        public String toString() {
            return "GraphQLResponseException{" +
                    "message=" + getMessage() +
                    ", errors=" + errors +
                    ", recoverySuggestion=" + getRecoverySuggestion() +
                    '}';
        }
    }
}
