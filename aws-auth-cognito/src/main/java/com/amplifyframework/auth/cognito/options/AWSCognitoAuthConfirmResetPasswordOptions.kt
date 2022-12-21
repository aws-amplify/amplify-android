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

import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions

/**
 * Cognito extension of confirm reset password options to add the platform specific fields.
 */
data class AWSCognitoAuthConfirmResetPasswordOptions
internal constructor(val metadata: Map<String, String>) : AuthConfirmResetPasswordOptions() {

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
    class CognitoBuilder : Builder<CognitoBuilder>() {
        /**
         * Custom attributes to be sent to the service such as information about the client.
         */
        private var metadata: Map<String, String> = mapOf()

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * Set the metadata field for the object being built.
         * @param metadata Custom user metadata to be sent with the confirm reset password request.
         * @return The builder object to continue building.
         */
        fun metadata(metadata: Map<String, String>) = apply { this.metadata = metadata }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthConfirmResetPasswordOptions with the values specified in the builder.
         */
        override fun build() = AWSCognitoAuthConfirmResetPasswordOptions(metadata)
    }
}
