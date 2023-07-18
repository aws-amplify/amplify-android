/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch

import android.content.Context
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.BuildConfig
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.Logger
import com.amplifyframework.logging.LoggingPlugin
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import com.amplifyframework.logging.cloudwatch.worker.CloudwatchRouterWorker
import com.amplifyframework.logging.cloudwatch.worker.CloudwatchWorkerFactory
import java.net.URL
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Logging plugin to send logs to AWS Cloudwatch
*/
class AWSCloudWatchLoggingPlugin @JvmOverloads constructor(
    private val awsCloudWatchLoggingPluginConfig: AWSCloudWatchLoggingPluginConfiguration? = null,
    private val awsRemoteLoggingConstraintProvider: RemoteLoggingConstraintProvider? = null
) : LoggingPlugin<CloudWatchLogsClient>() {

    private val loggingConstraintsResolver =
        LoggingConstraintsResolver(
            localLoggingConstraint = awsCloudWatchLoggingPluginConfig?.loggingConstraints,
            remoteLoggingConstraintProvider = awsRemoteLoggingConstraintProvider
        )
    private val awsCloudWatchLoggingPluginImplementation = AWSCloudWatchLoggingPluginImplementation(
        loggingConstraintsResolver,
        awsCloudWatchLoggingPluginConfig
    )
    private lateinit var cloudWatchLogsClient: CloudWatchLogsClient
    private val logger = Amplify.Logging.logger(CategoryType.LOGGING, this::class.java.simpleName)

    companion object {
        internal const val SHARED_PREFERENCE_FILENAME = "com.amplify.logging.a3fa4188-0ac5-11ee-be56-0242ac120002"
        private const val PLUGIN_NAME = "awsCloudWatchLoggingPlugin"
    }

    @Deprecated("Deprecated in Java")
    override fun forNamespace(namespace: String?): Logger {
        return awsCloudWatchLoggingPluginImplementation.forNamespace(namespace)
    }

    override fun logger(namespace: String): Logger {
        return awsCloudWatchLoggingPluginImplementation.logger(namespace)
    }

    override fun logger(categoryType: CategoryType, namespace: String): Logger {
        return awsCloudWatchLoggingPluginImplementation.logger(categoryType, namespace)
    }

    override fun enable() {
        awsCloudWatchLoggingPluginImplementation.enable()
    }

    override fun disable() {
        awsCloudWatchLoggingPluginImplementation.disable()
    }

    public fun flushLogs(
        onSuccess: Action,
        onError: Consumer<AmplifyException>
    ) {
        awsCloudWatchLoggingPluginImplementation.flushLogs(onSuccess, onError)
    }

    override fun getPluginKey(): String {
        return PLUGIN_NAME
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        try {
            val awsLoggingConfig = awsCloudWatchLoggingPluginConfig ?: getConfigFromFile(pluginConfiguration)
            loggingConstraintsResolver.context = context
            cloudWatchLogsClient = CloudWatchLogsClient {
                credentialsProvider = CognitoCredentialsProvider()
                region = awsLoggingConfig.region
            }
            if (awsRemoteLoggingConstraintProvider == null) {
                awsLoggingConfig.defaultRemoteConfiguration?.let {
                    loggingConstraintsResolver.setRemoteConfigProvider(
                        DefaultRemoteLoggingConstraintProvider(
                            URL(it.endpoint),
                            awsLoggingConfig.region,
                            it.refreshIntervalInSeconds
                        )
                    )
                }
            }
            val cloudWatchLogManager =
                CloudWatchLogManager(context, awsLoggingConfig, cloudWatchLogsClient, loggingConstraintsResolver)
            awsCloudWatchLoggingPluginImplementation.cloudWatchLogManager = cloudWatchLogManager
            CloudwatchRouterWorker.workerFactories[CloudwatchRouterWorker.WORKER_FACTORY_KEY] = CloudwatchWorkerFactory(
                cloudWatchLogManager,
                loggingConstraintsResolver
            )
            awsCloudWatchLoggingPluginImplementation.configure(awsLoggingConfig)
        } catch (exception: AmplifyException) {
            logger.error("failed to configure plugin", exception)
            throw AmplifyException(
                "Failed to configure AWSCloudwatchLoggingPlugin",
                exception,
                "Make sure your configuration is valid."
            )
        }
    }

    override fun getEscapeHatch(): CloudWatchLogsClient {
        return cloudWatchLogsClient
    }

    override fun getVersion(): String = BuildConfig.VERSION_NAME

    @OptIn(ExperimentalSerializationApi::class)
    private fun getConfigFromFile(pluginConfiguration: JSONObject?): AWSCloudWatchLoggingPluginConfiguration {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        return pluginConfiguration?.let {
            json.decodeFromString(
                it.getJSONObject(pluginKey).toString()
            )
        } ?: throw IllegalStateException("Plugin configuration is missing")
    }
}
