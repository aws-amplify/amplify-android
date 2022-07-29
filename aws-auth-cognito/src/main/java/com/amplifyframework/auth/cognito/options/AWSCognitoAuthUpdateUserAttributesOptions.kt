package com.amplifyframework.auth.cognito.options

import androidx.core.util.ObjectsCompat
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.util.Immutable
import java.util.Objects.requireNonNull

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
/**
 * Cognito extension of update user attributes options to add the platform specific fields.
 */
class AWSCognitoAuthUpdateUserAttributesOptions
/**
 * Advanced options for update user attributes.
 * @param metadata Additional custom attributes to be sent to the service such as information about the client
 */
protected constructor(
    val metadata: Map<String, String>
) : AuthUpdateUserAttributesOptions() {

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
            val authUpdateUserAttributesOptions = obj as AWSCognitoAuthUpdateUserAttributesOptions
            ObjectsCompat.equals(metadata, authUpdateUserAttributesOptions.metadata)
        }
    }

    override fun toString(): String {
        return "AWSCognitoAuthUpdateUserAttributesOptions{" +
            "metadata=" + metadata +
            '}'
    }

    /**
     * The builder for this class.
     */
    class CognitoBuilder : Builder<CognitoBuilder>() {
        private val metadata: MutableMap<String, String>

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        override fun getThis(): CognitoBuilder {
            return this
        }

        /**
         * Set the metadata field for the object being built.
         * @param metadata Custom user metadata to be sent with the update user attributes request.
         * @return The builder object to continue building.
         */
        fun metadata(metadata: Map<String, String>): CognitoBuilder {
            requireNonNull(metadata)
            this.metadata.clear()
            this.metadata.putAll(metadata)
            return getThis()
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthUpdateUserAttributesOptions with the values specified in the builder.
         */
        override fun build(): AuthUpdateUserAttributesOptions {
            return AWSCognitoAuthUpdateUserAttributesOptions(
                Immutable.of(metadata)
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
         * @return a builder object.
         */
        fun builder(): CognitoBuilder {
            return CognitoBuilder()
        }
    }
}
