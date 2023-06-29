/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.cloudwatch.models.LoggingConstraints
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class LoggingConstraintsResolver internal constructor(
    internal var context: Context? = null,
    internal var localLoggingConstraint: LoggingConstraints? = null,
    private var remoteLoggingConstraintProvider: RemoteLoggingConstraintProvider? = null,
    internal var userId: String? = null,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val coroutine = CoroutineScope(coroutineDispatcher)
    private var remoteLoggingConstraint: LoggingConstraints? = null
    private val logger = Amplify.Logging.logger(CategoryType.LOGGING, this::class.java.simpleName)

    companion object {
        internal const val REMOTE_LOGGING_CONSTRAINTS_KEY = "remoteLoggingConstraints"
    }

    init {
        loadRemoteConfig()
    }

    fun resolveLogLevel(namespace: String, categoryType: CategoryType?): LogLevel {
        return remoteLoggingConstraint?.let { loggingConstraint -> // look first in remote config
            loggingConstraint.userLogLevel[userId]?.let { userLogLevel ->
                userLogLevel.categoryLogLevel[categoryType] ?: userLogLevel.defaultLogLevel
            } ?: loggingConstraint.categoryLogLevel[categoryType] ?: loggingConstraint.defaultLogLevel
        } ?: localLoggingConstraint?.let { loggingConstraint -> // then in local config
            loggingConstraint.userLogLevel[userId]?.let { userLogLevel ->
                userLogLevel.categoryLogLevel[categoryType] ?: userLogLevel.defaultLogLevel
            } ?: loggingConstraint.categoryLogLevel[categoryType] ?: loggingConstraint.defaultLogLevel
        } ?: LogLevel.ERROR // set to default if both local & remote config is missing
    }

    private fun loadRemoteConfig() {
        remoteLoggingConstraintProvider?.let { remoteProvider ->
            coroutine.launch {
                while (true) {
                    remoteProvider.fetchLoggingConfig({
                        remoteLoggingConstraint = it
                        saveRemoteConstraintsToSharedPreference(it)
                    }, {
                        logger.error("failed to load remote config, error: ${Log.getStackTraceString(it)}")
                        remoteLoggingConstraint = getRemoteConstraintsFromSharedPreference()
                    })
                    delay(remoteProvider.getConstraintsSyncInterval() * 1000L)
                }
            }
        }
    }

    internal fun setRemoteConfigProvider(
        defaultRemoteLoggingConstraintProvider: DefaultRemoteLoggingConstraintProvider,
    ) {
        remoteLoggingConstraintProvider = defaultRemoteLoggingConstraintProvider
        loadRemoteConfig()
    }

    private fun saveRemoteConstraintsToSharedPreference(loggingConstraint: LoggingConstraints) {
        context?.let {
            val sharedPreferences =
                it.getSharedPreferences(AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putString(REMOTE_LOGGING_CONSTRAINTS_KEY, LoggingConstraints.toJsonString(loggingConstraint)).apply()
        }
    }

    private fun getRemoteConstraintsFromSharedPreference(): LoggingConstraints? {
        return context?.let {
            val remoteConstraints = it.getSharedPreferences(
                AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE,
            )
                .getString(REMOTE_LOGGING_CONSTRAINTS_KEY, null)
            remoteConstraints?.let {
                LoggingConstraints.fromString(remoteConstraints)
            }
        }
    }
}
