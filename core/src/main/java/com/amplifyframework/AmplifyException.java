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
import androidx.core.util.ObjectsCompat;

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

    /**
     * A common recovery suggestion for errors that are unexpected and could be originated from a bug.
     * This can be used when the framework makes assumptions that are not met due to programming or
     * setup mistakes that we could not protected against or foresee.
     */
    public static final String REPORT_BUG_TO_AWS_SUGGESTION =
            "There is a possibility that there is a bug if this error persists. Please take a look at \n" +
            "https://github.com/aws-amplify/amplify-android/issues to see if there are any existing issues that \n" +
            "match your scenario, and file an issue with the details of the bug if there isn't.";

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

    /**
     * If any child classes override this method, they should be sure to include the recoverySuggestion, message, and
     * cause in their hash.
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getRecoverySuggestion(),
                getMessage(),
                getCause()
        );
    }

    /**
     * If any child classes override this method, they should be sure to include the recoverySuggestion, message, and
     * cause in their equality check.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || !(obj instanceof AmplifyException)) {
            return false;
        } else {
            AmplifyException amplifyException = (AmplifyException) obj;
            return ObjectsCompat.equals(getRecoverySuggestion(), amplifyException.getRecoverySuggestion()) &&
                    ObjectsCompat.equals(getMessage(), amplifyException.getMessage()) &&
                    ObjectsCompat.equals(getCause(), amplifyException.getCause());
        }
    }

    /**
     * If any child classes override this method, they should be sure to include the recoverySuggestion, message, and
     * cause in their returned string.
     */
    @Override
    public String toString() {
        return "AmplifyException {" +
                "message=" + getMessage() +
                ", cause=" + getCause() +
                ", recoverySuggestion=" + getRecoverySuggestion() +
                '}';
    }
}
