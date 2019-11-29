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

package com.amplifyframework.analytics.pinpoint;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;

/**
 * Submits all the recorded event periodically.
 */
class AutoEventSubmitter {
    private static final String TAG = AutoEventSubmitter.class.getSimpleName();

    private final Handler handler;
    private Runnable submitRunnable;
    private long autoFlushInterval;

    AutoEventSubmitter() {
        this.handler = new Handler(Looper.getMainLooper());
        this.submitRunnable = null;
    }

    void start(final AnalyticsClient analyticsClient, long autoFlushInterval) {
        this.autoFlushInterval = autoFlushInterval;
        submitRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, String.format("Auto submitting events after %d seconds", autoFlushInterval));
                analyticsClient.submitEvents();
            }
        };
        handler.postDelayed(submitRunnable, autoFlushInterval);
    }

    void stop() {
        if (submitRunnable != null) {
            handler.removeCallbacks(submitRunnable);
        }

        submitRunnable = null;
    }

}
