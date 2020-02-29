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

import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link PeriodicReachabilityChecker}.
 */
public final class PeriodicReachabilityCheckerInstrumentationTest {
    private static final int SECURE_HTTPS_PORT = 443;
    private static final long TEST_SCAN_TIME_MS = 500;

    /**
     * A host that does not exist is not reachable.
     */
    @Test
    public void isReachableForBadHostReturnsFalse() {
        assertFalse(
            PeriodicReachabilityChecker.instance(TEST_SCAN_TIME_MS)
                .isReachable(SocketHost.from(String.format(
                    "https://%s.aws.amazon.com:%d", RandomString.string(), SECURE_HTTPS_PORT
                )))
        );
    }

    /**
     * A valid online host is reachable.
     */
    @Test
    public void isReachableForGoodHostReturnsTrue() {
        assertTrue(
            PeriodicReachabilityChecker.instance(TEST_SCAN_TIME_MS)
                .isReachable(SocketHost.from("aws.amazon.com", SECURE_HTTPS_PORT))
        );
    }

    /**
     * {@link PeriodicReachabilityChecker#whenReachable(Host, Reachability.OnHostReachableAction)}
     * will dispatch a callback action when it correctly determines a valid online host to be
     * reachable.
     */
    @Test
    public void whenReachableReturnsHostWhenReachable() {
        final Reachability reachability = PeriodicReachabilityChecker.instance(TEST_SCAN_TIME_MS);
        final Host awsWebsite = SocketHost.from("aws.amazon.com", SECURE_HTTPS_PORT);
        final LatchingAction latchingAction = new LatchingAction();
        reachability.whenReachable(awsWebsite, latchingAction);
        final Host actionHost = latchingAction.awaitHost();
        assertEquals(awsWebsite, actionHost);
        assertFalse(reachability.hasPendingActions());
    }

    /**
     * If an action registration is canceled before evaluating whether or not the host
     * is reachable, then the action will not be executed. This is the case even if the host
     * would have actually been reachable, and would have caused the action to get
     * invoked, otherwise.
     */
    @Test(expected = LatchingAction.ActionNotExecutedException.class)
    public void canceledActionNotCalledWhenHostIsAvailable() {
        final LatchingAction latchingAction = new LatchingAction();
        PeriodicReachabilityChecker.instance(TEST_SCAN_TIME_MS)
            .whenReachable(SocketHost.from("aws.amazon.com", SECURE_HTTPS_PORT), latchingAction)
            .cancel();
        latchingAction.awaitHost();
    }

    /**
     * A utility to block the test runner until we do (or do not) get a result back from
     * the {@link PeriodicReachabilityChecker#whenReachable(Host, Reachability.OnHostReachableAction)}
     * call.
     */
    static final class LatchingAction implements Reachability.OnHostReachableAction {
        private static final long WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(1);

        private final AtomicReference<Host> hostContainer;
        private final CountDownLatch latch;

        LatchingAction() {
            this.hostContainer = new AtomicReference<>();
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void onHostReachable(@NonNull Host host) {
            hostContainer.set(host);
            latch.countDown();
        }

        Host awaitHost() {
            try {
                if (!latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS)) {
                    throw new ActionNotExecutedException("Action callback not invoked.");
                }
            } catch (InterruptedException interruptedException) {
                throw new ActionNotExecutedException(interruptedException);
            }
            return hostContainer.get();
        }

        /**
         * An exception that the {@link LatchingAction} will throw if the latch never
         * counts down. In other words, the exception is thrown if there is no callback
         * action executed.
         */
        static final class ActionNotExecutedException extends RuntimeException {
            private static final long serialVersionUID = 1L;

            ActionNotExecutedException(Throwable cause) {
                super(cause);
            }

            ActionNotExecutedException(String message) {
                super(message);
            }
        }
    }
}
