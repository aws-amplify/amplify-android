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

import com.amplifyframework.core.async.AmplifyExecutors;
import com.amplifyframework.core.async.Cancelable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * PeriodicReachabilityChecker is a utility to determine whether or not a host is reachable.
 * PeriodicReachabilityChecker maintains a collection of hosts, and actions to perform when those hosts
 * become online. The actions will be invoked once, and then the registration will be
 * automatically removed. No notification is given when a host goes back offline.
 * Host reachability checks are performed periodically. The period is set when obtaining
 * PeriodicReachabilityChecker via the {@link PeriodicReachabilityChecker#instance(long)} factory.
 * A default value may be used when creating PeriodicReachabilityChecker via
 * {@link PeriodicReachabilityChecker#instance()}.
 */
@SuppressWarnings("unused")
public final class PeriodicReachabilityChecker implements Reachability {
    private static final long DEFAULT_SCAN_TIME_MS = TimeUnit.SECONDS.toMillis(5);

    private final Map<Host, Set<OnHostReachableAction>> actions;
    private final Object actionsLock;
    private final ExecutorService workExecutorService;
    private final long scanTimeMs;
    private final ScheduledExecutorService periodicCheckScheduler;
    private ScheduledFuture<?> periodicCheck;

    private PeriodicReachabilityChecker(long scanTimeMs) {
        this.actions = new HashMap<>();
        this.actionsLock = new Object();
        this.workExecutorService = AmplifyExecutors.standard();
        this.scanTimeMs = scanTimeMs;
        this.periodicCheckScheduler = AmplifyExecutors.periodic();
        this.periodicCheck = null;
    }

    /**
     * Creates an instance of the PeriodicReachabilityChecker reachability client, that will scan host reachability
     * every time a period of {@link #DEFAULT_SCAN_TIME_MS} elapses.
     * @return A PeriodicReachabilityChecker reachability instance
     */
    @SuppressWarnings("unused")
    @NonNull
    public static PeriodicReachabilityChecker instance() {
        return new PeriodicReachabilityChecker(DEFAULT_SCAN_TIME_MS);
    }

    /**
     * Creates an instance of PeriodicReachabilityChecker reachability client, that will scan host reachability
     * every time the provided scan period has elapsed.
     * @param scanPeriodMs Period of time between reachability scans, in milliseconds
     * @return A PeriodicReachabilityChecker reachability instance
     */
    @NonNull
    public static PeriodicReachabilityChecker instance(final long scanPeriodMs) {
        return new PeriodicReachabilityChecker(scanPeriodMs);
    }

    @Override
    public boolean hasPendingActions() {
        // locking on actionsLock and checking actions.isEmpty()
        // seems like a correct logic, but instead we want to check if
        // the component is doing work, since that's what actually will
        // impact the system. actions.isEmpty() is a less strong pre-condition
        // to periodicCheck == null being true.
        synchronized (periodicCheckScheduler) {
            return periodicCheck != null;
        }
    }

    @Override
    public boolean isReachable(@NonNull Host host) {
        return Objects.requireNonNull(host).isReachable();
    }

    @NonNull
    @Override
    public Cancelable whenReachable(
            @NonNull final Host host,
            @NonNull final OnHostReachableAction onHostReachableAction) {

        Objects.requireNonNull(host);
        Objects.requireNonNull(onHostReachableAction);

        addAction(host, onHostReachableAction);

        return () -> removeAction(host, onHostReachableAction);
    }

    private void addAction(@NonNull final Host host, @NonNull final OnHostReachableAction action) {
        boolean justBecameNonEmpty;
        synchronized (actionsLock) {
            Set<OnHostReachableAction> registeredActionsForHost = actions.get(host);
            if (null == registeredActionsForHost) {
                registeredActionsForHost = new HashSet<>();
                actions.put(host, registeredActionsForHost);
            }
            registeredActionsForHost.add(action);
            justBecameNonEmpty = 1 == registeredActionsForHost.size() && 1 == actions.size();
        }
        if (justBecameNonEmpty) {
            startPeriodicChecks();
        }
    }

    private void removeAction(@NonNull final Host host, @NonNull final OnHostReachableAction action) {
        final boolean isEmptyNow;
        synchronized (actionsLock) {
            final Set<OnHostReachableAction> hostActions = actions.get(host);
            if (hostActions != null) {
                hostActions.remove(action);
                if (hostActions.isEmpty()) {
                    actions.remove(host);
                }
            }
            isEmptyNow = actions.isEmpty();
        }
        if (isEmptyNow) {
            stopPeriodicChecks();
        }
    }

    private void fulfillActions(@NonNull final Host host) {
        for (final OnHostReachableAction action : copyActions(host)) {
            workExecutorService.execute(() -> {
                removeAction(host, action);
                action.onHostReachable(host);
            });
        }
    }

    @NonNull
    private Set<OnHostReachableAction> copyActions(@NonNull final Host host) {
        final Set<OnHostReachableAction> safeActions = new HashSet<>();
        synchronized (actionsLock) {
            final Set<OnHostReachableAction> registeredActions = actions.get(host);
            if (null != registeredActions) {
                safeActions.addAll(registeredActions);
            }
        }
        return safeActions;
    }

    @NonNull
    private Set<Host> copyHosts() {
        synchronized (actionsLock) {
            return new HashSet<>(actions.keySet());
        }
    }

    private void startPeriodicChecks() {
        synchronized (periodicCheckScheduler) {
            //noinspection CodeBlock2Expr
            periodicCheck = periodicCheckScheduler.scheduleAtFixedRate(() -> {
                checkReachability(copyHosts());
            }, 0, scanTimeMs, TimeUnit.MILLISECONDS);
        }
    }

    private void stopPeriodicChecks() {
        synchronized (periodicCheckScheduler) {
            if (periodicCheck != null) {
                periodicCheck.cancel(false);
                periodicCheck = null;
            }
        }
    }

    private void checkReachability(@NonNull final Set<Host> hosts) {
        for (final Host host : hosts) {
            workExecutorService.execute(() -> {
                if (host.isReachable()) {
                    fulfillActions(host);
                }
            });
        }
    }
}
