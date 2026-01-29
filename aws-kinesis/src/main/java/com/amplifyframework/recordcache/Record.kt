package com.amplifyframework.recordcache

data class RecordInput(
    val streamName: String,
    val partitionKey: String,
    val data: ByteArray,
    val dataSize: Int = data.size
)

data class Record(
    val id: Long,
    val streamName: String,
    val partitionKey: String,
    val data: ByteArray,
    val dataSize: Int,
    val retryCount: Int,
    val createdAt: Long
)
