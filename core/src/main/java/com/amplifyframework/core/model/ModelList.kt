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
import com.amplifyframework.core.Consumer
import kotlin.jvm.Throws

/**
 * The base wrapper class for providing a list of models.
 */
sealed interface ModelList<out M : Model>

/**
 * A wrapped list of preloaded models that were included in the selection set.
 */
interface LoadedModelList<out M : Model> : ModelList<M> {

    /** The list of preloaded models. */
    val items: List<M>
}

/**
 * A wrapped list of models that must be fetched.
 */
interface LazyModelList<out M : Model> : ModelList<M> {

    /**
     * Loads the next page of models.
     *
     * @throws AmplifyException when loading the page fails.
     * @param paginationToken the pagination token to use during load.
     * @return the next page of models.
     */
    @JvmSynthetic
    @Throws(AmplifyException::class)
    suspend fun fetchPage(paginationToken: PaginationToken? = null): ModelPage<M>

    /**
     * Loads the next page of models.
     *
     * @param onSuccess called upon successfully loading the next page of models.
     * @param onError called when loading the page fails.
     */
    fun fetchPage(
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>
    )

    /**
     * Loads the next page of models.
     *
     * @param paginationToken the pagination token to use during load.
     * @param onSuccess called upon successfully loading the next page of models.
     * @param onError called when loading the page fails.
     */
    fun fetchPage(
        paginationToken: PaginationToken?,
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>
    )
}

/**
 * Token providing information on the next page to load.
 */
interface PaginationToken

/**
 * A page of loaded models.
 */
interface ModelPage<out M : Model> {

    /** The list of loaded models. */
    val items: List<M>

    /** The token that can be used to load the next page. */
    val nextToken: PaginationToken?

    /** Whether the next page is available. */
    val hasNextPage: Boolean
        get() = nextToken != null
}
