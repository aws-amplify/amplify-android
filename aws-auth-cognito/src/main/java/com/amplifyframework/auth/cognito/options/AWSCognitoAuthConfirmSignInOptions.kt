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
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.util.Immutable

/**
 * Cognito extension of confirm sign in options to add the platform specific fields.
 */
open class AWSCognitoAuthConfirmSignInOptions protected constructor(
    /**
     * Get custom attributes to be sent to the service such as information about the client.
     * @return custom attributes to be sent to the service such as information about the client
     */
    val metadata: Map<String, String>,
    /**
     * Get additional user attributes which should be associated with this user on confirmSignIn.
     * @return additional user attributes which should be associated with this user on confirmSignIn
     */
    var userAttributes: List<AuthUserAttribute>
) : AuthConfirmSignInOptions() {

    override fun hashCode(): Int {
        return ObjectsCompat.hash(
            metadata,
            userAttributes
        )
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null || javaClass != obj.javaClass) {
            false
        } else {
            val authConfirmSignInOptions = obj as AWSCognitoAuthConfirmSignInOptions
            ObjectsCompat.equals(metadata, authConfirmSignInOptions.metadata) &&
                ObjectsCompat.equals(userAttributes, authConfirmSignInOptions.userAttributes)
        }
    }

    override fun toString(): String {
        return "AWSCognitoAuthConfirmSignInOptions{" +
            "userAttributes=" + userAttributes +
            ", metadata=" + metadata +
            '}'
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder?>() {
        private val metadata: MutableMap<String, String>
        private val userAttributes: MutableList<AuthUserAttribute>

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * Set the metadata field for the object being built.
         * @param _metadata Custom user metadata to be sent with the sign in request.
         * @return The builder object to continue building.
         */
        fun metadata(_metadata: Map<String, String>): CognitoBuilder {
            this.metadata.clear()
            this.metadata.putAll(_metadata)
            return getThis()
        }

        /**
         * Set the userAttributes field for the object being built.
         * @param _userAttributes A list of additional user attributes which should be
         * * associated with this user on confirmSignIn.
         * @return the instance of the builder.
         */
        fun userAttributes(_userAttributes: List<AuthUserAttribute>): CognitoBuilder {
            this.userAttributes.clear()
            this.userAttributes.addAll(_userAttributes)
            return getThis()
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthConfirmSignInOptions with the values specified in the builder.
         */
        override fun build(): AuthConfirmSignInOptions {
            return AWSCognitoAuthConfirmSignInOptions(
                Immutable.of(metadata),
                Immutable.of(userAttributes)
            )
        }

        /**
         * Constructor for the builder.
         */
        init {
            metadata = HashMap()
            userAttributes = ArrayList()
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

    /**
     * Advanced options for confirming sign in.
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param userAttributes A list of additional user attributes which should be
     * associated with this user on confirmSignIn.
     */
    init {
        userAttributes = userAttributes
    }
}
