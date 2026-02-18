package com.amplifyframework.kinesis

import android.content.Context
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.auth.convertToSdkCredentialsProvider
import com.amplifyframework.recordcache.AutoFlushScheduler
import com.amplifyframework.recordcache.ClearCacheResult
import com.amplifyframework.recordcache.FlushResult
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.recordcache.FlushStrategy.Interval
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.Logger
import com.amplifyframework.recordcache.RecordClient
import com.amplifyframework.recordcache.RecordInput
import com.amplifyframework.recordcache.RecordResult
import com.amplifyframework.recordcache.SQLiteRecordStorage
import aws.sdk.kotlin.services.kinesis.KinesisClient
import kotlin.system.measureTimeMillis

/**
 * A client for sending data to Amazon Kinesis Data Streams.
 *
 * Provides automatic batching, retry logic, and local caching for high-throughput
 * data streaming to Kinesis with configurable flush strategies.
 *
 * Example usage:
 * ```kotlin
 * val kinesis = KinesisDataStreams(
 *     context = applicationContext,
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider
 * )
 * 
 * // Record data
 * kinesis.record(
 *     data = "Hello Kinesis".toByteArray(),
 *     streamName = "my-stream",
 *     partitionKey = "partition-1"
 * )
 * 
 * // Flush cached records
 * val result = kinesis.flush()
 * ```
 *
 * @param context Android application context for database access
 * @param region AWS region where the Kinesis stream is located
 * @param credentialsProvider AWS credentials for authentication
 * @param options Configuration options with sensible defaults
 */
class KinesisDataStreams(
    val context: Context,
    val region: String,
    val credentialsProvider: AWSCredentialsProvider<AWSCredentials>, // TODO: Pending V3 types
    options: KinesisDataStreamsOptions = KinesisDataStreamsOptions.defaults()
) {
    private val logger: Logger = Amplify.Logging.logger(CategoryType.ANALYTICS, "KinesisDataStreams")

    /** The underlying SDK [KinesisClient] for direct access. */
    val kinesisClient: KinesisClient = KinesisClient {
        this.region = this@KinesisDataStreams.region
        this.credentialsProvider = convertToSdkCredentialsProvider(this@KinesisDataStreams.credentialsProvider)
        options.configureClient?.applyConfiguration(this)
    }

    private val recordClient: RecordClient = RecordClient(
        sender = KinesisRecordSender(
            kinesisClient = kinesisClient,
            maxRetries = options.maxRetries
        ),
        storage = SQLiteRecordStorage(
            context = context,
            identifier = region,
            maxRecords = options.maxRecords,
            maxBytes = options.cacheMaxBytes
        )
    )
    private val scheduler: AutoFlushScheduler
    @Volatile private var isEnabled = false

    init {
        if (options.flushStrategy is FlushStrategy.Interval) {
            scheduler = AutoFlushScheduler(
                options.flushStrategy,
                client = recordClient
            )
        } else {
            throw IllegalArgumentException("Flush strategy must be interval")
        }
    }

    /**
     * Records data to the specified Kinesis stream.
     *
     * @param data The data to record as byte array
     * @param partitionKey The partition key for the record
     * @param streamName The name of the Kinesis stream
     * @return Result.success(Unit) on success, or Result.failure with:
     *   - [KinesisLimitExceededException] (cache full)
     *   - [KinesisStorageException] (database errors)
     */
    suspend fun record(data: ByteArray, partitionKey: String, streamName: String): Result<Unit> {
        if (!isEnabled) {
            logger.debug("Record collection is disabled, dropping record")
            return Result.success(Unit)
        }
        logger.verbose { "Recording to stream: $streamName" }
        return logOp(
            operation = { recordClient.record(RecordInput(streamName, partitionKey, data)).map { }.wrapError() },
            logSuccess = { _, timeMs -> logger.debug("Record completed successfully in ${timeMs}ms") }, // TODO: Use lazy evaluation for log messages
            logFailure = { error, timeMs -> logger.warn("Record failed in ${timeMs}ms: ${error?.message}") } // TODO: Use lazy evaluation for log messages
        )
    }

    /**
     * Flushes all cached records to their respective Kinesis streams.
     *
     * @return Result.success(FlushData) on success, or Result.failure with:
     *   - [KinesisServiceException] (API/network failures)
     *   - [KinesisStorageException] (database errors)
     */
    suspend fun flush(): FlushResult {
        logger.info("Starting flush")
        return logOp(
            operation = { recordClient.flush().wrapError() },
            logSuccess = { data, timeMs -> logger.info("Flush completed successfully in ${timeMs}ms - ${data.recordsFlushed} records flushed") }, // TODO: Use lazy evaluation for log messages
            logFailure = { error, timeMs -> logger.warn("Flush failed in ${timeMs}ms: ${error?.message}") } // TODO: Use lazy evaluation for log messages
        )
    }

    /**
     * Clears all cached records from local storage.
     *
     * @return Result.success(ClearCacheData) on success, or Result.failure with:
     *   - [KinesisStorageException] (database errors)
     */
    suspend fun clearCache(): ClearCacheResult {
        logger.info("Clearing cache")
        return logOp(
            operation = { recordClient.clearCache().wrapError() },
            logSuccess = { data, timeMs -> logger.info("Clear cache completed successfully in ${timeMs}ms - ${data.recordsCleared} records cleared") }, // TODO: Use lazy evaluation for log messages
            logFailure = { error, timeMs -> logger.warn("Clear cache failed in ${timeMs}ms: ${error?.message}") } // TODO: Use lazy evaluation for log messages
        )
    }

    /**
     * Enables record collection and automatic flushing of cached records.
     */
    fun enable() {
        isEnabled = true
        scheduler.start()
    }

    /**
     * Disables record collection and automatic flushing. Records submitted while
     * disabled are silently dropped. Already-cached records remain in storage.
     */
    fun disable() {
        isEnabled = false
        scheduler.disable()
    }

    /** Maps any failure in the [Result] to a [KinesisException] via [KinesisException.from]. */
    private fun <T> Result<T>.wrapError(): Result<T> {
        if (isSuccess) return this
        val error = exceptionOrNull() ?: return this
        return Result.failure(KinesisException.from(error))
    }

    private suspend inline fun <T> logOp(
        operation: suspend () -> Result<T>,
        logSuccess: (T, Long) -> Unit,
        logFailure: (Throwable?, Long) -> Unit
    ): Result<T> {
        val result: Result<T>
        val timeMs = measureTimeMillis {
            result = operation()
        }
        if (result.isSuccess) {
            logSuccess(result.getOrThrow(), timeMs)
        } else {
            logFailure(result.exceptionOrNull(), timeMs)
        }
        return result
    }
}
