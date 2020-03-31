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

package com.amplifyframework.testutils;

import com.amplifyframework.hub.HubEvent;

/**
 * A test utility to sleep the thread of execution.
 * This exists so that we don't have to catch {@link InterruptedException} all over the place,
 * cluttering our test code.
 *
 * Okay. Now, here's a rant about why you _almost certainly_ shouldn't use this class:
 *
 * Sleeping for the purpose of state synchronization is a code smell. Often, mechanisms like
 * this will be used as a means to cross your fingers and hope that some components have reached a
 * desired state after (some magic amount of) milliseconds. Well, what if the state is reached
 * after (some magic amount of milliseconds) + 10ms?
 *
 * Instead, you should coordinate component state based on deterministic events. Instead of
 * using this utility, consider publishing a {@link HubEvent} from one component, and listening
 * for it in another.
 *
 * All of this considered, "When ya gotta sleep, ya gotta, sleep." Ya know?
 */
public final class Sleep {
    private Sleep() {}

    /**
     * Sleeps the thread of execution for a duration of milliseconds.
     * @param duration Duration of time to sleep
     * @throws RuntimeException If thread is interrupted while sleeping
     */
    public static void milliseconds(long duration) throws RuntimeException {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }
}
