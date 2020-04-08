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

package com.amplifyframework;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Top-level exception in the Amplify framework. All other Amplify exceptions should extend this.
 */
public class AmplifyException extends Exception {
    /**
     * All Amplify Exceptions should have a recovery suggestion. This string can be used as a filler until one is
     * defined but should ultimately be replaced.
     */
    public static final String TODO_RECOVERY_SUGGESTION = "Sorry, we don't have a suggested fix for this error yet.";
    private static final long serialVersionUID = 1L;

    private final String recoverySuggestion;

    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param cause The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AmplifyException(
            @NonNull final String message,
            @NonNull final Throwable cause,
            @NonNull final String recoverySuggestion
    ) {
        super(message, cause);
        this.recoverySuggestion = Objects.requireNonNull(recoverySuggestion);
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AmplifyException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion) {
        super(message);
        this.recoverySuggestion = Objects.requireNonNull(recoverySuggestion);
    }

    /**
     * Gets the recovery suggestion message.
     * @return customized recovery suggestion message
     */
    @NonNull
    public final String getRecoverySuggestion() {
        return recoverySuggestion;
    }
}
