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
import android.os.Bundle;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.analytics.SessionClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link AutoSessionTracker}.
 */
@RunWith(RobolectricTestRunner.class)
public final class AutoSessionTrackerTest {
    private AnalyticsClient analyticsClient;
    private SessionClient sessionClient;
    private Application.ActivityLifecycleCallbacks callbacks;

    /**
     * Setup dependencies and object under test.
     */
    @Before
    public void setup() {
        // Dependencies
        this.analyticsClient = mock(AnalyticsClient.class);
        this.sessionClient = mock(SessionClient.class);

        // Object under test
        this.callbacks = new AutoSessionTracker(analyticsClient, sessionClient);
    }

    /**
     * When the app is opened, a start session should be recorded.
     */
    @Test
    public void sessionStartedWhenAppOpened() {
        // Given: the launcher activity instance and bundle class instance.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the app is opened main activity goes through the following lifecycle states.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);

        // Then: Make sure that startSession was invoked on the session client.
        verify(sessionClient).startSession();
    }

    /**
     * When the app is started, user interacts with the app and presses the home button
     * a start session and a stop session should be recorded in that order.
     */
    @Test
    public void sessionStoppedWhenHomeButtonPressed() {
        // Given: the activity that is currently in foreground.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the app goes to background, the activity in foreground is stopped.
        // Activity is put in resume state when the app is started.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);

        // Activity stopped when home button is pressed.
        callbacks.onActivityPaused(activity);
        callbacks.onActivityStopped(activity);

        // Then: Make sure startSession and stopSession are invoked in that order.
        InOrder inOrder = inOrder(sessionClient);
        inOrder.verify(sessionClient).startSession();
        inOrder.verify(sessionClient).stopSession();
    }

    /**
     * When the app is started and later killed, a start session and a stop session should be recorded
     * in that order.
     */
    @Test
    public void sessionStopWhenAppKilled() {
        // Given: the activity that is currently in foreground.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the app is killed, the current activity is destroyed.
        // Activity is put in resume state when the app is started.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);

        // Activity stopped and destroyed when home button is pressed.
        callbacks.onActivityPaused(activity);
        callbacks.onActivityStopped(activity);
        callbacks.onActivityDestroyed(activity);

        // Then: Make sure startSession and stopSession are invoked in that order.
        InOrder inOrder = inOrder(sessionClient);
        inOrder.verify(sessionClient).startSession();
        inOrder.verify(sessionClient).stopSession();
    }

    /**
     * When the app is temporarily interrupted by events such as phone call or a  pop-up,
     * same session should be continued, i.e stop session should not be recorded.
     */
    @Test
    public void sessionNotStoppedWhenAppInterrupted() {
        // Given: the activity that is currently in foreground.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the app is interrupted by an event such as phone call, pop-up or app losing focus in
        // Multi-window mode, running activity is paused.
        // Activity is put in resume state when the app is started.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);

        // Activity paused upon interruption.
        callbacks.onActivityPaused(activity);

        // Then: Make sure stopSession is not invoked.
        verify(sessionClient).startSession();
        verify(sessionClient, never()).stopSession();
    }

    /**
     * When the app transitions from activity to another, session should not be interrupted i.e
     * stop session should not be recorded.
     */
    @Test
    public void sessionNotStoppedOnActivityTransition() {
        // Given: two activities of the application.
        Activity activity1 = mock(Activity.class);
        Activity activity2 = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the activity is paused because of an interruptive event such as
        // Activity is started
        callbacks.onActivityCreated(activity1, bundle);
        callbacks.onActivityStarted(activity1);
        callbacks.onActivityResumed(activity1);

        // Second activity is started causing first activity to be paused.
        callbacks.onActivityPaused(activity1);

        // Second activity is resumed
        callbacks.onActivityCreated(activity2, bundle);
        callbacks.onActivityStarted(activity2);
        callbacks.onActivityResumed(activity2);

        // Previous activity is stopped only after the new activity is in foreground.
        callbacks.onActivityStopped(activity1);

        // Then: Make sure that the session is not interrupted by the activity transition.
        verify(sessionClient).startSession();
        verify(sessionClient, never()).stopSession();
    }
}
