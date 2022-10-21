package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer


abstract class LazyList<M: Model> {
    abstract fun getValue():List<M>?

    abstract suspend fun get(): List<M>?

    suspend fun require(): List<M> {
        return get() ?: throw DataIntegrityException("Required model could not be found")
    }

    abstract fun get(onSuccess: Consumer<List<M>>,
                     onFailure: Consumer<AmplifyException>)
}