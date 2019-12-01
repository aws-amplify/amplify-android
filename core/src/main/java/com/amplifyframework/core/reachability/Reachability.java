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

/**
 * Reachability refers to whether or not the client is able to reach a system over a network.
 * This is a different concept than whether or not an Android device has an active interface
 * for communication.
 */
@SuppressWarnings("unused")
public interface Reachability {

    /**
     * Checks if there are any actions that still need to be executed.
     * In other words, are there any hosts that are still unreachable?
     * @return true if there are hosts for which actions have not been fulfilled,
     *         false, otherwise
     */
    boolean hasPendingActions();

    /**
     * Checks if a host is reachable.
     * @param host A host to check for reachability
     * @return true if host is reachable; false, otherwise
     */
    boolean isReachable(@NonNull Host host);

    /**
     * Perform an action when a host is determined to be reachable.
     * This call completes as soon as a host is determined to be reachable once; no
     * further calls are placed to the provided action.
     * If you change your mind before the action is called, you can cancel this
     * registration via the returned Cancelable.
     * @param host A host which may or may not be reachable
     * @param onHostReachableAction An action to take when the host is determined to
     *                              be reachable
     * @return A Cancellable which may be called to de-schedule the action from being
     *         called
     */
    @NonNull
    Cancelable whenReachable(@NonNull Host host, @NonNull OnHostReachableAction onHostReachableAction);

    /**
     * An action that gets performed when a host is determined to be reachable.
     */
    interface OnHostReachableAction {
        /**
         * A host was recently been determined to be reachable. This may be called when
         * a host transitions from unreachable to reachable, or if
         * {@link #whenReachable(Host, OnHostReachableAction)} is called against an
         * already-reachable host.
         * @param host The host to consider for online state.
         */
        void onHostReachable(@NonNull Host host);
    }
}
