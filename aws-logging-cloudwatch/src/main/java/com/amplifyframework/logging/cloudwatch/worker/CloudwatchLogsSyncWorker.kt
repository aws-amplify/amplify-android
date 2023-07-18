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
package com.amplifyframework.logging.cloudwatch.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amplifyframework.logging.cloudwatch.CloudWatchLogManager

internal class CloudwatchLogsSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val cloudWatchLogManager: CloudWatchLogManager
) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val WORKER_NAME_TAG = "CloudwatchLogsSyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            cloudWatchLogManager.syncLogEventsWithCloudwatch()
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        } finally {
            cloudWatchLogManager.enqueueSync()
        }
    }
}
