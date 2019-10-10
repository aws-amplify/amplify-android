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
 * Top-level run-time exception in the Amplify System. Any run-time
 * exception in Amplify should derive from this exception.
 */
public class AmplifyRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String recoverySuggestion;
    private final boolean isRetryable;

    /**
     * Creates a new AmplifyRuntimeException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     * @param recoverySuggestion An optional text containing a hint on how
     *                           the user might recover from the current error
     * @param isRetryable A boolean indicating whether or not a consumer
     *                    should retry the operation which raised this exception,
     *                    or alternately, if the exception is not recoverable
     */
    public AmplifyRuntimeException(
            final String message,
            final Throwable throwable,
            @Nullable final String recoverySuggestion,
            final boolean isRetryable) {
        super(message, throwable);
        this.recoverySuggestion = recoverySuggestion;
        this.isRetryable = isRetryable;
    }

    /**
     * Constructs a new AmplifyRuntimeException.
     * @param message A message explaining why the exception occurred
     * @param throwable An associated error, perhaps a cause of this exception
     */
    public AmplifyRuntimeException(final String message, final Throwable throwable) {
        this(message, throwable, null, false);
    }

    /**
     * Creates a new AmplifyRuntimeException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public AmplifyRuntimeException(final String message) {
        this(message, null, null, false);
    }

    /**
     * Create an AmplifyRuntimeException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public AmplifyRuntimeException(final Throwable throwable) {
        this(null, throwable, null, false);
    }

    /**
     * Gets an optional recovery suggestion message.
     * @return customized recovery suggestion message
     */
    public final String getRecoverySuggestion() {
        return recoverySuggestion;
    }

    /**
     * Returns a hint as to whether it makes sense to retry upon this exception.
     * @return true if it is retryable.
     */
    public final boolean isRetryable() {
        return isRetryable;
    }
}
