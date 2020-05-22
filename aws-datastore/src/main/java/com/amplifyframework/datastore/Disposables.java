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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.Cancelable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

/**
 * Utility class for functions used throughout DataStore.
 */
public final class Disposables {

    private Disposables() {}

    /**
     * A utility method to convert a cancelable to a Disposable.
     * @param cancelable An Amplify Cancelable
     * @return An RxJava2 Disposable that disposed by invoking the cancellation.
     */
    public static Disposable fromCancelable(@NonNull Cancelable cancelable) {
        Objects.requireNonNull(cancelable);
        return new Disposable() {
            private final AtomicReference<Boolean> isCanceled = new AtomicReference<>(false);
            @Override
            public void dispose() {
                synchronized (isCanceled) {
                    if (!isCanceled.get()) {
                        cancelable.cancel();
                        isCanceled.set(true);
                    }
                }
            }

            @Override
            public boolean isDisposed() {
                synchronized (isCanceled) {
                    return isCanceled.get();
                }
            }
        };
    }
}
