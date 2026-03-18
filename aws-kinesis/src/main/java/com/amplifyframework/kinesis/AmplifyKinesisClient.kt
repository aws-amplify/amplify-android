package com.amplifyframework.kinesis

import android.content.Context
import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.kinesis.KinesisClient
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toSmithyProvider
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.mapFailure
import com.amplifyframework.foundation.useragent.AmplifyUserAgentInterceptor
import com.amplifyframework.recordcache.AutoFlushScheduler
import com.amplifyframework.recordcache.ClearCacheResult
import com.amplifyframework.recordcache.FlushResult
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.recordcache.RecordClient
import com.amplifyframework.recordcache.RecordData
import com.amplifyframework.recordcache.RecordInput
import com.amplifyframework.recordcache.RecordResult
import com.amplifyframework.recordcache.SQLiteRecordStorage
import com.amplifyframework.recordcache.logOp

/**
 * Kinesis supports up to 500 records per PutRecords request.
 * See [the docs](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecords.html)
 */
private const val MAX_RECORDS_PER_STREAM = 500

/**
 * Maximum size of a single record (partition key + data blob) in bytes (10 MiB).
 * See [PutRecordsRequestEntry](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecordsRequestEntry.html)
 */
private const val MAX_RECORD_SIZE_BYTES = 10L * 1_024 * 1_024

/**
 * Maximum total payload size per PutRecords request in bytes (10 MiB).
 * See [PutRecords](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecords.html)
 */
private const val MAX_PUT_RECORDS_SIZE_BYTES = 10L * 1_024 * 1_024

/**
 * Maximum length of a partition key in Unicode code points.
 * See [PutRecordsRequestEntry](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecordsRequestEntry.html)
 */
private const val MAX_PARTITION_KEY_LENGTH = 256

/**
 * A client for sending data to Amazon Kinesis Data Streams.
 *
 * Provides automatic batching, retry logic, and local caching for high-throughput
 * data streaming to Kinesis with configurable flush strategies.
 *
 * Example usage:
 * ```kotlin
 * // Bridge V2 AuthPlugin credentials to foundation credentials via Smithy types
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
 *     partitionKey = "partition-1",
 *     streamName = "my-stream"
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
    context: Context,
    private val region: String,
    private val credentialsProvider: AwsCredentialsProvider<AwsCredentials>,
    @field:VisibleForTesting val options: AmplifyKinesisClientOptions = AmplifyKinesisClientOptions.defaults()
) {
    private val logger: Logger = AmplifyLogging.logger<AmplifyKinesisClient>()

    /** The underlying SDK [KinesisClient] for direct access. */
    val kinesisClient: KinesisClient = KinesisClient {
        this.region = this@AmplifyKinesisClient.region
        this.credentialsProvider = this@AmplifyKinesisClient.credentialsProvider.toSmithyProvider()
        options.configureClient?.applyConfiguration(this)
        interceptors += AmplifyUserAgentInterceptor("amplify-kinesis", BuildConfig.VERSION_NAME)
    }

    private val recordClient: RecordClient = RecordClient(
        sender = KinesisRecordSender(
            kinesisClient = kinesisClient,
            maxRetries = options.maxRetries
        ),
        storage = SQLiteRecordStorage(
            context = context.applicationContext,
            dbPrefix = "kinesis_records",
            identifier = region,
            maxRecordsByStream = MAX_RECORDS_PER_STREAM,
            cacheMaxBytes = options.cacheMaxBytes,
            maxRecordSizeBytes = MAX_RECORD_SIZE_BYTES,
            maxBytesPerStream = MAX_PUT_RECORDS_SIZE_BYTES,
            maxPartitionKeyLength = MAX_PARTITION_KEY_LENGTH
        ),
        maxRetries = options.maxRetries
    )
    private val scheduler: AutoFlushScheduler?

    @Volatile private var isEnabled = true

    init {
        scheduler = when (options.flushStrategy) {
            is FlushStrategy.Interval -> AutoFlushScheduler(
                options.flushStrategy,
                client = recordClient
            )
            is FlushStrategy.None -> null
        }

        // Auto-start the scheduler if present
        scheduler?.start()
    }

    /**
     * Records data to the specified Kinesis stream.
     *
     * @param data The data to record as byte array
     * @param partitionKey The partition key for the record
     * @param streamName The name of the Kinesis stream
     * @return Result.Success(RecordData) on success, or Result.Failure with:
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
     * Flushes cached records to their respective Kinesis streams.
     *
     * Each invocation sends at most one batch per stream, limited by the Kinesis
     * `PutRecords` constraints (up to 500 records or 10 MB per stream). If the cache
     * contains more records than a single batch can hold, the remaining records are
     * sent on subsequent flush invocations — either manually or via the auto-flush
     * scheduler.
     *
     * Records that fail within a batch are marked for retry on the next flush. Records
     * that exceed [AmplifyKinesisClientOptions.maxRetries] are removed from the cache.
     *
     * SDK Kinesis errors (throttling, invalid stream, etc.) are logged and skipped so
     * other streams can still flush. Non-SDK errors (e.g. network, storage) abort the
     * flush and are returned as a failure.
     *
     * If a flush is already in progress, the call returns immediately with
     * `FlushData(recordsFlushed = 0, flushInProgress = true)`.
     *
     * @return Result.Success(FlushData) on success, or Result.Failure with:
     *   - [AmplifyKinesisStorageException] (database errors)
     *   - [AmplifyKinesisUnknownException] (unexpected failures)
     */
    suspend fun flush(): FlushResult {
        logger.verbose { "Starting flush" }
        return logOp(
            operation = { recordClient.flush().wrapError() },
            logSuccess = { data, timeMs ->
                logger.debug {
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
     * @return Result.Success(ClearCacheData) on success, or Result.Failure with:
     *   - [AmplifyKinesisStorageException] (database errors)
     */
    suspend fun clearCache(): ClearCacheResult {
        logger.verbose { "Clearing cache" }
        return logOp(
            operation = { recordClient.clearCache().wrapError() },
            logSuccess = { data, timeMs ->
                logger.debug {
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
        logger.info { "Enabling record collection and automatic flushing" }
        isEnabled = true
        scheduler?.start()
    }

    /**
     * Disables record collection and automatic flushing. Records submitted while
     * disabled are silently dropped. Already-cached records remain in storage.
     */
    fun disable() {
        logger.info { "Disabling record collection and automatic flushing" }
        isEnabled = false
        scheduler?.disable()
    }

    private fun <T> Result<T, Throwable>.wrapError(): Result<T, AmplifyKinesisException> = mapFailure {
        AmplifyKinesisException.from(it)
    }
}
