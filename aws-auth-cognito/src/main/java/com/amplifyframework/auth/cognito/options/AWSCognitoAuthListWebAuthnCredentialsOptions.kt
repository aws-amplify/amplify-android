/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions
import com.amplifyframework.auth.result.AuthListWebAuthnCredentialsResult

/**
 * Options for the listWebAuthnCredentials API that are specific to Cognito.
 * @param nextToken The token returned the [AuthListWebAuthnCredentialsResult] that will load the next page of results.
 *      Should be null to load the first page.
 * @param maxResults The maximum number of results to return per page. Set to null to use the default max.
 */
data class AWSCognitoAuthListWebAuthnCredentialsOptions internal constructor(
    val nextToken: String?,
    val maxResults: Int?
) : AuthListWebAuthnCredentialsOptions() {
    companion object {
        /**
         * Create a [Builder] for this class
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Construct using a DSL syntax
         */
        @JvmSynthetic
        inline operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        /**
         * Return the default options
         */
        @JvmStatic
        fun defaults() = builder().build()

        private fun AuthListWebAuthnCredentialsOptions.asCognitoOptions() =
            this as? AWSCognitoAuthListWebAuthnCredentialsOptions
        internal val AuthListWebAuthnCredentialsOptions.nextToken: String?
            get() = this.asCognitoOptions()?.nextToken
        internal val AuthListWebAuthnCredentialsOptions.maxResults: Int?
            get() = this.asCognitoOptions()?.maxResults
    }

    /**
     * Builder for cognito-specific [AuthListWebAuthnCredentialsOptions].
     */
    class Builder : AuthListWebAuthnCredentialsOptions.Builder<Builder>() {
        /**
         *The next token that was returned in the prior page of results. Set to null to load the first page.
         */
        var nextToken: String? = null
            @JvmSynthetic set

        /**
         * The maximum number of results to return. Set to null to use the service-default max value.
         */
        var maxResults: Int? = null
            @JvmSynthetic set

        /**
         * Returns this instance for typesafe chaining from the parent class
         */
        override fun getThis() = this

        /**
         * Set the next token to load a further page of results
         * @param nextToken The next token that was returned in the prior page of results. Set to null to load the
         * first page.
         * @return This instance for chaining calls
         */
        fun nextToken(nextToken: String?) = apply { this.nextToken = nextToken }

        /**
         * Set the maximum number of results to return per page
         * @param maxResults The maximum number of results to return. Set to null to use the service-default max
         * value.
         * @return This instance for chaining calls
         */
        fun maxResults(maxResults: Int?) = apply { this.maxResults = maxResults }

        /**
         * Builds the options object
         * @return The constructed [AWSCognitoAuthListWebAuthnCredentialsOptions] objects
         */
        override fun build() = AWSCognitoAuthListWebAuthnCredentialsOptions(
            nextToken = nextToken,
            maxResults = maxResults
        )
    }
}
