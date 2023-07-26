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
package com.amplifyframework.logging.cloudwatch.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.amplifyframework.logging.cloudwatch.CloudWatchLogManager
import com.amplifyframework.logging.cloudwatch.LoggingConstraintsResolver

internal class CloudwatchWorkerFactory(
    private val cloudWatchLogManager: CloudWatchLogManager,
    private val loggingConstraintsResolver: LoggingConstraintsResolver
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        return when (workerClassName) {
            CloudwatchLogsSyncWorker::class.java.simpleName -> {
                CloudwatchLogsSyncWorker(
                    appContext,
                    workerParameters,
                    cloudWatchLogManager
                )
            }
            RemoteConfigSyncWorker::class.java.simpleName -> {
                RemoteConfigSyncWorker(
                    appContext,
                    workerParameters,
                    loggingConstraintsResolver
                )
            }
            else -> throw IllegalStateException("Failed to find matching Worker for $workerClassName")
        }
    }
}
