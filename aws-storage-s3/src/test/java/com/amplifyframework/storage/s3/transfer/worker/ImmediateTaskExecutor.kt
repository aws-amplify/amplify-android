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
package com.amplifyframework.storage.s3.transfer.worker

import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import java.util.concurrent.Executor

class ImmediateTaskExecutor : TaskExecutor {
    private val mSynchronousExecutor: Executor = SynchronousExecutor()
    private val mSerialExecutor = SerialExecutor(mSynchronousExecutor)
    override fun postToMainThread(runnable: Runnable?) {
        runnable?.run()
    }

    override fun getMainThreadExecutor(): Executor {
        return mSynchronousExecutor
    }

    override fun executeOnBackgroundThread(runnable: Runnable?) {
        runnable?.let { mSerialExecutor.execute(it) }
    }

    override fun getBackgroundExecutor(): SerialExecutor {
        return mSerialExecutor
    }
}
