/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import android.os.Handler;
import android.os.Looper;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;

/**
 * Closes the WebSocket connection if the time remaining has elapsed.
 * Enables resetting of the watchdog remaining time.
 */
final class TimeoutWatchdog {
    private final Handler handler;

    private Runnable timeoutAction;
    private long timeoutMs;

    TimeoutWatchdog() {
        // TODO: this should be a HandlerThread, don't use MainLooper.
        this.handler = new Handler(Looper.getMainLooper());
        this.timeoutMs = -1;
        this.timeoutAction = null;
    }

    /**
     * Starts a new timer. If a timer is already in progress, it will be stopped,
     * without running to completion. Instead, the new timer will be used in its place.
     * @param timeoutAction An action to perform after a timeout has elapsed
     * @param timeoutMs After this period of time, action is performed
     */
    synchronized void start(final Runnable timeoutAction, long timeoutMs) throws ApiException {
        if (timeoutAction == null) {
            throw new ApiException(
                "Passed null action to watchdog.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        } else if (timeoutMs <= 0) {
            throw new ApiException(
                "timeoutMs must be > 0.",
                "Make sure you didn't set a negative timeout"
            );
        }

        // If there's an existing timer, stop it.
        stop();

        // Now, make a new timer, and save the timeout.
        this.timeoutMs = timeoutMs;
        this.timeoutAction = timeoutAction;
        handler.postDelayed(this.timeoutAction, timeoutMs);
    }

    /**
     * If there is an existing ongoing timer, this will reset it so that
     * the previously set timeoutMs can be counted down again.
     */
    synchronized void reset() {
        if (timeoutAction != null) {
            handler.removeCallbacks(timeoutAction);
            handler.postDelayed(timeoutAction, timeoutMs);
        }
    }

    /**
     * If there is an existing ongoing timer, stop counting it down.
     * The {@see Runnable} provided at {@link #start(Runnable, long)} will not be called.
     */
    synchronized void stop() {
        if (timeoutAction != null) {
            handler.removeCallbacks(timeoutAction);
        }
        timeoutAction = null;
        timeoutMs = -1;
    }
}
