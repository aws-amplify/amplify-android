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
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.util.Immutable
import java.util.Objects

/**
 * Cognito extension of sign in options to add the platform specific fields.
 */
open class AWSCognitoAuthSignInOptions protected constructor(
    /**
     * Get custom attributes to be sent to the service such as information about the client.
     *
     * @return custom attributes to be sent to the service such as information about the client
     */
    val metadata: Map<String, String>,
    /**
     * Get authFlowType to be sent to the service.
     *
     * @return authFlowType to be sent to the signIn api
     */
    var authFlowType: AuthFlowType?
) : AuthSignInOptions() {

    override fun hashCode(): Int {
        return ObjectsCompat.hash(
            metadata
        )
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null || javaClass != obj.javaClass) {
            false
        } else {
            val authSignInOptions = obj as AWSCognitoAuthSignInOptions
            ObjectsCompat.equals(metadata, authSignInOptions.metadata)
        }
    }

    override fun toString(): String {
        return "AWSCognitoAuthSignInOptions{" +
                "metadata=" + metadata +
                '}'
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder?>() {
        private val metadata: MutableMap<String, String>
        private var authFlowType: AuthFlowType? = null

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         *
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * Set the metadata field for the object being built.
         *
         * @param metadata Custom user metadata to be sent with the sign in request.
         * @return The builder object to continue building.
         */
        fun metadata(metadata: Map<String, String>): CognitoBuilder {
            Objects.requireNonNull(metadata)
            this.metadata.clear()
            this.metadata.putAll(metadata)
            return getThis()
        }

        /**
         * Set the authFlowType for the object being built.
         *
         * @param authFlowType authFlowType to be sent to sign in request.
         * @return The builder object to continue building.
         */
        fun authFlowType(authFlowType: AuthFlowType): CognitoBuilder {
            this.authFlowType = authFlowType
            return getThis()
        }

        /**
         * Construct and return the object with the values set in the builder.
         *
         * @return a new instance of AWSCognitoAuthSignInOptions with the values specified in the builder.
         */
        override fun build(): AWSCognitoAuthSignInOptions {
            return AWSCognitoAuthSignInOptions(
                Immutable.of(metadata),
                authFlowType!!
            )
        }

        /**
         * Constructor for the builder.
         */
        init {
            metadata = HashMap()
        }
    }

    companion object {
        /**
         * Get a builder object.
         *
         * @return a builder object.
         */
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }
    }

    /**
     * Advanced options for signing in.
     *
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param authFlowType AuthFlowType to be used by signIn API
     */
    init {
        authFlowType = authFlowType
    }
}