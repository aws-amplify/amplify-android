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

package com.amplifyframework.api;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown by API category plugins.
 */
public class ApiException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param throwable The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public ApiException(
            @NonNull final String message,
            final Throwable throwable,
            @NonNull final String recoverySuggestion
    ) {
        super(message, throwable, recoverySuggestion);
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public ApiException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion
    ) {
        super(message, recoverySuggestion);
    }

    /**
     * This type of exception should not be retried.
     */
    public static final class NonRetryableException extends ApiException {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for NonRetryable Exception.
         * @param message message for exception.
         * @param recoverySuggestion recovery suggestions.
         */
        public NonRetryableException(@NonNull @NotNull String message, @NotNull String recoverySuggestion) {

            super(message, recoverySuggestion);
        }
    }

    /**
     * Represents authn/authz errors as it relates to interacting with the API backend.
     */
    public static final class ApiAuthException extends ApiException {
        private static final long serialVersionUID = 1L;

        /**
         * Public constructor that accepts an exception to be used as a cause.
         * @param message message Explains the reason for the exception.
         * @param throwable An exception to be used as a cause.
         * @param recoverySuggestion Text suggesting a way to recover from the error being described.
         */
        public ApiAuthException(@NonNull String message,
                                Throwable throwable,
                                @NonNull String recoverySuggestion) {
            super(message, throwable, recoverySuggestion);
        }

        /**
         * Public constructor that takes a message and a recovery suggestion.
         * @param message message Explains the reason for the exception.
         * @param recoverySuggestion Text suggesting a way to recover from the error being described.
         */
        public ApiAuthException(@NonNull String message, @NonNull String recoverySuggestion) {
            super(message, recoverySuggestion);
        }
    }
}
