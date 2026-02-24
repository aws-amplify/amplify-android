package com.amplifyframework.recordcache

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.exceptionOrNull
import com.amplifyframework.foundation.result.isSuccess
import com.amplifyframework.foundation.result.mapFailure
import com.amplifyframework.foundation.result.resultCatching
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(InternalAmplifyApi::class)
internal class SQLiteRecordStorage internal constructor(
    maxRecordsByStream: Int,
    maxBytes: Long,
    identifier: String,
    connectionFactory: () -> SQLiteConnection,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : RecordStorage(maxRecordsByStream, maxBytes, identifier) {
    private val logger: Logger = AmplifyLogging.logger<SQLiteRecordStorage>()
    private val connection: SQLiteConnection = connectionFactory()
    private var cachedSize = AtomicInteger(0)
    private val dbMutex = Mutex()
    private val maxRecordsByStream = maxRecordsByStream

    constructor(
        context: Context,
        maxRecordsByStream: Int,
        maxBytes: Long,
        identifier: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(maxRecordsByStream, maxBytes, identifier, {
        val dbFile = File(context.getDatabasePath("kinesis_records_$identifier.db").absolutePath)
        BundledSQLiteDriver().open(dbFile.absolutePath)
    }, dispatcher)

    init {
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
    private suspend fun <T> wrapDispatchAndCatching(block: () -> T): Result<T, Throwable> = resultCatching {
        withContext(dispatcher) {
            dbMutex.withLock {
                block()
            }
        }
    }

    /**
     * Helper to wrap DB queries in a transaction and suspend
     */
    private suspend fun <T> wrapDispatchAndTransactionAndCatching(block: () -> T): Result<T, Throwable> =
        wrapDispatchAndCatching {
            connection.execSQL("BEGIN IMMEDIATE TRANSACTION")
            try {
                val result = block()
                connection.execSQL("END TRANSACTION")
                result
            } catch (e: Exception) {
                connection.execSQL("ROLLBACK TRANSACTION")
                throw e
            }
        }

    override suspend fun addRecord(record: RecordInput): Result<Unit, RecordCacheException> = wrapDispatchAndCatching {
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
        Unit
    }.recoverAsRecordCacheException("Failed to add record to cache")

    override suspend fun getRecordsByStream(): Result<List<List<Record>>, RecordCacheException> =
        wrapDispatchAndTransactionAndCatching {
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
                stmt.bindInt(1, maxRecordsByStream)
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
        }.recoverAsRecordCacheException("Could not retrieve records from storage")

    override suspend fun deleteRecords(ids: List<Long>): Result<Unit, RecordCacheException> = wrapDispatchAndCatching {
        if (ids.isNotEmpty()) {
            val placeholders = ids.joinToString(",") { "?" }

            connection.prepare("DELETE FROM records WHERE id IN ($placeholders)").use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.bindLong(index + 1, id)
                }
                stmt.step()
            }
            resetCacheSizeFromDb()
        }
    }.recoverAsRecordCacheException("Failed to delete records from cache")

    override suspend fun incrementRetryCount(ids: List<Long>): Result<Unit, RecordCacheException> =
        wrapDispatchAndCatching {
            if (ids.isNotEmpty()) {
                val placeholders = ids.joinToString(",") { "?" }
                connection.prepare(
                    "UPDATE records SET retry_count = retry_count + 1 WHERE id IN ($placeholders)"
                ).use { stmt ->
                    ids.forEachIndexed { index, id ->
                        stmt.bindLong(index + 1, id)
                    }
                    stmt.step()
                }
            }
        }.recoverAsRecordCacheException("Failed to increment retry count")

    /**
     * Resets the cached size by recalculating from the database.
     * Use when manual tracking might be out of sync.
     */
    @VisibleForTesting
    internal fun resetCacheSizeFromDb() {
        cachedSize.set(
            connection.prepare("SELECT COALESCE(SUM(data_size), 0) FROM records").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }.toInt()
        )
    }

    override suspend fun getCurrentCacheSize(): Result<Int, RecordCacheException> = Result.Success(cachedSize.toInt())

    override suspend fun clearRecords(): Result<ClearCacheData, RecordCacheException> =
        wrapDispatchAndTransactionAndCatching {
            val count = connection.prepare("SELECT COUNT(*) FROM records").use { stmt ->
                if (stmt.step()) stmt.getInt(0) else 0
            }

            connection.execSQL("DELETE FROM records")
            cachedSize.set(0)
            ClearCacheData(count)
        }.recoverAsRecordCacheException("Failed to clear cache")

    private fun <R> Result<R, Throwable>.recoverAsRecordCacheException(
        message: String
    ): Result<R, RecordCacheException> = mapFailure { exception ->
        when(exception) {
            is RecordCacheException -> exception
            else -> RecordCacheDatabaseException(message, DEFAULT_RECOVERY_SUGGESTION, exception)
        }
    }
}
