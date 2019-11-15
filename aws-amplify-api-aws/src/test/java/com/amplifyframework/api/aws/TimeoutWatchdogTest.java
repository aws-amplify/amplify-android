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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests the {@link TimeoutWatchdog}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TimeoutWatchdogTest {
    private static final int DEFAULT_TIMEOUT_MS = 100;

    private TimeoutWatchdog watchdog;
    private Runnable timeoutAction;

    /**
     * Setup dependencies and the object under test.
     */
    @Before
    public void setup() {
        timeoutAction = mock(Runnable.class);
        watchdog = new TimeoutWatchdog();
    }

    /**
     * When the timer is started, and more time elapses than the timer
     * allowed, then the timeout action should be run.
     */
    @Test
    public void timeoutActionIsInvokedAfterTimeElapsesFromStart() {
        // When watchdog is started,
        watchdog.start(timeoutAction, DEFAULT_TIMEOUT_MS);

        // Act: the timeout elapses,
        ShadowLooper.idleMainLooper(1 + DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // The timeout action fires.
        verify(timeoutAction).run();
    }

    /**
     * If the watchdog is never started, then the timeout action is never run.
     */
    @Test
    public void timeoutActionNotRunWhenWatchdogNotStarted() {
        // Arrange: watchdog not started
        // watchdog.start(timeoutAction, DEFAULT_TIMEOUT_MS);

        // Act: Time elapses
        ShadowLooper.idleMainLooper(1 + DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Timeout action is not invoked
        verifyNoInteractions(timeoutAction);
    }

    /**
     * If the watchdog gets reset, and the new timeout still has not elapsed,
     * then the timeout action is not run (yet).
     */
    @Test
    public void timeoutActionNotRunAfterResetBeforeNewTimeout() {
        // Arrange: timer is started and almost counted down.
        watchdog.start(timeoutAction, DEFAULT_TIMEOUT_MS);
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_MS - 1, TimeUnit.MILLISECONDS);

        // Act: Timer is reset, and time advances.
        watchdog.reset();
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_MS - 1, TimeUnit.MILLISECONDS);

        // Assert: the timeout action still wasn't run, even though 198ms have elapsed.
        verifyNoInteractions(timeoutAction);
    }

    /**
     * If you start the watchdog, and then reset it, but time elapses beyond
     * even the newly set timeout, then the timeout action will run.
     */
    @Test
    public void timeoutActionIsRunEvenAfterResetTimePeriod() {
        // Arrange: started watchdog, time has gone by.
        watchdog.start(timeoutAction, DEFAULT_TIMEOUT_MS);
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_MS - 1, TimeUnit.MILLISECONDS);

        // Act: reset, and then more than the new time goes by
        watchdog.reset();
        ShadowLooper.idleMainLooper(1 + DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert: timeout action is run
        verify(timeoutAction).run();
    }

    /**
     * If you stop the watchdog before it times out, then the timeout action
     * doesn't get run.
     */
    @Test
    public void timeoutActionIsRunIfWatchdogStoppedBeforeTimeout() {
        // Arrange: timer is started, and almost out of time
        watchdog.start(timeoutAction, DEFAULT_TIMEOUT_MS);
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_MS - 1, TimeUnit.MILLISECONDS);

        // Act: we stop it, and then much more time (past original quota) elapses
        watchdog.stop();
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert: timeout action has not been run.
        verifyNoInteractions(timeoutAction);
    }

    /**
     * Calling reset() on a not-started watchdog doesn't do much.
     * The timeout action won't be run.
     */
    @Test
    public void resetOnStoppedWatchdogDoesNothing() {
        watchdog.reset();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verifyNoInteractions(timeoutAction);
    }

    /**
     * Calling stop() on a not-started watchdog doesn't do much.
     * The timeout action won't be run.
     */
    @Test
    public void stopOnStoppedWatchdogDoesNothing() {
        watchdog.stop();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verifyNoInteractions(timeoutAction);
    }
}
