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

package com.amplifyframework.core.reachability;

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.reachability.Reachability.OnHostReachableAction;
import com.amplifyframework.testutils.Sleep;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the {@link PeriodicReachabilityChecker}.
 */
public final class PeriodicReachabilityCheckerTest {
    private static final long TEST_SCAN_PERIOD_MS = 100;

    private Reachability reachability;

    /**
     * Obtain an instance of the component under test, i.e. {@link PeriodicReachabilityChecker}.
     */
    @Before
    public void before() {
        reachability = PeriodicReachabilityChecker.instance(TEST_SCAN_PERIOD_MS);
    }

    /**
     * When a host is not originally online, the checker will continue to check it.
     * When the host comes online, the checker will determine that the host is reachable,
     * the next time it performs a check. Upon determining that the host is reachable,
     * the checker will execute the callback action, and remove the action from its
     * pending actions.
     */
    @Test
    public void callbackInvokedWhenHostBecomesReachable() {
        // Register an action for a host. Host is initially offline.
        LatchingAction latchingAction = new LatchingAction();
        StubHost expectedHost = new StubHost();
        reachability.whenReachable(expectedHost, latchingAction);

        // After a second, the host comes online.
        Sleep.milliseconds(TimeUnit.SECONDS.toMillis(1));
        expectedHost.setReachable();

        // Wait a couple of scan periods.
        Sleep.milliseconds(2 * TEST_SCAN_PERIOD_MS);

        // Wait for the action to get called, and validate that the host argument
        // it receives is the one that we passed in.
        assertEquals(latchingAction.awaitActionExecution(), latchingAction.awaitActionExecution());

        // Since we only had one action to process, the reachability component
        // should not have any actions left to process.
        assertFalse(reachability.hasPendingActions());
    }

    /**
     * If an action is registered via
     * {@link PeriodicReachabilityChecker#whenReachable(Host, Reachability.OnHostReachableAction)},
     * but is then cancelled via the returned {@link Cancelable}, then the action will never
     * be executed. This is the case even if the checker would have determined that the host was
     * online -- because, the checker will not actually perform the check, without any action
     * to call.
     */
    @Test(expected = LatchingAction.ActionNotExecutedException.class)
    public void callbackNotInvokedIfActionCanceledBeforeScan() {
        StubHost host = new StubHost();
        LatchingAction action = new LatchingAction();
        Cancelable cancelable = reachability.whenReachable(host, action);

        // Allow a couple of scans to occur ...
        Sleep.milliseconds(2 * TEST_SCAN_PERIOD_MS);

        // Cancel our request, right before host becomes reachable
        cancelable.cancel();
        host.setReachable();

        // There are no pending actions, since we removed the one that did exist.
        assertFalse(reachability.hasPendingActions());

        // We expect this latch will time out,
        // since we don't expect any action callback due to cancel().
        action.awaitActionExecution();
    }

    /**
     * A simple test-stub implementation of {@link Host}, which allows
     * us to toggle the reachability of the {@link Host} without getting involved
     * with any actual networking.
     */
    static final class StubHost implements Host {
        private boolean reachable;

        @Override
        public boolean isReachable() {
            return reachable;
        }

        void setReachable() {
            this.reachable = true;
        }
    }

    /**
     * An {@link OnHostReachableAction} which uses a {@link CountDownLatch}, to await an
     * action callback from {@link PeriodicReachabilityChecker#whenReachable(Host, OnHostReachableAction)}.
     * Call {@link LatchingAction#awaitActionExecution()} to block until the action has executed.
     */
    private static final class LatchingAction implements Reachability.OnHostReachableAction {
        private static final long DEFAULT_LATCH_AWAIT_MS = TimeUnit.SECONDS.toMillis(2);

        private final AtomicReference<Host> hostContainer;
        private final CountDownLatch latch;
        private final long waitTimeMs;

        LatchingAction(@SuppressWarnings("SameParameterValue") long waitTimeMs) {
            this.hostContainer = new AtomicReference<>();
            this.latch = new CountDownLatch(1);
            this.waitTimeMs = waitTimeMs;
        }

        LatchingAction() {
            this(DEFAULT_LATCH_AWAIT_MS);
        }

        @Override
        public void onHostReachable(@NonNull Host host) {
            hostContainer.set(host);
            latch.countDown();
        }

        @NonNull
        Host awaitActionExecution() {
            try {
                if (!latch.await(waitTimeMs, TimeUnit.MILLISECONDS)) {
                    throw new ActionNotExecutedException("callback latch was not counted down - callback not fired?");
                }
            } catch (InterruptedException interruptedException) {
                throw new ActionNotExecutedException(interruptedException);
            }
            return hostContainer.get();
        }

        /**
         * An exception which is thrown it the {@link LatchingAction#awaitActionExecution()}
         * fails or times out. This means that the callback action was never invoked.
         */
        static final class ActionNotExecutedException extends RuntimeException {
            private static final long serialVersionUID = 1L;

            ActionNotExecutedException(final Throwable cause) {
                super(cause);
            }

            ActionNotExecutedException(@SuppressWarnings("SameParameterValue") final String message) {
                super(message);
            }
        }
    }
}
