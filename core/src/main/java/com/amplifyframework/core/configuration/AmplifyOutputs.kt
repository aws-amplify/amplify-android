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

package com.amplifyframework.core.configuration

import androidx.annotation.RawRes

/**
 * Base interface for types that represent some form of the outputs from the Amplify Gen2 CLI.
 * Instances can be created by using on of the static factory methods, or by using one of the function overloads
 * (Kotlin only).
 */
sealed interface AmplifyOutputs {
    companion object {
        /**
         * Create an [AmplifyOutputs] from a resource file. The given resource identifier will be read and parsed when
         * calling Amplify.configure.
         * @param resourceId The resource ID for a file containing JSON data that adheres to the amplify_outputs schema
         * @return An instance of [AmplifyOutputs] that can be passed to Amplify.configure
         */
        @JvmStatic
        fun fromResource(@RawRes resourceId: Int): AmplifyOutputs = AmplifyOutputsResource(resourceId)

        /**
         * Create an [AmplifyOutputs] from a String containing JSON data. The given String will be parsed when calling
         * Amplify.configure.
         * @param json The String representation of JSON adhering to the amplify_outputs schema
         * @return An instance of [AmplifyOutputs] that can be passed to Amplify.configure
         */
        @JvmStatic
        fun fromString(json: String): AmplifyOutputs = AmplifyOutputsString(json)
    }
}

/**
 * Convenience function for instantiating an [AmplifyOutputs] instance with a resource identifier.
 * @param resourceId The resource ID for a file containing JSON data that adheres to the amplify_outputs schema
 * @return An instance of [AmplifyOutputs] that can be passed to Amplify.configure
 */
@JvmSynthetic // Java should use AmplifyOutputs.fromResource(resourceId)
fun AmplifyOutputs(@RawRes resourceId: Int) = AmplifyOutputs.fromResource(resourceId)

/**
 * Convenience function for instantiating an [AmplifyOutputs] instance with a JSON String.
 * @param json The String representation of JSON adhering to the amplify_outputs schema
 * @return An instance of [AmplifyOutputs] that can be passed to Amplify.configure
 */
@JvmSynthetic // Java should use AmplifyOutputs.fromString(json)
fun AmplifyOutputs(json: String) = AmplifyOutputs.fromString(json)

// Concrete classes for passing different forms of AmplifyOutputs to Amplify.configure
internal data class AmplifyOutputsResource(@RawRes val resourceId: Int) : AmplifyOutputs
internal data class AmplifyOutputsString(val json: String) : AmplifyOutputs
