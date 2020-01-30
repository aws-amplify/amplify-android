/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.async;

import android.os.Process;
import androidx.annotation.NonNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * ExecutorServices for Amplify.
 */
public final class AmplifyExecutors {
    private static final String THREAD_GROUP_NAME = "Amplify";
    private static final ThreadGroup THREAD_GROUP = threadGroup();
    private static final ExecutorService STANDARD = standardExecutorService();
    private static final ScheduledExecutorService PERIODIC = scheduledExecutorService();

    @SuppressWarnings("checkstyle:all") private AmplifyExecutors() {}

    /**
     * Gets a fixed thread pool {@link ExecutorService} with as many threads
     * as there are processors on the host.
     * @return A fixed size thread pool {@link ExecutorService}
     */
    @NonNull
    public static ExecutorService standard() {
        return STANDARD;
    }

    /**
     * Gets a single-threaded {@link ScheduledExecutorService}.
     * @return Single-threaded {@link ScheduledExecutorService}
     */
    @NonNull
    public static ScheduledExecutorService periodic() {
        return PERIODIC;
    }

    private static String threadName() {
        return THREAD_GROUP_NAME + UUID.randomUUID().toString();
    }

    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread thread = new Thread(THREAD_GROUP, runnable, threadName());
            // "The Java Virtual Machine exits when the only threads running are all daemon threads."
            thread.setDaemon(true);
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            // TODO: publish uncaught exception to Hub?
            return thread;
        };
    }

    private static ThreadGroup threadGroup() {
        return new ThreadGroup(THREAD_GROUP_NAME);
    }

    private static ExecutorService standardExecutorService() {
        return Executors.newCachedThreadPool(threadFactory());
    }

    private static ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(threadFactory());
    }
}
