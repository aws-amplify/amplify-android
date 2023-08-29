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

import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions

/**
 * Cognito extension of update verify totp setup options to add the platform specific fields.
 */
class AWSCognitoAuthVerifyTOTPSetupOptions private constructor(
    /**
     * Return the friendlyDeviceName to set during cognito TOTP setup.
     * @return friendlyDeviceName string
     */
    val friendlyDeviceName: String?
) : AuthVerifyTOTPSetupOptions() {

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }

        inline operator fun invoke(block: CognitoBuilder.() -> Unit) = CognitoBuilder()
            .apply(block).build()
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder>() {
        private var friendlyDeviceName: String? = null

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * Friendly device name to be set in Cognito.
         * @param friendlyDeviceName String input for friendlyDeviceName
         * @return current CognitoBuilder instance
         */
        fun friendlyDeviceName(friendlyDeviceName: String): CognitoBuilder {
            this.friendlyDeviceName = friendlyDeviceName
            return this
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthVerifyTOTPSetupOptions with the values specified in the builder.
         */
        override fun build(): AWSCognitoAuthVerifyTOTPSetupOptions {
            return AWSCognitoAuthVerifyTOTPSetupOptions(friendlyDeviceName)
        }
    }
}
