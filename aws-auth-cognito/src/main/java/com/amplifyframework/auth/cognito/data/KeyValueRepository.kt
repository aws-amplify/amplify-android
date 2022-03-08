package com.amplifyframework.auth.cognito.data

interface KeyValueRepository {
    fun put(dataKey: String, value: String?)
    fun get(dataKey: String): String?
    fun remove(dataKey: String)
}

