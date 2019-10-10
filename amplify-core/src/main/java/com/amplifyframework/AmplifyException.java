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

import androidx.annotation.Nullable;

/**
 * Top-level compile-time exception in the Amplify System. Any compile-time
 * exception in Amplify should derive from this exception.
 */
public class AmplifyException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String recoverySuggestion;
    private final boolean isRetryable;

    /**
     * Creates a new AmplifyException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     * @param recoverySuggestion An optional text suggesting a way to recover
     *                           from the error being described
     * @param isRetryable A boolean indicating whether or not this exception
     *                    indicates a recoverable state, or if a consumer
     *                    should not retry the operation that failed
     */
    public AmplifyException(
            final String message,
            final Throwable throwable,
            @Nullable final String recoverySuggestion,
            final boolean isRetryable) {
        super(message, throwable);
        this.recoverySuggestion = recoverySuggestion;
        this.isRetryable = isRetryable;
    }

    /**
     * Constructs a new AmplifyException using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param throwable An error associated with this exception
     */
    public AmplifyException(final String message, final Throwable throwable) {
        this(message, throwable, null, false);
    }

    /**
     * Creates a new AmplifyException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public AmplifyException(final String message) {
        this(message, null, null, false);
    }

    /**
     * Create an AmplifyException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public AmplifyException(final Throwable throwable) {
        this(null, throwable);
    }

    /**
     * Gets the optional recovery suggestion message.
     * @return customized recovery suggestion message
     */
    public final String getRecoverySuggestion() {
        return recoverySuggestion;
    }

    /**
     * Returns a hint as to whether it makes sense to retry upon this exception.
     * Default is true, but subclass may override.
     * @return true if it is retryable.
     */
    public final boolean isRetryable() {
        return isRetryable;
    }
}

