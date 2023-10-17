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

package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NullableConsumer

/**
 * Base interface for a class holding a Model type
 */
sealed interface ModelReference<M : Model> {
    @InternalAmplifyApi
    fun getIdentifier(): Map<String, Any>
}

/**
 * A reference holder that holds an in-memory model
 */
interface LoadedModelReference<M : Model> : ModelReference<M> {

    /** The loaded model value */
    val value: M?
}

/**
 * A reference holder that allows lazily loading a model
 */
interface LazyModelReference<M : Model> : ModelReference<M> {

    /**
     * Load the model instance represented by this LazyModelReference.
     *
     * @throws AmplifyException If loading the model fails.
     * @return The lazily loaded model or null if no such model exists.
     */
    @JvmSynthetic
    @Throws(AmplifyException::class)
    suspend fun fetchModel(): M?

    /**
     * Load the model instance represented by this LazyModelReference.
     *
     * @param onSuccess Called upon successfully loading the model.
     * @param onError Called when loading the model fails.
     */
    fun fetchModel(onSuccess: NullableConsumer<M?>, onError: Consumer<AmplifyException>)
}
