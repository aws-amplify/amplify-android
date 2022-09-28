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

/**
 * Options to provide additional fields to federate to Cognito Identity Provider.
 */
data class FederateToIdentityPoolOptions internal constructor(val developerProvidedIdentityId: String?) {

    /**
     * The builder for this class.
     */
    class Builder {
        private var developerProvidedIdentityId: String? = null

        /**
         * Set the developerProvidedIdentityId field for the object being built.
         * @param _developerProvidedIdentityId Provide identity id for federation to Cognito Identity Provider
         * @return The builder object to continue building.
         */
        fun developerProvidedIdentityId(_developerProvidedIdentityId: String): Builder {
            this.developerProvidedIdentityId = _developerProvidedIdentityId
            return this
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of FederateToIdentityPoolOptions with the values specified in the builder.
         */
        fun build(): FederateToIdentityPoolOptions {
            return FederateToIdentityPoolOptions(developerProvidedIdentityId)
        }
    }

    companion object {
        /**
         * Get a builder object.
         * @return a builder object.
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
