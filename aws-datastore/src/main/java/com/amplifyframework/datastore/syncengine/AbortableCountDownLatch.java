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

import java.util.concurrent.CountDownLatch;

/**
 * A CountDownLatch with the ability to handle errors.  When {@link #abort(E)} is called, all waiting threads are
 * unblocked, and the error is thrown to them to be handled.
 * @param <E> the type of Throwable to be handled.
 */
public abstract class AbortableCountDownLatch<E extends Throwable> extends CountDownLatch {
    private E error;

    AbortableCountDownLatch(int count) {
        super(count);
    }

    /**
     * abort is called to check if the latch was finished or not.
     * @param error the type of Throwable to be handled.
     */
    public final void abort(E error) {
        if (getCount() == 0) {
            return;
        }

        this.error = error;
        while (getCount() > 0) {
            countDown();
        }
    }

    /**
     * Use getError to access E for {@link #abortableAwait()} as needed.
     * E must be set via {@link #abort(E)}
     * @return E the error
     */
    protected E getError() {
        return error;
    }

    /**
     * Method is called when blocking a latch. You can choose how to enable the aborting
     * or awaiting.
     * @return true if the await was success
     * @throws InterruptedException interrupted before timeout
     * @throws E if interrupted by timeout
     */
    public abstract boolean abortableAwait() throws InterruptedException, E;
}
