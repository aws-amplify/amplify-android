package com.amplifyframework.recordcache

data class RecordData(val success: Boolean = true)
data class FlushData(val recordsFlushed: Int = 0)
data class ClearCacheData(val recordsCleared: Int = 0)

typealias RecordResult = Result<RecordData>
typealias FlushResult = Result<FlushData>
typealias ClearCacheResult = Result<ClearCacheData>
