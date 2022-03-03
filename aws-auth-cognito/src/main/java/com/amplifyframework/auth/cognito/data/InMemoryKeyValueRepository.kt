package com.amplifyframework.auth.cognito.data

import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueRepository : KeyValueRepository {
    private val cache = ConcurrentHashMap<String, String?>()

    override fun put(dataKey: Any, value: Any?) {
        value?.run { cache.put(dataKey.toString(), value.toString()) }
    }

    override fun get(dataKey: Any): Any? = cache.getOrDefault(dataKey, null)

    override fun remove(dataKey: String) {
        cache.remove(dataKey)
    }

}
