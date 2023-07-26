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

package com.amplifyframework.predictions.options

import com.amplifyframework.annotations.InternalAmplifyApi

@InternalAmplifyApi
open class FaceLivenessSessionOptions protected constructor() {
    companion object {

        /**
         * Use the default options.
         * @return Default options.
         */
        @JvmStatic
        fun defaults() = FaceLivenessSessionOptions()

        /**
         * Get a builder to construct an instance of this object.
         * @return a builder to construct an instance of this object.
         */
        @JvmStatic
        fun builder(): Builder<*> = CoreBuilder()
    }

    override fun hashCode() = javaClass.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else !(other == null || javaClass != other.javaClass)
    }

    override fun toString(): String {
        return "FaceLivenessSessionOptions()"
    }

    abstract class Builder<T : Builder<T>> {

        /**
         * Return the type of builder this is so that chaining can work correctly without implicit casting.
         * @return the type of builder this is
         */
        abstract fun getThis(): T

        /**
         * Build an instance of FaceLivenessSessionOptions (or one of its subclasses).
         * @return an instance of FaceLivenessSessionOptions (or one of its subclasses)
         */
        open fun build() = FaceLivenessSessionOptions()
    }

    class CoreBuilder : Builder<CoreBuilder>() {
        override fun getThis() = this
    }
}
