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
package com.amplifyframework.auth.cognito.options

import androidx.core.util.ObjectsCompat
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.util.Immutable

/**
 * Cognito extension of sign up options to add the platform specific fields.
 */
class AWSCognitoAuthSignUpOptions
/**
 * Advanced options for signing in.
 * @param userAttributes Additional user attributes which should be associated with this user on registration
 * @param validationData A map of custom key/values to be sent as part of the sign up process
 * @param clientMetadata Additional custom attributes to be sent to the service such as information about the client
 */
internal constructor(
    userAttributes: List<AuthUserAttribute>,
    /**
     * Get a map of custom key/values to be sent as part of the sign up process.
     * @return a map of custom key/values to be sent as part of the sign up process
     */
    val validationData: Map<String, String>,
    /**
     * Get additional custom attributes to be sent to the service such as information about the client.
     * @return a map of additional custom attributes to be sent to the service such as information about the client
     */
    val clientMetadata: Map<String, String>
) : AuthSignUpOptions(userAttributes) {

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder?>() {
        private val validationData: MutableMap<String, String>
        private val clientMetadata: MutableMap<String, String>

        /**
         * Gets the type of builder to support proper flow with this being an extended class.
         * @return the type of builder to support proper flow with this being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * A map of custom data the user can send as part of the sign up process for validation.
         * @param validationData A map of custom data the user can send as part of the sign up process for validation.
         * @return the instance of the builder.
         */
        fun validationData(validationData: Map<String, String>): CognitoBuilder {
            this.validationData.clear()
            this.validationData.putAll(validationData)
            return getThis()
        }

        /**
         * A map of additional custom attributes to be sent to the service such as information about the client.
         * @param clientMetadata A map of additional custom attributes to be sent to the service such as information
         * about the client.
         * @return the instance of the builder.
         */
        fun clientMetadata(clientMetadata: Map<String, String>): CognitoBuilder {
            this.clientMetadata.clear()
            this.clientMetadata.putAll(clientMetadata)
            return getThis()
        }

        /**
         * Build the object.
         * @return a new instance of AWSCognitoAuthSignUpOptions.
         */
        override fun build(): AWSCognitoAuthSignUpOptions {
            return AWSCognitoAuthSignUpOptions(
                Immutable.of(super.getUserAttributes()),
                Immutable.of(validationData),
                Immutable.of(clientMetadata)
            )
        }

        /**
         * Constructs the builder.
         */
        init {
            validationData = HashMap()
            clientMetadata = HashMap()
        }
    }

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }

        inline operator fun invoke(block: CognitoBuilder.() -> Unit) =
            CognitoBuilder()
                .apply(block).build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass || other !is AWSCognitoAuthSignUpOptions) return false
        if (!super.equals(other)) return false

        if (validationData != other.validationData) return false
        if (clientMetadata != other.clientMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(
            userAttributes,
            validationData,
            clientMetadata
        )
    }

    override fun toString(): String {
        return "AuthSignUpOptions{" +
            "userAttributes=" + userAttributes +
            "validationData=" + validationData +
            "clientMetaData=" + clientMetadata +
            '}'
    }
}
