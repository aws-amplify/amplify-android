package com.amplifyframework.auth.cognito.data

import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueRepository : KeyValueRepository {
    private val cache = ConcurrentHashMap<String, String?>()

    override fun put(dataKey: String, value: String?) {
        value?.run { cache.put(dataKey, value) }
    }

    override fun get(dataKey: String): String? = cache.getOrDefault(dataKey, null)

    override fun remove(dataKey: String) {
        cache.remove(dataKey)
    }
}
