package com.amplifyframework.core.model


public interface ILazyList<M: Model> {
    val value: M

    suspend fun get(): List<M>?

    suspend fun require(): List<M> {
        return get() ?: throw DataIntegrityException("Required model could not be found")
    }
}