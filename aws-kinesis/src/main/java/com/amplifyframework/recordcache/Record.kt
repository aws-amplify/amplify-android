package com.amplifyframework.recordcache

internal data class RecordInput(
    val streamName: String,
    val partitionKey: String,
    val data: ByteArray,
    val dataSize: Int = data.size
)

internal data class Record(
    val id: Long,
    val streamName: String,
    val partitionKey: String,
    val data: ByteArray,
    val dataSize: Int,
    val retryCount: Int,
    val createdAt: Long
)
