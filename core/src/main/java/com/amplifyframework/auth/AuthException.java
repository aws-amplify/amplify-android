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
        private static final String DEFAULT_RECOVERY_SUGGESTION = "Please sign in and reattempt the operation.";
        private static final String IDENTITY_POOL_RECOVERY_SUGGESTION = "If you have guest access enabled, please " +
                "check that your device is online and try again. Otherwise if guest access is not enabled, you'll " +
                "need to sign in and try again.";

        /**
         * Default message/recovery suggestion without a cause.
         */
        public SignedOutException() {
            super(MESSAGE, DEFAULT_RECOVERY_SUGGESTION);
        }

        /**
         * Returns the default error message with a recovery message based on whether guest access is enabled or not
         * since the user would not necessarily have to sign in to recover from the error if guest access is enabled.
         * @param guestAccess specifies whether guest access is enabled or not so that the proper recovery message can
         *                    be returned.
         */
        public SignedOutException(GuestAccess guestAccess) {
            super(MESSAGE, guestAccess == GuestAccess.GUEST_ACCESS_ENABLED ?
                            IDENTITY_POOL_RECOVERY_SUGGESTION :
                            DEFAULT_RECOVERY_SUGGESTION);
        }

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public SignedOutException(Throwable cause) {
            super(MESSAGE, cause, DEFAULT_RECOVERY_SUGGESTION);
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

    /**
     * Could not perform the action because user was not found in the system.
     */
    public static class UserNotFoundException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "User not found in the system.";
        private static final String RECOVERY_SUGGESTION = "Please enter correct username.";

        /**
         * Default message/recovery suggestion with a cause.
         *
         * @param cause The original error.
         */
        public UserNotFoundException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because user is not confirmed in the system.
     */
    public static class UserNotConfirmedException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "User not confirmed in the system.";
        private static final String RECOVERY_SUGGESTION = "Please confirm user first and then retry operation";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public UserNotConfirmedException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because username already exists in the system.
     */
    public static class UsernameExistsException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Username already exists in the system.";
        private static final String RECOVERY_SUGGESTION = "Retry operation and enter another username.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public UsernameExistsException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because alias (an account with certain email or phone) already exists in the system.
     */
    public static class AliasExistsException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE =
                "Alias (an account with this email or phone) already exists in the system.";
        private static final String RECOVERY_SUGGESTION = "Retry operation and use another alias.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public AliasExistsException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because error occured when delivering the confirmation code.
     */
    public static class CodeDeliveryFailureException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Error in delivering the confirmation code.";
        private static final String RECOVERY_SUGGESTION = "Retry operation and send another confirmation code.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public CodeDeliveryFailureException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because user entered incorrect confirmation code.
     */
    public static class CodeMismatchException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Confirmation code entered is not correct.";
        private static final String RECOVERY_SUGGESTION = "Enter correct confirmation code.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public CodeMismatchException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because confirmation code has expired.
     */
    public static class CodeExpiredException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Confirmation code has expired.";
        private static final String RECOVERY_SUGGESTION =
                "Resend a new confirmation code and then retry operation with it.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public CodeExpiredException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because there are incorrect parameters.
     */
    public static class InvalidParameterException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "One or more parameters are incorrect.";
        private static final String RECOVERY_SUGGESTION = "Enter correct parameters.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public InvalidParameterException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because the password given is invalid.
     */
    public static class InvalidPasswordException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "The password given is invalid.";
        private static final String RECOVERY_SUGGESTION = "Enter correct password.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public InvalidPasswordException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because number of allowed operation has exceeded.
     */
    public static class LimitExceededException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Number of allowed operation has exceeded.";
        private static final String RECOVERY_SUGGESTION =
                "Please wait a while before re-attempting or increase the service limit.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public LimitExceededException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because password needs to be reset.
     */
    public static class PasswordResetRequiredException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Required to reset the password of the user.";
        private static final String RECOVERY_SUGGESTION = "Reset the password of the user.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public PasswordResetRequiredException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because Amplify cannot find the requested resource.
     */
    public static class ResourceNotFoundException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Could not find the requested online resource.";
        private static final String RECOVERY_SUGGESTION =
                "Retry with exponential back-off or check your config file to be sure the endpoint is valid.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public ResourceNotFoundException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Could not perform the action because user made too many failed attempts for a given action.
     */
    public static class FailedAttemptsLimitExceededException extends AuthException {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "User has made too many failed attempts for a given action.";
        private static final String RECOVERY_SUGGESTION =
                "Please check out the service configuration to see the condition of locking.";

        /**
         * Default message/recovery suggestion with a cause.
         * @param cause The original error.
         */
        public FailedAttemptsLimitExceededException(Throwable cause) {
            super(MESSAGE, cause, RECOVERY_SUGGESTION);
        }
    }

    /**
     * Allows the user to specify whether guest access is enabled or not since this can affect which
     * recovery message should be included.
     */
    public enum GuestAccess {
        /**
         * Auth has been configured to support guest access (where a user can get credentials once online without being
         * signed in).
         */
        GUEST_ACCESS_ENABLED,
        /**
         * AAuth has not been configured to support guest access.
         */
        GUEST_ACCESS_DISABLED
    }
}
