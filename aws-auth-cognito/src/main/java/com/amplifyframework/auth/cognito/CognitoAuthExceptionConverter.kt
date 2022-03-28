/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AliasExistsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryFailureException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ExpiredCodeException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidParameterException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidPasswordException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.LimitExceededException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.MfaMethodNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.PasswordResetRequiredException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.TooManyFailedAttemptsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.TooManyRequestsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotConfirmedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthException.FailedAttemptsLimitExceededException
import com.amplifyframework.auth.AuthException.SoftwareTokenMFANotFoundException

/**
 * Convert AWS Cognito Exceptions to AuthExceptions.
 */
class CognitoAuthExceptionConverter {
    companion object {
        private const val defaultRecoveryMessage = "See attached exception for more details."

        /**
         * Lookup method to convert AWS Cognito Exception to AuthException.
         * @param error Exception thrown by AWSCognitoAuthService
         * @param fallbackMessage Fallback message to inform failure
         * @return AuthException Specific exception for Amplify Auth
         */
        fun lookup(error: Exception, fallbackMessage: String): AuthException {
            return when (error) {
                is UserNotFoundException -> AuthException.UserNotFoundException(error)
                is UserNotConfirmedException -> AuthException.UserNotConfirmedException(error)
                is UsernameExistsException -> AuthException.UsernameExistsException(error)
                is AliasExistsException -> AuthException.AliasExistsException(error)
                is InvalidPasswordException -> AuthException.InvalidPasswordException(error)
                is InvalidParameterException -> AuthException.InvalidParameterException(error)
                is ExpiredCodeException -> AuthException.CodeExpiredException(error)
                is CodeMismatchException -> AuthException.CodeMismatchException(error)
                is CodeDeliveryFailureException -> AuthException.CodeDeliveryFailureException(error)
                is LimitExceededException -> AuthException.LimitExceededException(error)
                is MfaMethodNotFoundException -> AuthException.MFAMethodNotFoundException(error)
                is NotAuthorizedException -> AuthException.NotAuthorizedException(error)
                is ResourceNotFoundException -> AuthException.ResourceNotFoundException(error)
                is SoftwareTokenMfaNotFoundException -> SoftwareTokenMFANotFoundException(error)
                is TooManyFailedAttemptsException -> FailedAttemptsLimitExceededException(error)
                is TooManyRequestsException -> AuthException.TooManyRequestsException(error)
                is PasswordResetRequiredException -> AuthException.PasswordResetRequiredException(
                    error
                )
                else -> AuthException(fallbackMessage, error, defaultRecoveryMessage)
            }
        }
    }
}
