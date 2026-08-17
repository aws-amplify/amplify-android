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
package com.amplifyframework.cloudwatch.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amplifyframework.cloudwatch.CloudWatchLogManager

internal class CloudWatchLogsSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val cloudWatchLogManager: CloudWatchLogManager
) : CoroutineWorker(context, workerParameters) {

    companion object {
        // Distinct from the v2 plugin's unique-work name so the two never clobber each other's
        // scheduled auto-flush when both run in the same app (WorkManager unique names are app-global).
        internal const val WORKER_NAME_TAG = "AmplifyCloudWatchClientLogsSyncWorker"
    }

    override suspend fun doWork(): Result = try {
        cloudWatchLogManager.syncLogEventsWithCloudwatch()
        Result.success()
    } catch (exception: Exception) {
        Result.retry()
    } finally {
        cloudWatchLogManager.enqueueSync()
    }
}
