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
package com.amplifyframework.datastore.storage

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.DataStoreException

@InternalAmplifyApi
sealed class StorageOperation<out M : Model> private constructor(
    val model: M,
    val onItemChange: Consumer<StorageItemChange<@UnsafeVariance M>>
) {

    internal class Create<M : Model>(
        model: M,
        onItemChange: Consumer<StorageItemChange<M>>
    ) : StorageOperation<M>(model, onItemChange)

    internal class Delete<M : Model>(
        model: M,
        onItemChange: Consumer<StorageItemChange<M>>
    ) : StorageOperation<M>(model, onItemChange)
}

internal sealed class StorageResult<T : Model> {
    class Success<T : Model>(val storageItemChange: StorageItemChange<T>) : StorageResult<T>()
    class Failure<T : Model>(val exception: DataStoreException) : StorageResult<T>()
}
