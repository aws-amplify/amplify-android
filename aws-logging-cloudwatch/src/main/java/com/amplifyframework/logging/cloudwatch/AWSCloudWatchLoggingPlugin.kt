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
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.Logger
import com.amplifyframework.logging.LoggingPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AWSCloudWatchLoggingPlugin @JvmOverloads constructor(
    private val awsCloudWatchLoggingPluginConfig: AWSCloudWatchLoggingPluginConfig? = null,
) : LoggingPlugin<Void>() {

    private val defaultNamespace = "amplify"
    private var cloudwatchLogEventRecorder: CloudwatchLogEventRecorder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var logToLogcat = false
    private val loggingConstraintsResolver = LoggingConstraintsResolver()

    override fun forNamespace(namespace: String?): Logger {
        Log.i("CloudWatchLoggingPlugin", "forNamespace $namespace")
        val resolvedNameSpace = namespace ?: defaultNamespace
        return CloudWatchLogger(
            resolvedNameSpace,
            null,
            logToLogcat,
            cloudwatchLogEventRecorder,
            loggingConstraintsResolver,
        )
    }

    override fun logger(namespace: String): Logger {
        TODO("Not yet implemented")
    }

    override fun logger(categoryType: CategoryType, namespace: String): Logger {
        TODO("Not yet implemented")
    }

    override fun enable() {
        TODO("Not yet implemented")
    }

    override fun disable() {
        TODO("Not yet implemented")
    }

    public fun flushLogs(
        onSuccess: Consumer<Void>,
        onError: Consumer<AmplifyException>,
    ) {
    }

    override fun getPluginKey(): String {
        return "AWSCloudWatchLoggingPlugin"
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        Log.i("CloudWatchLoggingPlugin", "onConfiguration $pluginConfiguration")
        System.loadLibrary("sqlcipher")
        cloudwatchLogEventRecorder = CloudwatchLogEventRecorder(context)
        val cloudWatchLogsClient = CloudWatchLogsClient {
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "ID"
                secretAccessKey = "ACCESS_KEY"
            }
            region = "us-east-1"
        }
        cloudwatchLogEventRecorder?.awsCloudWatchLogsClient = cloudWatchLogsClient
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.startSync()
        }

        loggingConstraintsResolver.localLoggingConstraint = awsCloudWatchLoggingPluginConfig?.localLoggingConstraint
        logToLogcat = awsCloudWatchLoggingPluginConfig?.logToConsole == true
    }

    override fun getEscapeHatch(): Void? {
        TODO("Not yet implemented")
    }

    override fun getVersion(): String {
        TODO("Not yet implemented")
    }
}
