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

    /**
     * Exception thrown by AWS Cognito Auth.
     */
    public static class AWSCognitoAuthException extends AuthException {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a new AWS cognito auth related exception with a message, root cause, and recovery suggestion.
         *
         * @param message            An error message describing why this exception was thrown
         * @param cause              The underlying cause of this exception
         * @param recoverySuggestion Text suggesting a way to recover from the error being described
         */
        public AWSCognitoAuthException(
                @NonNull final String message,
                @NonNull final Throwable cause,
                @NonNull final String recoverySuggestion
        ) {
            super(message, cause, recoverySuggestion);
        }

        /**
         * Constructs a new AWS cognito auth related exception using a provided message and an associated error.
         *
         * @param message            Explains the reason for the exception
         * @param recoverySuggestion Text suggesting a way to recover from the error being described
         */
        public AWSCognitoAuthException(
                @NonNull final String message,
                @NonNull final String recoverySuggestion
        ) {
            super(message, recoverySuggestion);
        }

        /**
         * Could not perform the action because user not found in the system.
         */
        public static class UserNotFoundException extends AWSCognitoAuthException {
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
         * Could not perform the action because user not confirmed in the system.
         */
        public static class UserNotConfirmedException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "User not confirmed in the system.";
            private static final String RECOVERY_SUGGESTION = "Retry operation and make a confirmation.";

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
        public static class UsernameExistsException extends AWSCognitoAuthException {
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
         * Could not perform the action because alias already exists in the system.
         */
        public static class AliasExistsException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Alias already exists in the system.";
            private static final String RECOVERY_SUGGESTION = "Retry operation and enter another alias.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public AliasExistsException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because error occurs when delivering the confirmation code.
         */
        public static class CodeDeliveryFailureException extends AWSCognitoAuthException {
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
         * Could not perform the action because user enters incorrect confirmation code.
         */
        public static class CodeMismatchException extends AWSCognitoAuthException {
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
        public static class CodeExpiredException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Confirmation code has expired.";
            private static final String RECOVERY_SUGGESTION = "Retry operation and send another confirmation code.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public CodeExpiredException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because there exists incorrect parameters.
         */
        public class InvalidParameterException extends AWSCognitoAuthException {
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
        public static class InvalidPasswordException extends AWSCognitoAuthException {
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
        public class LimitExceededException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Number of allowed operation has exceeded.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

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
        public static class PasswordResetRequiredException extends AWSCognitoAuthException {
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
         * Could not perform the action because Amazon Cognito service cannot find the requested resource.
         */
        public static class ResourceNotFoundException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Amazon Cognito service cannot find the requested resource.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

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
        public static class FailedAttemptsLimitExceededException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "User has made too many failed attempts for a given action.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public FailedAttemptsLimitExceededException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because user made too many requests for a given operation.
         */
        public static class RequestLimitExceededException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "The user has made too many requests for a given operation.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public RequestLimitExceededException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because Amazon Cognito service encounters an invalid AWS Lambda response
         * or encounters an unexpected exception with the AWS Lambda service.
         */
        public static class LambdaException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE =
                    "Amazon Cognito service encounters an invalid AWS Lambda response " +
                            "or encounters an unexpected exception with the AWS Lambda service.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public LambdaException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because device is not tracked.
         */
        public static class DeviceNotTrackedException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Device is not tracked.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public DeviceNotTrackedException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because there exists error in loading web UI.
         */
        public static class ErrorLoadingUIException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Error in loading the web UI.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public ErrorLoadingUIException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because user cancelled the step.
         */
        public static class UserCancelledException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "User cancelled the step.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public UserCancelledException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action because requested resource is not available with the current account setup.
         */
        public static class InvalidAccountTypeException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Requested resource is not available with the current account setup.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public InvalidAccountTypeException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }

        /**
         * Could not perform the action since request was not completed because of any network related issue.
         */
        public static class NetworkException extends AWSCognitoAuthException {
            private static final long serialVersionUID = 1L;
            private static final String MESSAGE = "Request was not completed because of any network related issue.";
            private static final String RECOVERY_SUGGESTION = "Turn off and re-attempt this operation.";

            /**
             * Default message/recovery suggestion with a cause.
             * @param cause The original error.
             */
            public NetworkException(Throwable cause) {
                super(MESSAGE, cause, RECOVERY_SUGGESTION);
            }
        }
    }
}


