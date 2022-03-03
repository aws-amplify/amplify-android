package com.amplifyframework.auth.cognito.data

interface KeyValueRepository {
    fun put(dataKey: Any, value: Any?)
    fun get(dataKey: Any): Any?
    fun remove(dataKey: String)
}

