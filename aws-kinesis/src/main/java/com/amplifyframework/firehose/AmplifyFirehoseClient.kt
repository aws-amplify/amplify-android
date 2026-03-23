/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.firehose

import android.content.Context
import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.firehose.FirehoseClient
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toSmithyProvider
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.mapFailure
import com.amplifyframework.foundation.useragent.AmplifyUserAgentInterceptor
import com.amplifyframework.kinesis.BuildConfig
import com.amplifyframework.recordcache.AutoFlushScheduler
import com.amplifyframework.recordcache.ClearCacheData
import com.amplifyframework.recordcache.FlushData
import com.amplifyframework.recordcache.FlushStrategy
import com.amplifyframework.recordcache.RecordClient
import com.amplifyframework.recordcache.RecordData
import com.amplifyframework.recordcache.RecordInput
import com.amplifyframework.recordcache.SQLiteRecordStorage
import com.amplifyframework.recordcache.logOp

typealias FirehoseRecordResult = Result<RecordData, AmplifyFirehoseException>
typealias FirehoseFlushResult = Result<FlushData, AmplifyFirehoseException>
typealias FirehoseClearCacheResult = Result<ClearCacheData, AmplifyFirehoseException>

/**
 * Firehose supports up to 500 records per PutRecords request.
 * See [the docs](https://docs.aws.amazon.com/firehose/latest/APIReference/API_PutRecordBatch.html)
 */
private const val MAX_RECORDS_PER_BATCH = 500

/**
 * Maximum size of a single record (partition key + data blob) in bytes (10 MiB).
 * See [PutRecordsRequestEntry](https://docs.aws.amazon.com/firehose/latest/APIReference/API_PutRecordBatch.html)
 */
private const val MAX_RECORD_SIZE_BYTES = 1_000L * 1_024

/**
 * Maximum total payload size per PutRecords request in bytes (10 MiB).
 * See [PutRecords](https://docs.aws.amazon.com/firehose/latest/APIReference/API_PutRecordBatch.html)
 */
private const val MAX_PUT_RECORD_BATCH_SIZE_BYTES = 4L * 1_024 * 1_024

/**
 * A client for sending data to Amazon Data Firehose.
 *
 * Provides automatic batching, retry logic, and local caching for high-throughput
 * data streaming to Firehose with configurable flush strategies.
 *
 * Example usage:
 * ```kotlin
 * * // Bridge V2 AuthPlugin credentials to foundation credentials via Smithy types
 * val credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()
 *
 * val firehose = AmplifyFirehoseClient(
 *     context = applicationContext,
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider
 * )
 *
 * firehose.record(
 *     data = "Hello Firehose".toByteArray(),
 *     streamName = "my-delivery-stream"
 * )
 *
 * val result = firehose.flush()
 * ```
 * @param context Android application context for database access
 * @param region AWS region where the Kinesis stream is located
 * @param credentialsProvider AWS credentials for authentication. Use
 *   `CognitoCredentialsProvider().toAwsCredentialsProvider()` to bridge from V2 Auth.
 * @param options Configuration options with sensible defaults
 */
