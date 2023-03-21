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

package com.amplifyframework.pinpoint.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.annotations.InternalAmplifyApi;

/**
 * Tracks when the host application enters or leaves foreground.
 * The constructor registers to receive activity lifecycle events.
 **/
@InternalAmplifyApi
public final class AutoSessionTracker implements Application.ActivityLifecycleCallbacks {
    private static final String LOG_TAG = AutoSessionTracker.class.getSimpleName();
    private final SessionClient sessionClient;
    private final AnalyticsClient analyticsClient;
    private boolean inForeground;
    private int foregroundActivityCount;

    /**
     * Constructor. Registers to receive activity lifecycle events.
     * @param analyticsClient Amazon pinpoint analytics client
     * @param sessionClient Amazon pinpoint session client
     */
    AutoSessionTracker(@NonNull final AnalyticsClient analyticsClient, @NonNull final SessionClient sessionClient) {
        this.analyticsClient = analyticsClient;
        this.sessionClient = sessionClient;
        inForeground = false;
        foregroundActivityCount = 0;
    }

    void startSessionTracking(final Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    void stopSessionTracking(final Application application) {
        application.unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(final Activity activity, final Bundle bundle) {
        Log.d(LOG_TAG, "Activity created: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStarted(final Activity activity) {
        Log.d(LOG_TAG, "Activity started: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        // An activity came to foreground. Application potentially entered foreground as well
        // if there were no other activities in the foreground.
        Log.d(LOG_TAG, "Activity resumed: " + activity.getLocalClassName());
        checkIfApplicationEnteredForeground();
        foregroundActivityCount++;
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        // onPause is always followed by onStop except when the app is interrupted by an event such
        // as a phone call, pop-ups or app losing focus in a multi-window mode, in which case activity is
        // resumed if app regains focus.In either case, app foreground status does not change for the
        // purpose of session tracking.
        Log.d(LOG_TAG, "Activity paused: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStopped(final Activity activity) {
        // An activity entered stopped state. Application potentially entered background if there are
        // no other activities in non-stopped states, in which case app is not visible to user and has
        // entered background state.
        Log.d(LOG_TAG, "Activity stopped: " + activity.getLocalClassName());
        foregroundActivityCount--;
        checkIfApplicationEnteredBackground();
    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
        Log.d(LOG_TAG, "Activity state saved: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
        // onStop is always called before onDestroy so no action is required in onActivityDestroyed.
        Log.d(LOG_TAG, "Activity destroyed " + activity.getLocalClassName());
    }

    /**
     * Called when the application enters the foreground.
     */
    void applicationEnteredForeground() {
        Log.d(LOG_TAG, "Application entered the foreground.");
        sessionClient.startSession();
    }

    /**
     * Called when the application enters the background.
     */
    void applicationEnteredBackground() {
        Log.d(LOG_TAG, "Application entered the background.");
        sessionClient.stopSession();
        analyticsClient.flushEvents();
    }

    /**
     * Called from onActivityResumed to check if the application came to the foreground.
     */
    private void checkIfApplicationEnteredForeground() {
        // if nothing is in the activity lifecycle map indicating that we are likely in the background, and the flag
        // indicates we are indeed in the background.
        if (foregroundActivityCount == 0 && !inForeground) {
            inForeground = true;
            // Since this is called when an activity has started, we now know the app has entered the foreground.
            applicationEnteredForeground();
        }
    }

    /**
     * Called from onActivityStopped to check if the application receded to the background.
     */
    private void checkIfApplicationEnteredBackground() {
        // If the App is in the foreground and there are no longer any activities that have not been stopped.
        if (foregroundActivityCount == 0 && inForeground) {
            inForeground = false;
            applicationEnteredBackground();
        }
    }
}
