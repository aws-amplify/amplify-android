package com.amplifyframework.analytics.pinpoint;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;

/**
 * Submits all the recorded event periodically
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
                Log.d(TAG, String.format("Auto submitting events after %l seconds", autoFlushInterval));
                analyticsClient.submitEvents();
            }
        };
    }

    void stop() {
        if (submitRunnable != null) {
            handler.removeCallbacks(submitRunnable);
        }

        submitRunnable = null;
    }

}
