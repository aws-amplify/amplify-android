/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.EnableSoftwareTokenMfaException
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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserLambdaValidationException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotConfirmedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.WebAuthnNotEnabledException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.exceptions.service.CodeExpiredException
import com.amplifyframework.auth.cognito.exceptions.service.FailedAttemptsLimitExceededException
import com.amplifyframework.auth.cognito.exceptions.service.MFAMethodNotFoundException
import com.amplifyframework.auth.cognito.exceptions.service.SoftwareTokenMFANotFoundException
import com.amplifyframework.auth.exceptions.UnknownException

/**
 * Convert AWS Cognito Exceptions to AuthExceptions.
 */
internal fun Exception.toAuthException(fallbackMessage: String) = when (this) {
    is AuthException -> this
    is UserNotFoundException -> com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException(
        this
    )
    is UserNotConfirmedException ->
        com.amplifyframework.auth.cognito.exceptions.service.UserNotConfirmedException(this)
    is UsernameExistsException ->
        com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException(this)
    is AliasExistsException -> com.amplifyframework.auth.cognito.exceptions.service.AliasExistsException(
        this
    )
    is InvalidPasswordException ->
        com.amplifyframework.auth.cognito.exceptions.service.InvalidPasswordException(this)
    is InvalidParameterException ->
        com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException(cause = this)
    is ExpiredCodeException -> CodeExpiredException(this)
    is CodeMismatchException -> com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException(
        this
    )
    is CodeDeliveryFailureException ->
        com.amplifyframework.auth.cognito.exceptions.service.CodeDeliveryFailureException(this)
    is LimitExceededException ->
        com.amplifyframework.auth.cognito.exceptions.service.LimitExceededException(this)
    is MfaMethodNotFoundException -> MFAMethodNotFoundException(this)
    is NotAuthorizedException -> com.amplifyframework.auth.exceptions.NotAuthorizedException(cause = this)
    is ResourceNotFoundException ->
        com.amplifyframework.auth.cognito.exceptions.service.ResourceNotFoundException(this)
    is SoftwareTokenMfaNotFoundException ->
        SoftwareTokenMFANotFoundException(this)
    is TooManyFailedAttemptsException ->
        FailedAttemptsLimitExceededException(this)
    is TooManyRequestsException ->
        com.amplifyframework.auth.cognito.exceptions.service.TooManyRequestsException(this)
    is PasswordResetRequiredException ->
        com.amplifyframework.auth.cognito.exceptions.service.PasswordResetRequiredException(this)
    is EnableSoftwareTokenMfaException ->
        com.amplifyframework.auth.cognito.exceptions.service.EnableSoftwareTokenMFAException(this)
    is UserLambdaValidationException ->
        com.amplifyframework.auth.cognito.exceptions.service.UserLambdaValidationException(
            this.message,
            this
        )
    is WebAuthnNotEnabledException ->
        com.amplifyframework.auth.cognito.exceptions.service.WebAuthnNotEnabledException(
            cause = this
        )
    else -> UnknownException(fallbackMessage, this)
}
