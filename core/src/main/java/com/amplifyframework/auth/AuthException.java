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

package com.amplifyframework.auth;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;

/**
 * Exception thrown by Storage category plugins.
 */
public class AuthException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param cause The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AuthException(
            @NonNull final String message,
            @NonNull final Throwable cause,
            @NonNull final String recoverySuggestion
    ) {
        super(message, cause, recoverySuggestion);
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AuthException(
            @NonNull final String message,
            @NonNull final String recoverySuggestion
    ) {
        super(message, recoverySuggestion);
    }

    /**
     * Auth exception caused by the user being signed out.
     */
    public static class SignedOutException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "You are currently signed out.";
        private static final String RECOVERY_SUGGESTION = "Please sign in and reattempt the operation.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public SignedOutException() {
            super(MESSAGE, RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public SignedOutException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not get valid credentials due to the device being offline.
     */
    public static class SessionUnavailableOfflineException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Unable to fetch/refresh credentials because the server is unreachable.";
        private static final String RECOVERY_SUGGESTION = "Check online connectivity and retry operation.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public SessionUnavailableOfflineException() {
            super(MESSAGE, RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public SessionUnavailableOfflineException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not get valid credentials due to an error from the underlying service.
     */
    public static class SessionUnavailableServiceException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Unable to fetch/refresh credentials because of a service error.";
        private static final String RECOVERY_SUGGESTION = "Retry with exponential backoff.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public SessionUnavailableServiceException() {
            super(MESSAGE, RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public SessionUnavailableServiceException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because the configuration of the signed in account does not support it.
     */
    public static class InvalidAccountTypeException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "The account type you have configured doesn't support this operation.";
        private static final String RECOVERY_SUGGESTION =
                "Update your Auth configuration to an account type which supports this operation.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public InvalidAccountTypeException() {
            super(MESSAGE, RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public InvalidAccountTypeException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Unable to get valid credentials until the user signs in again.
     */
    public static class SessionExpiredException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Your session has expired.";
        private static final String RECOVERY_SUGGESTION = "Please sign in and reattempt the operation.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public SessionExpiredException() {
            super(MESSAGE, RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public SessionExpiredException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the Auth operation for an unknown reason.
     */
    public static class UnknownException extends AuthException {
        private static final long serialVersionUID = 1L;

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public UnknownException(Throwable cause) {
            super(
                    "An unclassified error prevented this operation.",
                    cause,
                    "See the attached exception for more details"
            );
        }
    }
}
