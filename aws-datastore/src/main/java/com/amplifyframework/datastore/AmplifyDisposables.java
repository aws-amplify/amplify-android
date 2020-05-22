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
import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Cancelable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

/**
 * A utility for building Rx {@link Disposable}s from Amplify entities,
 * e.g. the {@link Cancelable}.
 */
public final class AmplifyDisposables {
    private AmplifyDisposables() {}

    /**
     * Builds an Rx {@link Disposable} around an Amplify {@link Cancelable}.
     * @param cancelable An Amplify Cancelable
     * @return An Rx Disposable
     */
    @NonNull
    public static Disposable fromCancelable(@Nullable Cancelable cancelable) {
        if (cancelable == null) {
            return io.reactivex.disposables.Disposables.empty();
        }
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
