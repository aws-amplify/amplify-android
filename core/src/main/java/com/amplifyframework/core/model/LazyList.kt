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
import com.amplifyframework.api.ApiException
import com.amplifyframework.core.Consumer

abstract class LazyList<M : Model> {
    abstract fun getItems(): List<M>

    @Throws(ApiException::class)
    abstract suspend fun getNextPage(): List<M>

    suspend fun require(): List<M> {
        return getNextPage() ?: throw DataIntegrityException("Required model could not be found")
    }

    abstract fun getNextPage(onSuccess: Consumer<List<M>>,
                     onFailure: Consumer<AmplifyException>)
    
    abstract fun hasNextPage(): Boolean
}
