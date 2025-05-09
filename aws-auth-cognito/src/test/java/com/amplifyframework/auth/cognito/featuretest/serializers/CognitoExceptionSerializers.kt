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

@file:Suppress("UNCHECKED_CAST")

package com.amplifyframework.auth.cognito.featuretest.serializers

import aws.sdk.kotlin.services.cognitoidentity.model.CognitoIdentityException
import aws.sdk.kotlin.services.cognitoidentity.model.TooManyRequestsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidParameterException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.amplifyframework.auth.exceptions.UnknownException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class CognitoExceptionSurrogate(
    val errorType: String,
    val errorMessage: String?
) {
    fun <T> toRealException(): T {
        val exception = when (errorType) {
            NotAuthorizedException::class.java.simpleName -> NotAuthorizedException.invoke {
                message = errorMessage
            } as T
            ResourceNotFoundException::class.java.simpleName -> ResourceNotFoundException.invoke {
                message = errorMessage
            } as T
            TooManyRequestsException::class.java.simpleName -> TooManyRequestsException.invoke {
                message = errorMessage
            } as T
            UsernameExistsException::class.java.simpleName -> UsernameExistsException.invoke {
                message = errorMessage
            } as T
            InvalidParameterException::class.java.simpleName -> InvalidParameterException.invoke {
                message = errorMessage
            } as T
            UserNotFoundException::class.java.simpleName -> UserNotFoundException.invoke {
                message = errorMessage
            } as T
            UnknownException::class.java.simpleName -> UnknownException(message = errorMessage ?: "") as T
            CodeMismatchException::class.java.simpleName -> CodeMismatchException.invoke {
                message = errorMessage
            } as T
            else -> {
                error("Exception for $errorType not defined")
            }
        }
        return exception
    }

    companion object {
        fun <T> toSurrogate(exception: T): CognitoExceptionSurrogate = when (exception) {
            is CognitoIdentityProviderException -> CognitoExceptionSurrogate(
                exception!!::class.java.simpleName,
                exception.message
            )
            is CognitoIdentityException -> CognitoExceptionSurrogate(
                exception!!::class.java.simpleName,
                exception.message
            )
            else -> {
                error("Exception for $exception not defined!")
            }
        }
    }
}

object CognitoIdentityProviderExceptionSerializer :
    KSerializer<CognitoIdentityProviderException> by CognitoExceptionSerializer()

object CognitoIdentityExceptionSerializer : KSerializer<CognitoIdentityException> by CognitoExceptionSerializer()

private class CognitoExceptionSerializer<T> : KSerializer<T> {
    private val strategy = CognitoExceptionSurrogate.serializer()

    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(strategy).toRealException() as T

    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(strategy, CognitoExceptionSurrogate.toSurrogate(value))
    }
}
