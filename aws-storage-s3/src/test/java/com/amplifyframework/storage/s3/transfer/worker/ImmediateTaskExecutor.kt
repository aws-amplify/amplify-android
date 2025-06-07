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

import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import java.util.concurrent.Executor

class ImmediateTaskExecutor : TaskExecutor {
    private val mSynchronousExecutor: Executor = SynchronousExecutor()
    private val mSerialExecutor = SerialExecutorImpl(mSynchronousExecutor)

    override fun getMainThreadExecutor(): Executor = mSynchronousExecutor

    override fun getSerialTaskExecutor(): SerialExecutor = mSerialExecutor

    private class SerialExecutorImpl(val delegate: Executor) : SerialExecutor, Executor by delegate {
        // There's never any pending tasks since we execute all immediately
        override fun hasPendingTasks() = false
    }
}
