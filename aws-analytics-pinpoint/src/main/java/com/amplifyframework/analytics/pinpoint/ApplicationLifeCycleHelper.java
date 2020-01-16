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

package com.amplifyframework.analytics.pinpoint;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;

import java.util.WeakHashMap;

/**
 * Aids in determining when your application has entered or left the foreground.
 * The constructor registers to receive Activity lifecycle events and also registers a
 * broadcast receiver to handle the screen being turned off.  Abstract methods are
 * provided to handle when the application enters the background or foreground.
 * Any activity lifecycle callbacks can easily be overriden if additional handling
 * is needed. Just be sure to call through to the super method so that this class
 * will still behave as intended.
 **/
public abstract class ApplicationLifeCycleHelper implements Application.ActivityLifecycleCallbacks {
    private static final String LOG_TAG = ApplicationLifeCycleHelper.class.getSimpleName();
    private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    private boolean inForeground = false;
    /** Tracks the lifecycle of activities that have not stopped (including those restarted). */
    private WeakHashMap<Activity, String> activityLifecycleStateMap = new WeakHashMap<>();

    /**
     * Constructor. Registers to receive activity lifecycle events.
     * @param application The Android Application class.
     */
    public ApplicationLifeCycleHelper(final Application application) {
        application.registerActivityLifecycleCallbacks(this);
        final ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();
        application.registerReceiver(screenOffReceiver, new IntentFilter(ACTION_SCREEN_OFF));
    }

    @Override
    public void onActivityCreated(final Activity activity, final Bundle bundle) {
        Log.d(LOG_TAG, "onActivityCreated " + activity.getLocalClassName());
        handleOnCreateOrOnStartToHandleApplicationEnteredForeground();
        activityLifecycleStateMap.put(activity, "created");
    }

    @Override
    public void onActivityStarted(final Activity activity) {
        Log.d(LOG_TAG, "onActivityStarted " + activity.getLocalClassName());
        handleOnCreateOrOnStartToHandleApplicationEnteredForeground();
        activityLifecycleStateMap.put(activity, "started");
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        Log.d(LOG_TAG, "onActivityResumed " + activity.getLocalClassName());
        activityLifecycleStateMap.put(activity, "resumed");
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        Log.d(LOG_TAG, "onActivityPaused " + activity.getLocalClassName());
        activityLifecycleStateMap.put(activity, "paused");
    }

    @Override
    public void onActivityStopped(final Activity activity) {
        Log.d(LOG_TAG, "onActivityStopped " + activity.getLocalClassName());
        // When the activity is stopped, we remove it from the lifecycle state map since we
        // no longer consider it keeping a session alive.
        activityLifecycleStateMap.remove(activity);
    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
        Log.d(LOG_TAG, "onActivitySaveInstanceState " + activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
        Log.d(LOG_TAG, "onActivityDestroyed " + activity.getLocalClassName());
        // Activity should not be in the activityLifecycleStateMap any longer.
        if (activityLifecycleStateMap.containsKey(activity)) {
            Log.wtf(LOG_TAG, "Destroyed activity present in activityLifecycleMap!?");
            activityLifecycleStateMap.remove(activity);
        }
    }

    /**
     * Call this method when your Application trims memory.
     * @param level the level passed through from Application.onTrimMemory().
     */
    public void handleOnTrimMemory(final int level) {
        Log.d(LOG_TAG, "onTrimMemory " + level);
        // If no activities are running and the app has gone into the background.
        if (level >= Application.TRIM_MEMORY_UI_HIDDEN) {
            checkForApplicationEnteredBackground();
        }
    }

    class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkForApplicationEnteredBackground();
        }
    }

    /**
     * Called back when your application enters the Foreground.
     */
    protected abstract void applicationEnteredForeground();

    /**
     * Called back when your application enters the Background.
     */
    protected abstract void applicationEnteredBackground();

    /**
     * Called from onActivityCreated and onActivityStarted to handle when the application enters
     * the foreground.
     */
    private void handleOnCreateOrOnStartToHandleApplicationEnteredForeground() {
        // if nothing is in the activity lifecycle map indicating that we are likely in the background, and the flag
        // indicates we are indeed in the background.
        if (activityLifecycleStateMap.size() == 0 && !inForeground) {
            inForeground = true;
            // Since this is called when an activity has started, we now know the app has entered the foreground.
            applicationEnteredForeground();
        }
    }

    private void checkForApplicationEnteredBackground() {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // If the App is in the foreground and there are no longer any activities that have not been stopped.
                if ((activityLifecycleStateMap.size() == 0) && inForeground) {
                    inForeground = false;
                    applicationEnteredBackground();
                }
            }
        });
    }
}
