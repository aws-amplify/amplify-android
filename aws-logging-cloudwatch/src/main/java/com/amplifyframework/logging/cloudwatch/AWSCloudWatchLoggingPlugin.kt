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
import android.util.Log
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.Resources
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.Logger
import com.amplifyframework.logging.LoggingPlugin
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import java.net.URL
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Logging plugin to send logs to AWS Cloudwatch
*/
class AWSCloudWatchLoggingPlugin @JvmOverloads constructor(
    private val awsCloudWatchLoggingPluginConfig: AWSCloudWatchLoggingPluginConfiguration? = null,
    private val awsRemoteLoggingConstraintProvider: RemoteLoggingConstraintProvider? = null,
) : LoggingPlugin<CloudWatchLogsClient>() {

    private val loggingConstraintsResolver =
        LoggingConstraintsResolver(
            localLoggingConstraint = awsCloudWatchLoggingPluginConfig?.loggingConstraints,
            remoteLoggingConstraintProvider = awsRemoteLoggingConstraintProvider,
        )
    private val awsCloudWatchLoggingPluginBehavior = AWSCloudWatchLoggingPluginBehavior(
        loggingConstraintsResolver,
        awsCloudWatchLoggingPluginConfig,
    )
    private lateinit var cloudWatchLogsClient: CloudWatchLogsClient

    companion object {
        private const val CONFIG_FILENAME = "amplifyconfiguration_logging"
        internal const val SHARED_PREFERENCE_FILENAME = "com.amplify.logging.a3fa4188-0ac5-11ee-be56-0242ac120002"
    }

    @Deprecated("Deprecated in Java")
    override fun forNamespace(namespace: String?): Logger {
        return awsCloudWatchLoggingPluginBehavior.forNamespace(namespace)
    }

    override fun logger(namespace: String): Logger {
        return awsCloudWatchLoggingPluginBehavior.logger(namespace)
    }

    override fun logger(categoryType: CategoryType, namespace: String): Logger {
        return awsCloudWatchLoggingPluginBehavior.logger(categoryType, namespace)
    }

    override fun enable() {
        awsCloudWatchLoggingPluginBehavior.enable()
    }

    override fun disable() {
        awsCloudWatchLoggingPluginBehavior.disable()
    }

    public fun flushLogs(
        onSuccess: Action,
        onError: Consumer<AmplifyException>,
    ) {
        awsCloudWatchLoggingPluginBehavior.flushLogs(onSuccess, onError)
    }

    override fun getPluginKey(): String {
        return "awsCloudWatchLoggingPlugin"
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        try {
            // TODO: move reading from file to core
            val awsLoggingConfig = awsCloudWatchLoggingPluginConfig ?: getConfigFromFile(context)
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
                            it.refreshIntervalInSeconds,
                        ),
                    )
                }
            }
            val cloudWatchLogManager =
                CloudWatchLogManager(context, awsLoggingConfig, cloudWatchLogsClient, loggingConstraintsResolver)
            awsCloudWatchLoggingPluginBehavior.cloudWatchLogManager = cloudWatchLogManager
            awsCloudWatchLoggingPluginBehavior.configure(awsLoggingConfig)
        } catch (exception: Exception) {
            Log.e("AWSCloudWatchLoggingPlugin", "failed to configure plugin", exception)
        }
    }

    override fun getEscapeHatch(): CloudWatchLogsClient {
        return cloudWatchLogsClient
    }

    override fun getVersion(): String = BuildConfig.VERSION_NAME

    private fun getConfigFromFile(context: Context): AWSCloudWatchLoggingPluginConfiguration {
        val resourceId = Resources.getRawResourceId(context, CONFIG_FILENAME)
        val configJson = Resources.readJsonResourceFromId(context, resourceId)
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        return json.decodeFromString(
            configJson.getJSONObject(pluginKey).toString(),
        )
    }
}
