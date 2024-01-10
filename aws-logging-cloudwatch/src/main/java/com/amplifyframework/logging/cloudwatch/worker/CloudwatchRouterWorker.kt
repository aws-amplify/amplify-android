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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import java.lang.IllegalStateException

internal class CloudwatchRouterWorker(appContext: Context, private val parameter: WorkerParameters) :
    ListenableWorker(appContext, parameter) {

    private val workerClassName =
        parameter.inputData.getString(WORKER_CLASS_NAME)
            ?: throw IllegalArgumentException("Worker class name is missing")
    private var delegateWorker: CoroutineWorker? = null

    companion object {
        internal const val WORKER_CLASS_NAME = "WORKER_CLASS_NAME"
        internal const val WORKER_ID = "WORKER_ID"
        internal const val WORKER_FACTORY_KEY = "AWSCloudwatchFactory"
        private var isWorkerFactoriesInitialized: Boolean = false
        val workerFactories = object : AbstractMutableMap<String, CloudwatchWorkerFactory>() {

            private val backingWorkerMap = mutableMapOf<String, CloudwatchWorkerFactory>()

            override fun put(key: String, value: CloudwatchWorkerFactory): CloudwatchWorkerFactory? {
                isWorkerFactoriesInitialized = true
                return backingWorkerMap.put(key, value)
            }

            override val entries: MutableSet<MutableMap.MutableEntry<String, CloudwatchWorkerFactory>>
                get() = backingWorkerMap.entries
        }
    }

    override fun startWork(): ListenableFuture<Result> {
        val delegateWorkerFactory: CloudwatchWorkerFactory? = workerFactories[WORKER_FACTORY_KEY]
        delegateWorker = delegateWorkerFactory?.createWorker(
            applicationContext,
            workerClassName,
            parameter
        )
        delegateWorker?.let {
            return it.startWork()
        } ?: run {
            // this is to prevent a race condition where workManager starts work before worker factory is initialized
            if (!isWorkerFactoriesInitialized) {
                return CallbackToFutureAdapter.getFuture {
                    Result.retry()
                }
            } else {
                throw IllegalStateException("Failed to find delegate for $workerClassName")
            }
        }
    }

    override fun onStopped() {
        super.onStopped()
        delegateWorker?.onStopped()
    }
}
