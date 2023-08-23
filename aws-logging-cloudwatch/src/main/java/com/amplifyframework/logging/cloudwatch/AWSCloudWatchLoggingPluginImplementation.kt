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

import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.logging.Logger
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AWSCloudWatchLoggingPluginImplementation(
    private val loggingConstraintsResolver: LoggingConstraintsResolver,
    private var awsCloudWatchLoggingPluginConfig: AWSCloudWatchLoggingPluginConfiguration? = null,
    internal var cloudWatchLogManager: CloudWatchLogManager? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val defaultNameSpace = "amplify"
    private val coroutineScope = CoroutineScope(dispatcher)
    internal var isPluginEnabled = awsCloudWatchLoggingPluginConfig?.enable ?: true

    internal fun configure(
        awsPluginConfig: AWSCloudWatchLoggingPluginConfiguration
    ) {
        awsCloudWatchLoggingPluginConfig = awsPluginConfig
        loggingConstraintsResolver.localLoggingConstraint = awsPluginConfig.loggingConstraints
        isPluginEnabled = awsPluginConfig.enable
        if (isPluginEnabled) {
            coroutineScope.launch {
                cloudWatchLogManager?.startSync()
            }
        }
        Amplify.Hub.subscribe(HubChannel.AUTH) { event ->
            if (event.name == AuthChannelEventName.SIGNED_IN.name) {
                cloudWatchLogManager?.onSignIn()
            }
            if (event.name == AuthChannelEventName.SIGNED_OUT.name) {
                cloudWatchLogManager?.onSignOut()
            }
        }
    }

    internal fun forNamespace(namespace: String?): Logger {
        val resolvedNameSpace = namespace ?: defaultNameSpace
        return logger(resolvedNameSpace)
    }

    internal fun logger(namespace: String): Logger {
        return CloudWatchLogger(
            namespace,
            null,
            loggingConstraintsResolver,
            this
        )
    }

    internal fun logger(categoryType: CategoryType, namespace: String): Logger {
        return CloudWatchLogger(
            namespace,
            categoryType,
            loggingConstraintsResolver,
            this
        )
    }

    fun enable() {
        isPluginEnabled = true
        coroutineScope.launch {
            cloudWatchLogManager?.startSync()
        }
    }

    fun disable() {
        isPluginEnabled = false
        coroutineScope.launch {
            cloudWatchLogManager?.stopSync()
        }
    }

    internal fun flushLogs(
        onSuccess: Action,
        onError: Consumer<AmplifyException>
    ) {
        coroutineScope.launch {
            try {
                cloudWatchLogManager?.syncLogEventsWithCloudwatch()
                onSuccess.call()
            } catch (exception: Exception) {
                onError.accept(AmplifyException("Failed to flush logs", exception.cause, "Please try again"))
            }
        }
    }
}
