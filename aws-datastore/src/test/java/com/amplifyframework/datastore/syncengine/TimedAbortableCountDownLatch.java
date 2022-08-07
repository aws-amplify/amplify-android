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

package com.amplifyframework.datastore.syncengine;

import java.util.concurrent.TimeUnit;

/**
 * A TimedAbortableCountDownLatch with the ability to handle errors. This is really just for tests.
 * @param <E> the type of Throwable to be handled.
 */
final class TimedAbortableCountDownLatch<E extends Throwable> extends AbortableCountDownLatch<E> {
    private final long timeout;
    private final TimeUnit unit;

    TimedAbortableCountDownLatch(int count, long timeout, TimeUnit unit) {
        super(count);
        this.timeout = timeout;
        this.unit = unit;
    }

    public boolean abortableAwait() throws InterruptedException, E {
        final boolean success = super.await(timeout, unit);
        E error = getError();
        if (error != null) {
            throw error;
        }
        return success;
    }
}
