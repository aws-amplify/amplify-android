package com.amplifyframework.kinesis

import com.amplifyframework.recordcache.FlushStrategy
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES = 500L * 1024 * 1024

data class KinesisDataStreamsOptions internal constructor(
    val cacheMaxBytes: Long,
    val maxRecords: Int,
    val maxRetries: Int,
    val flushStrategy: FlushStrategy
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
        
        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()
        
        @JvmStatic
        fun defaults() = builder().build()
    }
    
    class Builder internal constructor() {
        var cacheMaxBytes: Long = DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES
            @JvmSynthetic set
        
        var maxRecords: Int = 500
            @JvmSynthetic set
        
        var maxRetries: Int = 5
            @JvmSynthetic set
        
        var flushStrategy: FlushStrategy = FlushStrategy.Interval(30.seconds)
            @JvmSynthetic set
        
        fun cacheMaxBytes(value: Long) = apply { cacheMaxBytes = value }
        fun maxRecords(value: Int) = apply { maxRecords = value }
        fun maxRetries(value: Int) = apply { maxRetries = value }
        fun flushStrategy(value: FlushStrategy) = apply { flushStrategy = value }
        
        fun build() = KinesisDataStreamsOptions(cacheMaxBytes, maxRecords, maxRetries, flushStrategy)
    }
}
