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
package com.amplifyframework.storage.s3.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.function.Consumer

object WorkmanagerTestUtils {

    fun initializeWorkmanagerTestUtil(context: Context) {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            WorkManager.getInstance(context).getWorkInfosByTagLiveData("awsS3StoragePlugin")
                .observeForever { observer: List<WorkInfo> ->
                    val driver = WorkManagerTestInitHelper.getTestDriver(context)
                    observer.forEach(
                        Consumer { action: WorkInfo ->
                            driver?.setAllConstraintsMet(
                                action.id
                            )
                        }
                    )
                }
        }
    }
}
