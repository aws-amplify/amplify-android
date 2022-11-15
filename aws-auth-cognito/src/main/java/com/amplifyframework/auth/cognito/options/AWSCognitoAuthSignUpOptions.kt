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

/**
 * Cognito extension of sign up options to add the platform specific fields.
 */
data class AWSCognitoAuthSignUpOptions
/**
 * Advanced options for signing in.
 * @param attributes Additional user attributes which should be associated with this user on registration
 * @param validationData A map of custom key/values to be sent as part of the sign up process
 * @param clientMetadata Additional custom attributes to be sent to the service such as information about the client
 */
internal constructor(
    @get:JvmName("getAttributes")
    private val attributes: List<AuthUserAttribute>,
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

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }

        inline operator fun invoke(block: CognitoBuilder.() -> Unit) = CognitoBuilder().apply(block).build()
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder?>() {
        private var validationData: Map<String, String> = mapOf()
        private var clientMetadata: Map<String, String> = mapOf()

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
        fun validationData(validationData: Map<String, String>) = apply { this.validationData = validationData }

        /**
         * A map of additional custom attributes to be sent to the service such as information about the client.
         * @param clientMetadata A map of additional custom attributes to be sent to the service such as information
         * about the client.
         * @return the instance of the builder.
         */
        fun clientMetadata(clientMetadata: Map<String, String>) = apply { this.clientMetadata = clientMetadata }

        /**
         * Build the object.
         * @return a new instance of AWSCognitoAuthSignUpOptions.
         */
        override fun build() = AWSCognitoAuthSignUpOptions(super.getUserAttributes(), validationData, clientMetadata)
    }
}
