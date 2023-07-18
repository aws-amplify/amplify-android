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

package com.amplifyframework.pinpoint.core;

import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;

import com.amplifyframework.annotations.InternalAmplifyApi;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;

import java.util.Locale;

/**
 * Submits all the recorded event periodically.
 */
@InternalAmplifyApi
public final class AutoEventSubmitter {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.ANALYTICS, "amplify:aws-analytics");

    private final Handler handler;
    private Runnable submitRunnable;
    private final long autoFlushInterval;

    AutoEventSubmitter(@NonNull final AnalyticsClient analyticsClient, final long autoFlushInterval) {
        HandlerThread handlerThread = new HandlerThread("AutoEventSubmitter");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
        this.autoFlushInterval = autoFlushInterval;
        this.submitRunnable = () -> {
            LOG.debug(String.format(Locale.US, "Auto submitting events after %d seconds", autoFlushInterval));
            analyticsClient.flushEvents();
            handler.postDelayed(this.submitRunnable, autoFlushInterval);
        };
    }

    synchronized void start() {
        handler.postDelayed(submitRunnable, autoFlushInterval);
    }

    synchronized void stop() {
        handler.removeCallbacksAndMessages(null);
    }
}
