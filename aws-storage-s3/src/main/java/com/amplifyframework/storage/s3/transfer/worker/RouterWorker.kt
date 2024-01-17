/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3.transfer.worker

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.google.common.util.concurrent.ListenableFuture
import java.lang.IllegalStateException

/**
 * Worker to route transfer WorkRequest to appropriate WorkerFactory
 */
internal class RouterWorker(
    appContext: Context,
    private val parameter: WorkerParameters
) : ListenableWorker(appContext, parameter) {

    private val logger =
        Amplify.Logging.logger(
            CategoryType.STORAGE,
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )
    private val workerClassName =
        parameter.inputData.getString(WORKER_CLASS_NAME)
            ?: throw IllegalArgumentException("Worker class name is missing")
    private val workerId = parameter.inputData.getString(BaseTransferWorker.WORKER_ID)

    private var delegateWorker: BaseTransferWorker? = null

    companion object {
        internal const val WORKER_CLASS_NAME = "WORKER_CLASS_NAME"
        private var isWorkerFactoriesInitialized: Boolean = false
        val workerFactories = object : AbstractMutableMap<String, TransferWorkerFactory>() {

            private val backingWorkerMap = mutableMapOf<String, TransferWorkerFactory>()

            override fun put(key: String, value: TransferWorkerFactory): TransferWorkerFactory? {
                isWorkerFactoriesInitialized = true
                return backingWorkerMap.put(key, value)
            }

            override val entries: MutableSet<MutableMap.MutableEntry<String, TransferWorkerFactory>> get() =
                backingWorkerMap.entries
        }
    }

    override fun startWork(): ListenableFuture<Result> {
        val delegateWorkerFactory: TransferWorkerFactory? = workerFactories[workerId]
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
                logger.error("DelegateWorker not initialized, initialize WorkerFactory")
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
        logger.debug("onStopped for $id")
        delegateWorker?.onStopped()
    }
}
