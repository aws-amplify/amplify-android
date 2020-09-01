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

package com.amplifyframework.auth.cognito.util;

import com.amplifyframework.auth.AuthException;

import com.amazonaws.services.cognitoidentityprovider.model.AliasExistsException;
import com.amazonaws.services.cognitoidentityprovider.model.CodeDeliveryFailureException;
import com.amazonaws.services.cognitoidentityprovider.model.CodeMismatchException;
import com.amazonaws.services.cognitoidentityprovider.model.ExpiredCodeException;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.LimitExceededException;
import com.amazonaws.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import com.amazonaws.services.cognitoidentityprovider.model.ResourceNotFoundException;
import com.amazonaws.services.cognitoidentityprovider.model.TooManyFailedAttemptsException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException;

/**
 * Convert AWS Cognito Exceptions to AuthExceptions.
 */
public final class CognitoAuthExceptionConverter {

    /**
     * Dis-allows instantiation of this class.
     */
    private CognitoAuthExceptionConverter() {}

    /**
     * Lookup method to convert AWS Cognito Exception to AuthException.
     * @param error Exception thrown by AWSMobileClient
     * @param fallbackMessage Fallback message to inform failure
     * @return AuthException
     */
    public static AuthException lookup(Exception error, String fallbackMessage) {
        if (error instanceof UserNotFoundException) {
            return new AuthException.UserNotFoundException(error);
        }

        if (error instanceof UserNotConfirmedException) {
            return new AuthException.UserNotConfirmedException(error);
        }

        if (error instanceof UsernameExistsException) {
            return new AuthException.UsernameExistsException(error);
        }

        if (error instanceof AliasExistsException) {
            return new AuthException.AliasExistsException(error);
        }

        if (error instanceof InvalidPasswordException) {
            return new AuthException.InvalidPasswordException(error);
        }

        if (error instanceof InvalidParameterException) {
            return new AuthException.InvalidParameterException(error);
        }

        if (error instanceof ExpiredCodeException) {
            return new AuthException.CodeExpiredException(error);
        }

        if (error instanceof CodeMismatchException) {
            return new AuthException.CodeMismatchException(error);
        }

        if (error instanceof CodeDeliveryFailureException) {
            return new AuthException.CodeDeliveryFailureException(error);
        }

        if (error instanceof LimitExceededException) {
            return new AuthException.LimitExceededException(error);
        }

        if (error instanceof ResourceNotFoundException) {
            return new AuthException.ResourceNotFoundException(error);
        }

        if (error instanceof TooManyFailedAttemptsException) {
            return new AuthException.FailedAttemptsLimitExceededException(error);
        }

        if (error instanceof PasswordResetRequiredException) {
            return new AuthException.PasswordResetRequiredException(error);
        }

        return new AuthException(fallbackMessage, error, "See attached exception for more details.");

    }
}