@OptIn(InternalAmplifyApi::class)
class AmplifyFirehoseClient(
    context: Context,
    private val region: String,
    private val credentialsProvider: AwsCredentialsProvider<AwsCredentials>,
    @field:VisibleForTesting val options: AmplifyFirehoseClientOptions = AmplifyFirehoseClientOptions.defaults()
) {
    private val logger: Logger = AmplifyLogging.logger<AmplifyFirehoseClient>()

    /** The underlying SDK [FirehoseClient] for direct access. */
    val firehoseClient: FirehoseClient = FirehoseClient {
        this.region = this@AmplifyFirehoseClient.region
        this.credentialsProvider = this@AmplifyFirehoseClient.credentialsProvider.toSmithyProvider()
        options.configureClient?.applyConfiguration(this)
        interceptors += AmplifyUserAgentInterceptor("amplify-firehose", BuildConfig.VERSION_NAME)
    }

    private val recordClient = RecordClient(
        sender = FirehoseRecordSender(firehoseClient, options.maxRetries),
        storage = SQLiteRecordStorage(
            context = context.applicationContext,
            dbPrefix = "firehose_records",
            identifier = region,
            maxRecordsByStream = MAX_RECORDS_PER_BATCH,
            cacheMaxBytes = options.cacheMaxBytes,
            maxRecordSizeBytes = MAX_RECORD_SIZE_BYTES,
            maxBytesPerStream = MAX_PUT_RECORD_BATCH_SIZE_BYTES
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
     * Records data to the specified Firehose stream.
     *
     * @param data The data to record as byte array
     * @param partitionKey The partition key for the record
     * @param streamName The name of the Kinesis stream
     * @return Result.Success(RecordData) on success, or Result.Failure with:
     *   - [AmplifyKinesisLimitExceededException] (cache full)
     *   - [AmplifyKinesisStorageException] (database errors)
     */
    suspend fun record(data: ByteArray, streamName: String): FirehoseRecordResult {
        if (!isEnabled) {
            logger.debug { "Record collection is disabled, dropping record" }
            return Result.Success(RecordData())
        }
        return logOp(
            operation = { recordClient.record(RecordInput(streamName = streamName, data = data)).wrapError() },
            logSuccess = { _, timeMs -> logger.debug { "Record completed in ${timeMs}ms" } },
            logFailure = { error, timeMs -> logger.warn { "Record failed in ${timeMs}ms: ${error?.message}" } }
        )
    }

    /**
     * Flushes all locally stored records to their respective Firehose streams.
     *
     * Each flush drains all pending records in batches per stream, limited by the
     * respecite constraints (up to 500 records or 4 MB per batch).
     * Progress is tracked per stream so that records already attempted in the
     * current flush cycle are not sent again. Failed records have their retry
     * count incremented and are picked up in the next flush cycle.
     *
     * Records that exceed [AmplifyFirehoseClientOptions.maxRetries] are removed from the cache.
     *
     * SDK Firehose errors (throttling, invalid stream, etc.) are logged and skipped so
     * other streams can still flush. Non-SDK errors (e.g. network, storage) abort the
     * flush and are returned as a failure.
     *
     * If a flush is already in progress, the call returns immediately with
     * `FlushData(recordsFlushed = 0, flushInProgress = true)`.
     *
     * @return Result.Success(FlushData) on success, or Result.Failure with:
     *   - [AmplifyFirehoseStorageException] (database errors)
     *   - [AmplifyFirehoseUnknownException] (unexpected failures)
     */
    suspend fun flush(): FirehoseFlushResult = logOp(
        operation = { recordClient.flush().wrapError() },
        logSuccess = { data, timeMs ->
            logger.debug {
                "Flush completed in ${timeMs}ms - ${data.recordsFlushed} records flushed"
            }
        },
        logFailure = { error, timeMs ->
            logger.warn { "Flush failed in ${timeMs}ms: ${error?.message}" }
        }
    )

    /**
     * Clears all cached records from local storage.
     *
     * @return Result.Success(ClearCacheData) on success, or Result.Failure with:
     *   - [AmplifyFirehoseStorageException] (database errors)
     */
    suspend fun clearCache(): FirehoseClearCacheResult = logOp(
        operation = { recordClient.clearCache().wrapError() },
        logSuccess = { data, timeMs ->
            logger.debug { "Clear cache completed in ${timeMs}ms - ${data.recordsCleared} records cleared" }
        },
        logFailure = { error, timeMs ->
            logger.warn { "Clear cache failed in ${timeMs}ms: ${error?.message}" }
        }
    )

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

    private fun <T> Result<T, Throwable>.wrapError(): Result<T, AmplifyFirehoseException> = mapFailure {
        AmplifyFirehoseException.from(it)
    }
}
