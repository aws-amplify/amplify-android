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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;

import java.util.concurrent.TimeUnit;

/**
 * An action which awaits a call. Block the current thread of execution until
 * action is called by using {@link #awaitCall()}.
 */
public final class LatchedAction implements Action {
    private static final long DEFAULT_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);

    private final LatchedConsumer<CompletionToken> actionConsumer;

    private LatchedAction(long waitTimeMs) {
        this.actionConsumer = LatchedConsumer.instance(waitTimeMs);
    }

    /**
     * Creates an latched action instance, using a default latch timeout.
     * @return A latched action
     */
    @NonNull
    public static LatchedAction instance() {
        return new LatchedAction(DEFAULT_WAIT_TIME_MS);
    }

    /**
     * Creates a latched action instance, using a provided latch timeout.
     * @param waitTimeMs Amount of time to wait when calling {@link #awaitCall()}
     * @return A latched action
     */
    @NonNull
    public static LatchedAction instance(long waitTimeMs) {
        return new LatchedAction(waitTimeMs);
    }

    @Override
    public void call() {
        actionConsumer.accept(CompletionToken.whatever());
    }

    /**
     * Wait until the action has been called.
     * @throws RuntimeException if the action was not called
     */
    public void awaitCall() throws RuntimeException {
        actionConsumer.awaitValue();
    }

    /**
     * Some dummy, non-null value class that we can pass into the latched consumer.
     * The latched consumer is re-used as an implementation detail of this LatchedAction
     * because it has most of the logic we need. However, we need to stuff something into its
     * parameter, since we don't care about the parameter. That's all this is for.
     */
    static final class CompletionToken {
        @SuppressWarnings("checkstyle:all") private CompletionToken() {}

        static CompletionToken whatever() {
            return new CompletionToken();
        }
    }
}
