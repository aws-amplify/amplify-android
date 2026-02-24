package com.amplifyframework.kinesis

import android.content.Context
import aws.sdk.kotlin.services.kinesis.KinesisClient
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toSmithyProvider
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.exceptionOrNull
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.isSuccess
import com.amplifyframework.foundation.result.mapFailure
import com.amplifyframework.recordcache.AutoFlushScheduler
import com.amplifyframework.recordcache.ClearCacheResult
import com.amplifyframework.recordcache.FlushResult
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.recordcache.FlushStrategy.Interval
import com.amplifyframework.recordcache.RecordClient
import com.amplifyframework.recordcache.RecordData
import com.amplifyframework.recordcache.RecordInput
import com.amplifyframework.recordcache.RecordResult
import com.amplifyframework.recordcache.SQLiteRecordStorage
import kotlin.system.measureTimeMillis

/**
 * Kinesis supports up to 500 records per stream.
 * See [the docs](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecords.html)
 */
private const val MAX_RECORDS_PER_STREAM = 500

/**
 * A client for sending data to Amazon Kinesis Data Streams.
 *
 * Provides automatic batching, retry logic, and local caching for high-throughput
 * data streaming to Kinesis with configurable flush strategies.
 *
 * Example usage:
 * ```kotlin
 * // Bridge V2 Auth to foundation credentials via Smithy types
 * val credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()
 *
 * val kinesis = AmplifyKinesisClient(
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
 * @param credentialsProvider AWS credentials for authentication. Use
 *   `CognitoCredentialsProvider().toAwsCredentialsProvider()` to bridge from V2 Auth.
 * @param options Configuration options with sensible defaults
 */
@OptIn(InternalAmplifyApi::class)
class AmplifyKinesisClient(
    val context: Context,
    val region: String,
    val credentialsProvider: AwsCredentialsProvider<out AwsCredentials>,
    options: AmplifyKinesisClientOptions = AmplifyKinesisClientOptions.defaults()
) {
    private val logger: Logger = AmplifyLogging.logger<AmplifyKinesisClient>()

    /** The underlying SDK [KinesisClient] for direct access. */
    val kinesisClient: KinesisClient = KinesisClient {
        this.region = this@AmplifyKinesisClient.region
        this.credentialsProvider = this@AmplifyKinesisClient.credentialsProvider.toSmithyProvider()
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
            maxRecordsByStream = MAX_RECORDS_PER_STREAM,
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
     * @return Result.success(RecordData) on success, or Result.failure with:
     *   - [AmplifyKinesisLimitExceededException] (cache full)
     *   - [AmplifyKinesisStorageException] (database errors)
     */
    suspend fun record(data: ByteArray, partitionKey: String, streamName: String): RecordResult {
        if (!isEnabled) {
            logger.debug { "Record collection is disabled, dropping record" }
            return Result.Success(RecordData())
        }
        logger.verbose { "Recording to stream: $streamName" }
        return logOp(
            operation = { recordClient.record(RecordInput(streamName, partitionKey, data)).wrapError() },
            logSuccess = { _, timeMs ->
                logger.debug { "Record completed successfully in ${timeMs}ms" }
            },
            logFailure = { error, timeMs ->
                logger.warn { "Record failed in ${timeMs}ms: ${error?.message}" }
            }
        )
    }

    /**
     * Flushes all cached records to their respective Kinesis streams.
     *
     * @return Result.success(FlushData) on success, or Result.failure with:
     *   - [AmplifyKinesisServiceException] (API failures)
     *   - [AmplifyKinesisStorageException] (database errors)
     *   - [AmplifyKinesisUnknownException] (unexpected failures)
     */
    suspend fun flush(): FlushResult {
        logger.info { "Starting flush" }
        return logOp(
            operation = { recordClient.flush().wrapError() },
            logSuccess = { data, timeMs ->
                logger.info {
                    "Flush completed successfully in ${timeMs}ms - ${data.recordsFlushed} records flushed"
                }
            },
            logFailure = { error, timeMs ->
                logger.warn { "Flush failed in ${timeMs}ms: ${error?.message}" }
            }
        )
    }

    /**
     * Clears all cached records from local storage.
     *
     * @return Result.success(ClearCacheData) on success, or Result.failure with:
     *   - [AmplifyKinesisStorageException] (database errors)
     */
    suspend fun clearCache(): ClearCacheResult {
        logger.info { "Clearing cache" }
        return logOp(
            operation = { recordClient.clearCache().wrapError() },
            logSuccess = { data, timeMs ->
                logger.info {
                    "Clear cache completed successfully in ${timeMs}ms - ${data.recordsCleared} records cleared"
                }
            },
            logFailure = { error, timeMs ->
                logger.warn { "Clear cache failed in ${timeMs}ms: ${error?.message}" }
            }
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

    private fun <T> Result<T, Throwable>.wrapError(): Result<T, AmplifyKinesisException> = mapFailure {
        AmplifyKinesisException.from(it)
    }

    private suspend inline fun <T> logOp(
        operation: suspend () -> Result<T, AmplifyKinesisException>,
        logSuccess: (T, Long) -> Unit,
        logFailure: (Throwable?, Long) -> Unit
    ): Result<T, AmplifyKinesisException> {
        val result: Result<T, AmplifyKinesisException>
        val timeMs = measureTimeMillis {
            result = operation()
        }
        when (result) {
            is Result.Failure -> logFailure(result.error, timeMs)
            is Result.Success -> logSuccess(result.data, timeMs)
        }
        return result
    }
}
