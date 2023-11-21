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

package com.amplifyframework.auth.cognito.options

import com.amplifyframework.auth.options.AuthMagicLinkOptions

/**
 * Cognito extension of passwordless Magic Link options to add the platform specific fields.
 */
data class AWSCognitoAuthMagicLinkOptions
/**
 * Advanced options for signing up using Passwordless.
 * @param userMetadata user attributes to be sent to the service such as information about the user
 * @param clientMetadata Additional custom attributes to be sent to the service such as information about the client
 */
internal constructor(
    /**
     * Get additional custom attributes to be sent to the service such as information about the client.
     * @return a map of additional custom attributes to be sent to the service such as information about the client
     */
    val clientMetadata: Map<String, String>,
    /**
     * Get user attributes to be sent to the service such as information about the user
     * @return user attributes to be sent to the service such as information about the user
     */
    val userMetadata: Map<String, String>
) : AuthMagicLinkOptions() {

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
        private var clientMetadata: Map<String, String> = mapOf()
        private var userMetadata: Map<String, String> = mapOf()
        /**
         * Gets the type of builder to support proper flow with this being an extended class.
         * @return the type of builder to support proper flow with this being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }
        /**
         * A map of additional custom attributes to be sent to the service such as information about the client.
         * @param clientMetadata A map of additional custom attributes to be sent to the service such as information
         * about the client.
         * @return the instance of the builder.
         */
        fun clientMetadata(clientMetadata: Map<String, String>) = apply { this.clientMetadata = clientMetadata }

        /**
         * A map of user attributes to be sent to the service such as information about the user.
         * @param userMetadata A map of user attributes to be sent to the service such as information
         * about the user
         * @return the instance of the builder.
         */
        fun userMetadata(userMetadata: Map<String, String>) = apply { this.userMetadata = userMetadata }

        /**
         * Build the object.
         * @return a new instance of AWSCognitoAuthMagicLinkOptions.
         */
        override fun build() = AWSCognitoAuthMagicLinkOptions(clientMetadata, userMetadata)
    }
}
