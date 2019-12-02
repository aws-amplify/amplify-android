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

package com.amplifyframework.analytics;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;

/**
 * Exception thrown by Analytics category plugins.
 */
public final class AnalyticsException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param throwable The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AnalyticsException(
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
    public AnalyticsException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion
    ) {
        super(message, recoverySuggestion);
    }
}
