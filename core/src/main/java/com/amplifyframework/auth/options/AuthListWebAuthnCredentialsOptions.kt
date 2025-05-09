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

package com.amplifyframework.auth.options

/**
 * The shared options among all Auth plugins.
 */
abstract class AuthListWebAuthnCredentialsOptions protected constructor() {
    companion object {
        /**
         * Use the default listWebAuthnCredentials options.
         * @return Default listWebAuthnCredentials options.
         */
        @JvmStatic
        fun defaults(): AuthListWebAuthnCredentialsOptions = DefaultAuthListWebAuthnCredentialsOptions()
    }

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    abstract class Builder<T : Builder<T>> {
        /**
         * Return the type of builder this is so that chaining can work correctly without implicit casting.
         * @return the type of builder this is
         */
        abstract fun getThis(): T

        /**
         * Build an instance of AuthListWebAuthnCredentialsOptions (or one of its subclasses).
         * @return an instance of AuthListWebAuthnCredentialsOptions (or one of its subclasses)
         */
        abstract fun build(): AuthListWebAuthnCredentialsOptions
    }

    private class DefaultAuthListWebAuthnCredentialsOptions : AuthListWebAuthnCredentialsOptions() {
        override fun hashCode() = javaClass.hashCode()
        override fun toString() = javaClass.simpleName
        override fun equals(other: Any?) = other is DefaultAuthListWebAuthnCredentialsOptions
    }
}
