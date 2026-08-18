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
package com.amplifyframework.cloudwatch

import android.content.Context
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.cloudwatch.common.models.CloudWatchLogEvent
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toSmithyProvider
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.LogMessage
import com.amplifyframework.foundation.logging.LogSink
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.useragent.AmplifyUserAgentInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Buffer capacity for the [AmplifyCloudWatchClient.events] flow. Kept small: it only needs to
 * absorb short bursts of failure notifications, and events are dropped oldest-first on overflow
 * rather than back-pressuring the emitting coroutine.
 */
private const val EVENTS_BUFFER_CAPACITY = 64

/**
 * Names for the client's own encrypted on-device store (database + passphrase preferences), kept
 * distinct from any other CloudWatch logging store on the device.
 */
private const val CLIENT_DATABASE_NAME = "amplify.cloudwatch.client.db"
private const val CLIENT_PASSPHRASE_PREFERENCES_NAME = "awscloudwatchclientdb"

/**
 * A standalone client for sending log events to Amazon CloudWatch Logs.
 *
 * Provides namespace-based logging with automatic batching, encrypted local persistence, and
 * configurable flush strategies.
 *
 * Implements [LogSink] so it can be registered with [AmplifyLogging.addSink] to capture
 * all framework log messages and forward them to CloudWatch.
 *
 * Use a single client instance per (region, log group). CloudWatch log streams are keyed
 * by device and user identifier, not by client instance, so two clients targeting the same
 * region and log group would write to the same streams and share the same local storage.
 *
 * Example usage:
 * ```kotlin
 * val cloudWatch = AmplifyCloudWatchClient(
 *     context = applicationContext,
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider,
 *     options = AmplifyCloudWatchClientOptions(logGroupName = "/app/my-android-app")
 * )
 *
 * // Register as a sink to capture all AmplifyLogging messages
 * AmplifyLogging.addSink(cloudWatch)
 *
 * cloudWatch.flushLogs()
 * ```
 *
 */
@ExperimentalAmplifyApi
@OptIn(InternalAmplifyApi::class)
class AmplifyCloudWatchClient internal constructor(
    private val cloudWatchLogsClient: CloudWatchLogsClient,
    initialConstraints: LoggingConstraints,
    private val scope: CoroutineScope,
    logManagerFactory: (client: CloudWatchLogsClient, events: MutableSharedFlow<LoggingEvent>) -> CloudWatchLogManager
) : LogSink {

    /**
     * @param context An Android [Context] used to locate the on-device log store
     * @param region The AWS region of the target log group
     * @param credentialsProvider Provides AWS credentials for CloudWatch Logs calls
     * @param options Configuration options for the client
     */
    constructor(
        context: Context,
        region: String,
        credentialsProvider: AwsCredentialsProvider<AwsCredentials>,
        options: AmplifyCloudWatchClientOptions
    ) : this(
        cloudWatchLogsClient = CloudWatchLogsClient {
            this.region = region
            this.credentialsProvider = credentialsProvider.toSmithyProvider()
            options.configureClient?.applyConfiguration(this)
            interceptors += AmplifyUserAgentInterceptor("amplify-cloudwatch", BuildConfig.VERSION_NAME)
        },
        initialConstraints = options.loggingConstraints,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        logManagerFactory = { client, events ->
            CloudWatchLogManager(
                context = context.applicationContext,
                logGroupName = options.logGroupName,
                localStoreMaxSizeInMB = options.localStoreMaxSizeInMB,
                flushIntervalInSeconds = when (val strategy = options.flushStrategy) {
                    is FlushStrategy.Interval -> strategy.interval.inWholeSeconds
                    FlushStrategy.None -> null
                },
                awsCloudWatchLogsClient = client,
                databaseName = CLIENT_DATABASE_NAME,
                passphrasePreferencesName = CLIENT_PASSPHRASE_PREFERENCES_NAME,
                onWriteLogFailure = { context, cause -> events.tryEmit(LoggingEvent.WriteLogFailure(context, cause)) },
                onFlushLogFailure = { context, cause -> events.tryEmit(LoggingEvent.FlushLogFailure(context, cause)) }
            )
        }
    )

    private val eventsFlow = MutableSharedFlow<LoggingEvent>(
        extraBufferCapacity = EVENTS_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * A stream of [LoggingEvent]s (flush failures, write failures, etc.).
     */
    val events: SharedFlow<LoggingEvent> = eventsFlow.asSharedFlow()

    private val logger: Logger = AmplifyLogging.logger<AmplifyCloudWatchClient>()

    private val filter = CloudWatchLoggingFilter(initialConstraints)

    @Volatile
    private var userIdentifier: String? = null

    @Volatile
    private var isEnabled = true

    private val logManager = logManagerFactory(cloudWatchLogsClient, eventsFlow)

    init {
        scope.launch { logManager.startSync() }
    }

    // region LogSink

    /** Returns true if the client is enabled. */
    override fun isEnabledFor(level: LogLevel): Boolean = isEnabled

    /**
     * Receives a log message, filters it against the current [LoggingConstraints], and forwards it
     * to CloudWatch when it passes. No-op while the client is disabled.
     */
    override fun emit(message: LogMessage) {
        if (!isEnabled) return
        if (!filter.canLog(message.name, message.level, userIdentifier)) return
        val text = "${message.level.name.lowercase()}/${message.name}: ${message.content}" +
            (message.cause?.let { ", error: $it" } ?: "")
        val event = CloudWatchLogEvent(System.currentTimeMillis(), text)
        scope.launch { logManager.saveLogEvent(event) }
    }

    // endregion

    // region Lifecycle

    /** Enable logging and automatic flushing. */
    fun enable() {
        logger.info { "Enabling CloudWatch logging and automatic flushing" }
        isEnabled = true
        scope.launch { logManager.startSync() }
    }

    /** Disable logging and automatic flushing. Messages emitted while disabled are dropped. */
    fun disable() {
        logger.info { "Disabling CloudWatch logging and automatic flushing" }
        isEnabled = false
        logManager.stopSync()
    }

    /** Flush all pending log entries to CloudWatch. */
    suspend fun flushLogs(): FlushResult = try {
        logManager.syncLogEventsWithCloudwatch()
        Result.Success(FlushData())
    } catch (error: Exception) {
        Result.Failure(AmplifyCloudWatchException.from(error))
    }

    /** Returns the underlying AWS CloudWatch Logs SDK client. */
    fun getCloudWatchLogsClient(): CloudWatchLogsClient = cloudWatchLogsClient

    // endregion

    // region User identity

    /**
     * Set the current user identifier. Affects log stream naming and user-specific log
     * level filtering. Pass `null` on sign-out.
     */
    fun setUserIdentifier(identifier: String?) {
        userIdentifier = identifier
        logManager.setUserIdentifier(identifier)
    }

    /**
     * Update the logging constraints. Affects log level filtering for all namespaces.
     */
    fun setLoggingConstraints(constraints: LoggingConstraints) {
        filter.loggingConstraints = constraints
    }

    // endregion
}
