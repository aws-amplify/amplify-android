package com.amplifyframework.recordcache

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SQLiteRecordStorage internal constructor(
    maxRecords: Int,
    maxBytes: Long,
    identifier: String,
    connectionFactory: () -> SQLiteConnection,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : RecordStorage(maxRecords, maxBytes, identifier) {
    private val connection: SQLiteConnection = connectionFactory()
    private var cachedSize = AtomicInteger(0)
    private val dbMutex = Mutex()

    constructor(
        context: Context,
        maxRecords: Int,
        maxBytes: Long,
        identifier: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(maxRecords, maxBytes, identifier, {
        val dbFile = File(context.getDatabasePath("kinesis_records_$identifier.db").absolutePath)
        BundledSQLiteDriver().open(dbFile.absolutePath)
    }, dispatcher)

    init {
        // We rely on transactions to ensure thread safety. Hence set generous timeout
        connection.execSQL("PRAGMA busy_timeout = 5000")

        // Create DB
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stream_name TEXT NOT NULL,
                partition_key TEXT NOT NULL,
                data BLOB NOT NULL,
                data_size INTEGER NOT NULL,
                retry_count INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        // Create indices for performance
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_stream_id ON records (stream_name, id)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_data_size ON records (data_size)")

        // Initialize cached size from database
        resetCacheSizeFromDb()
    }

    /**
     * Helper to wrap DB queries with locking and dispatch
     */
    private suspend fun <T> wrapDispatchAndLockingAndCatching(block: () -> T) = Result.runCatching {
        withContext(dispatcher) {
            dbMutex.withLock {
                block()
            }
        }
    }

    /**
     * Helper to wrap DB queries in a transaction and suspend
     */
    private suspend fun <T> wrapDispatchAndTransactionAndCatching(block: () -> T) = Result.runCatching {
        withContext(dispatcher) {
            dbMutex.withLock {
                connection.execSQL("BEGIN IMMEDIATE TRANSACTION")
                try {
                    val result = block()
                    connection.execSQL("END TRANSACTION")
                    return@withLock result
                } catch (e: Exception) {
                    connection.execSQL("ROLLBACK TRANSACTION")
                    throw e
                }
            }
        }
    }

    override suspend fun addRecord(record: RecordInput): Result<Unit> = wrapDispatchAndLockingAndCatching {
        // Check cache size limit before adding
        if (cachedSize.get() + record.dataSize > maxBytes) {
            throw RecordCacheLimitExceededException(
                "Cache size limit exceeded: ${cachedSize.get() + record.dataSize} bytes > $maxBytes bytes",
                "Call flush() to send cached records or increase cache size limit"
            )
        }

        connection.prepare(
            "INSERT INTO records (stream_name, partition_key, data, data_size) VALUES (?, ?, ?, ?)"
        ).use { stmt ->
            stmt.bindText(1, record.streamName)
            stmt.bindText(2, record.partitionKey)
            stmt.bindBlob(3, record.data)
            stmt.bindInt(4, record.dataSize)
            stmt.step()
        }
        cachedSize.addAndGet(record.dataSize)

        return@wrapDispatchAndLockingAndCatching
    }.recoverAsRecordCacheException(
        "Failed to add record to cache",
        "Check database permissions and storage space"
    )

    override suspend fun getRecordsByStream(): Result<List<List<Record>>> = wrapDispatchAndTransactionAndCatching {
        connection.prepare(
            """
                SELECT id, stream_name, partition_key, data, data_size, retry_count, created_at
                FROM (
                    SELECT *, 
                           ROW_NUMBER() OVER (PARTITION BY stream_name ORDER BY id) as rn,
                           SUM(data_size) OVER (PARTITION BY stream_name ORDER BY id) as running_size
                    FROM records
                ) 
                WHERE rn <= ? AND running_size <= ?
                ORDER BY stream_name, id
            """
        ).use { stmt ->
            stmt.bindInt(1, maxRecords)
            stmt.bindLong(2, maxBytes)

            val recordsByStream = mutableMapOf<String, MutableList<Record>>()

            while (stmt.step()) {
                val streamName = stmt.getText(1)

                recordsByStream.getOrPut(streamName) { mutableListOf() }.add(
                    Record(
                        id = stmt.getLong(0),
                        streamName = streamName,
                        partitionKey = stmt.getText(2),
                        data = stmt.getBlob(3),
                        dataSize = stmt.getInt(4),
                        retryCount = stmt.getInt(5),
                        createdAt = stmt.getLong(6)
                    )
                )
            }

            recordsByStream.values.toList()
        }
    }.recoverAsRecordCacheException("Could not retrieve records from storage", "Try again at a later time")

    override suspend fun deleteRecords(ids: List<Long>): Result<Unit> = wrapDispatchAndLockingAndCatching {
        if (!ids.isEmpty()) {
            val placeholders = ids.joinToString(",") { "?" }

            connection.prepare("DELETE FROM records WHERE id IN ($placeholders)").use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.bindLong(index + 1, id)
                }
                stmt.step()
            }
            resetCacheSizeFromDb()
        }
    }.recoverAsRecordCacheException(
        "Failed to delete records from cache",
        "Try again at a later time"
    )

    override suspend fun incrementRetryCount(ids: List<Long>): Result<Unit> = wrapDispatchAndLockingAndCatching {
        if (!ids.isEmpty()) {
            val placeholders = ids.joinToString(",") { "?" }
            connection.prepare(
                "UPDATE records SET retry_count = retry_count + 1 WHERE id IN ($placeholders)"
            ).use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.bindLong(index + 1, id)
                }
                stmt.step()
            }
            return@wrapDispatchAndLockingAndCatching
        }
    }.recoverAsRecordCacheException(
        "Failed to increment retry count",
        "Try again at a later time"
    )

    /**
     * Resets the cached size by recalculating from the database.
     * Use when manual tracking might be out of sync.
     */
    private fun resetCacheSizeFromDb() {
        cachedSize.set(
            connection.prepare("SELECT COALESCE(SUM(data_size), 0) FROM records").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }.toInt()
        )
    }

    override suspend fun getCurrentCacheSize(): Result<Int> = Result.success(cachedSize.toInt())

    override suspend fun clearRecords(): Result<ClearCacheData> = wrapDispatchAndTransactionAndCatching {
        val count = connection.prepare("SELECT COUNT(*) FROM records").use { stmt ->
            if (stmt.step()) stmt.getInt(0) else 0
        }

        connection.execSQL("DELETE FROM records")
        cachedSize.set(0)
        return@wrapDispatchAndTransactionAndCatching ClearCacheData(count)
    }.recoverAsRecordCacheException(
        "Failed to clear cache",
        "Try again at a later time"
    )
}

private fun <R> Result<R>.recoverAsRecordCacheException(message: String, recoverySuggestion: String): Result<R> {
    if (this.isSuccess) {
        return this
    }

    val transformedException = when (val exception = this.exceptionOrNull()) {
        is RecordCacheException -> exception
        else -> RecordCacheStorageException(
            message,
            recoverySuggestion,
            exception
        )
    }
    return Result.failure(transformedException)
}
