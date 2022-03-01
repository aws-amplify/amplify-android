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
import com.amplifyframework.auth.options.AuthSignOutOptions

/**
 * Cognito extension of sign out options to add the platform specific fields.
 */
class AWSCognitoAuthSignOutOptions
/**
 * Advanced options for signing out.
 * @param globalSignOut Sign out of all devices (do not use when signing out of a WebUI experience)
 * @param invalidateTokens Specify whether to invalidate tokens on the server by making a call.
 * @param browserPackage Specify which browser package should be used for signing out of an account which was signed
 * into with a web UI experience (example value: "org.mozilla.firefox").
 * Defaults to the Chrome package if not specified.
 */ protected constructor(
    globalSignOut: Boolean,
    val invalidateTokens: Boolean,
    val browserPackage: String?
) : AuthSignOutOptions(globalSignOut) {

    override fun hashCode(): Int {
        return ObjectsCompat.hash(
            isGlobalSignOut,
            invalidateTokens,
            browserPackage
        )
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null || javaClass != obj.javaClass) {
            false
        } else {
            val authSignOutOptions = obj as AWSCognitoAuthSignOutOptions
            ObjectsCompat.equals(isGlobalSignOut, authSignOutOptions.isGlobalSignOut) &&
                    ObjectsCompat.equals(browserPackage, authSignOutOptions.browserPackage) &&
                    ObjectsCompat.equals(invalidateTokens, authSignOutOptions.invalidateTokens)
        }
    }

    override fun toString(): String {
        return "AWSCognitoAuthSignOutOptions{" +
                "isGlobalSignOut=" + isGlobalSignOut +
                "invalidateTokens=" + invalidateTokens +
                ", browserPackage=" + browserPackage +
                '}'
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder>() {
        private var browserPackage: String? = null
        private var invalidateTokens: Boolean = true

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * When this is set to true - service call is made to invalidate the tokens (that used for making
         * calls to other AWS service) on the server.
         * @param invalidateTokens true if the user's token should be invalidated on the server.
         * @return the instance of the builder.
         */
        fun invalidateTokens(invalidateTokens: Boolean): CognitoBuilder {
            this.invalidateTokens = invalidateTokens
            return this
        }

        /**
         * This can optionally be set to specify which browser package should perform the sign out action
         * (e.g. "org.mozilla.firefox") in the case of an account which was signed in to from a web UI experience.
         * Defaults to the Chrome package if not set.
         *
         * @param browserPackage String specifying the browser package to perform the web sign out action.
         * @return the instance of the builder.
         */
        fun browserPackage(browserPackage: String): CognitoBuilder {
            this.browserPackage = browserPackage
            return this
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthSignOutOptions with the values specified in the builder.
         */
        override fun build(): AWSCognitoAuthSignOutOptions {
            return AWSCognitoAuthSignOutOptions(
                super.isGlobalSignOut(),
                invalidateTokens,
                browserPackage
            )
        }
    }

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }
    }
}