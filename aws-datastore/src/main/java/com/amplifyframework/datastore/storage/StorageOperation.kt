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
